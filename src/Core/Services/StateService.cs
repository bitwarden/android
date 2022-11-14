using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.Core.Services
{
    public class StateService : IStateService
    {
        private readonly IStorageService _storageService;
        private readonly IStorageService _secureStorageService;
        private readonly IMessagingService _messagingService;

        private State _state;
        private bool _migrationChecked;

        public List<AccountView> AccountViews { get; set; }

        public StateService(IStorageService storageService,
                            IStorageService secureStorageService,
                            IMessagingService messagingService)
        {
            _storageService = storageService;
            _secureStorageService = secureStorageService;
            _messagingService = messagingService;
        }

        public async Task<string> GetActiveUserIdAsync()
        {
            await CheckStateAsync();

            var activeUserId = _state?.ActiveUserId;
            if (activeUserId == null)
            {
                var state = await GetStateFromStorageAsync();
                activeUserId = state?.ActiveUserId;
            }
            return activeUserId;
        }

        public async Task<string> GetActiveUserEmailAsync()
        {
            var activeUserId = await GetActiveUserIdAsync();
            return await GetEmailAsync(activeUserId);
        }

        public async Task<bool> IsActiveAccountAsync(string userId = null)
        {
            if (userId == null)
            {
                return true;
            }
            return userId == await GetActiveUserIdAsync();
        }

        public async Task SetActiveUserAsync(string userId)
        {
            if (userId != null)
            {
                await ValidateUserAsync(userId);
            }
            await CheckStateAsync();
            var state = await GetStateFromStorageAsync();
            state.ActiveUserId = userId;
            await SaveStateToStorageAsync(state);
            _state.ActiveUserId = userId;

            // Update pre-auth settings based on now-active user
            await SetRememberedOrgIdentifierAsync(await GetRememberedOrgIdentifierAsync());
            await SetPreAuthEnvironmentUrlsAsync(await GetEnvironmentUrlsAsync());
        }

        public async Task CheckExtensionActiveUserAndSwitchIfNeededAsync()
        {
            var extensionUserId = await GetExtensionActiveUserIdFromStorageAsync();
            if (string.IsNullOrEmpty(extensionUserId))
            {
                return;
            }

            if (_state?.ActiveUserId == extensionUserId)
            {
                // Clear the value in case the user changes the active user from the app
                // so if that happens and the user sends the app to background and comes back
                // the user is not changed again.
                await SaveExtensionActiveUserIdToStorageAsync(null);
                return;
            }

            await SetActiveUserAsync(extensionUserId);
            await SaveExtensionActiveUserIdToStorageAsync(null);
            _messagingService.Send(AccountsManagerMessageCommands.SWITCHED_ACCOUNT);
        }

        public async Task<bool> IsAuthenticatedAsync(string userId = null)
        {
            return await GetAccessTokenAsync(userId) != null;
        }

        public async Task<string> GetUserIdAsync(string email)
        {
            if (string.IsNullOrWhiteSpace(email))
            {
                throw new ArgumentNullException(nameof(email));
            }

            await CheckStateAsync();
            if (_state?.Accounts != null)
            {
                foreach (var account in _state.Accounts)
                {
                    var accountEmail = account.Value?.Profile?.Email;
                    if (accountEmail == email)
                    {
                        return account.Value.Profile.UserId;
                    }
                }
            }
            return null;
        }

        public async Task RefreshAccountViewsAsync(bool allowAddAccountRow)
        {
            await CheckStateAsync();

            if (AccountViews == null)
            {
                AccountViews = new List<AccountView>();
            }
            else
            {
                AccountViews.Clear();
            }

            var accountList = _state?.Accounts?.Values.ToList();
            if (accountList == null)
            {
                return;
            }
            var vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            foreach (var account in accountList)
            {
                var isActiveAccount = account.Profile.UserId == _state.ActiveUserId;
                var accountView = new AccountView(account, isActiveAccount);

                if (await vaultTimeoutService.IsLoggedOutByTimeoutAsync(accountView.UserId) ||
                    await vaultTimeoutService.ShouldLogOutByTimeoutAsync(accountView.UserId))
                {
                    accountView.AuthStatus = AuthenticationStatus.LoggedOut;
                }
                else if (await vaultTimeoutService.IsLockedAsync(accountView.UserId) ||
                         await vaultTimeoutService.ShouldLockAsync(accountView.UserId))
                {
                    accountView.AuthStatus = AuthenticationStatus.Locked;
                }
                else
                {
                    accountView.AuthStatus = AuthenticationStatus.Unlocked;
                }
                AccountViews.Add(accountView);
            }
            if (allowAddAccountRow && AccountViews.Count < Constants.MaxAccounts)
            {
                AccountViews.Add(new AccountView());
            }
        }

        public async Task AddAccountAsync(Account account)
        {
            await ScaffoldNewAccountAsync(account);
            await SetActiveUserAsync(account.Profile.UserId);
            await RefreshAccountViewsAsync(true);
        }

        public async Task LogoutAccountAsync(string userId, bool userInitiated)
        {
            if (string.IsNullOrWhiteSpace(userId))
            {
                throw new ArgumentNullException(nameof(userId));
            }

            await CheckStateAsync();
            await RemoveAccountAsync(userId, userInitiated);

            // If user initiated logout (not vault timeout) and ActiveUserId is null after account removal, find the
            // next user to make active, if any
            if (userInitiated && _state?.ActiveUserId == null && _state?.Accounts != null)
            {
                foreach (var account in _state.Accounts)
                {
                    var uid = account.Value?.Profile?.UserId;
                    if (uid == null)
                    {
                        continue;
                    }
                    await SetActiveUserAsync(uid);
                    break;
                }
            }
        }

        public async Task<EnvironmentUrlData> GetPreAuthEnvironmentUrlsAsync()
        {
            return await GetValueAsync<EnvironmentUrlData>(
                Constants.PreAuthEnvironmentUrlsKey, await GetDefaultStorageOptionsAsync());
        }

        public async Task SetPreAuthEnvironmentUrlsAsync(EnvironmentUrlData value)
        {
            await SetValueAsync(
                Constants.PreAuthEnvironmentUrlsKey, value, await GetDefaultStorageOptionsAsync());
        }

        public async Task<EnvironmentUrlData> GetEnvironmentUrlsAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync())
            ))?.Settings?.EnvironmentUrls;
        }

        public async Task<bool?> GetBiometricUnlockAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.BiometricUnlockKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetBiometricUnlockAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.BiometricUnlockKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool> GetBiometricLockedAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultInMemoryOptionsAsync())
            ))?.VolatileData?.BiometricLocked ?? true;
        }

        public async Task SetBiometricLockedAsync(bool value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultInMemoryOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.VolatileData.BiometricLocked = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<bool> CanAccessPremiumAsync(string userId = null)
        {
            if (userId == null)
            {
                userId = await GetActiveUserIdAsync();
            }
            if (!await IsAuthenticatedAsync(userId))
            {
                return false;
            }

            var account = await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync()));
            if (account?.Profile?.HasPremiumPersonally.GetValueOrDefault() ?? false)
            {
                return true;
            }

            var organizationService = ServiceContainer.Resolve<IOrganizationService>("organizationService");
            var organizations = await organizationService.GetAllAsync(userId);
            return organizations?.Any(o => o.UsersGetPremium && o.Enabled) ?? false;
        }

        public async Task<string> GetProtectedPinAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.ProtectedPinKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetProtectedPinAsync(string value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.ProtectedPinKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetPinProtectedAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.PinProtectedKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetPinProtectedAsync(string value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.PinProtectedKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<EncString> GetPinProtectedKeyAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultInMemoryOptionsAsync())
            ))?.VolatileData?.PinProtectedKey;
        }

        public async Task SetPinProtectedKeyAsync(EncString value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultInMemoryOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.VolatileData.PinProtectedKey = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<KdfType?> GetKdfTypeAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync())
            ))?.Profile?.KdfType;
        }

        public async Task SetKdfTypeAsync(KdfType? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Profile.KdfType = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<int?> GetKdfIterationsAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync())
            ))?.Profile?.KdfIterations;
        }

        public async Task SetKdfIterationsAsync(int? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Profile.KdfIterations = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<string> GetKeyEncryptedAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultSecureStorageOptionsAsync());
            var key = Constants.KeyKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetKeyEncryptedAsync(string value, string userId)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultSecureStorageOptionsAsync());
            var key = Constants.KeyKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<SymmetricCryptoKey> GetKeyDecryptedAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultInMemoryOptionsAsync())
            ))?.VolatileData?.Key;
        }

        public async Task SetKeyDecryptedAsync(SymmetricCryptoKey value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultInMemoryOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.VolatileData.Key = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<string> GetKeyHashAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.KeyHashKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetKeyHashAsync(string value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.KeyHashKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetEncKeyEncryptedAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.EncKeyKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetEncKeyEncryptedAsync(string value, string userId)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.EncKeyKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, string>> GetOrgKeysEncryptedAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.EncOrgKeysKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, string>>(key, reconciledOptions);
        }

        public async Task SetOrgKeysEncryptedAsync(Dictionary<string, string> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.EncOrgKeysKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetPrivateKeyEncryptedAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.EncPrivateKeyKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetPrivateKeyEncryptedAsync(string value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.EncPrivateKeyKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<List<string>> GetAutofillBlacklistedUrisAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.AutofillBlacklistedUrisKey(reconciledOptions.UserId);
            return await GetValueAsync<List<string>>(key, reconciledOptions);
        }

        public async Task SetAutofillBlacklistedUrisAsync(List<string> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.AutofillBlacklistedUrisKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetAutofillTileAddedAsync()
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.AutofillTileAdded;
            return await GetValueAsync<bool?>(key, options);
        }

        public async Task SetAutofillTileAddedAsync(bool? value)
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.AutofillTileAdded;
            await SetValueAsync(key, value, options);
        }

        public async Task<string> GetEmailAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync())
            ))?.Profile?.Email;
        }

        public async Task<string> GetNameAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync())
            ))?.Profile?.Name;
        }

        public async Task SetNameAsync(string value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Profile.Name = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<string> GetOrgIdentifierAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync())
            ))?.Profile?.OrgIdentifier;
        }

        public async Task<long?> GetLastActiveTimeAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.LastActiveTimeKey(reconciledOptions.UserId);
            return await GetValueAsync<long?>(key, reconciledOptions);
        }

        public async Task SetLastActiveTimeAsync(long? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.LastActiveTimeKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<int?> GetVaultTimeoutAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync())
            ))?.Settings?.VaultTimeout;
        }

        public async Task SetVaultTimeoutAsync(int? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Settings.VaultTimeout = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<VaultTimeoutAction?> GetVaultTimeoutActionAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync())
            ))?.Settings?.VaultTimeoutAction;
        }

        public async Task SetVaultTimeoutActionAsync(VaultTimeoutAction? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Settings.VaultTimeoutAction = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<bool> GetScreenCaptureAllowedAsync(string userId = null)
        {
            if (CoreHelpers.ForceScreenCaptureEnabled())
            {
                return true;
            }

            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync())
            ))?.Settings?.ScreenCaptureAllowed ?? false;
        }

        public async Task SetScreenCaptureAllowedAsync(bool value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Settings.ScreenCaptureAllowed = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<DateTime?> GetLastFileCacheClearAsync()
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.LastFileCacheClearKey;
            return await GetValueAsync<DateTime?>(key, options);
        }

        public async Task SetLastFileCacheClearAsync(DateTime? value)
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.LastFileCacheClearKey;
            await SetValueAsync(key, value, options);
        }

        public async Task<PreviousPageInfo> GetPreviousPageInfoAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.PreviousPageKey(reconciledOptions.UserId);
            return await GetValueAsync<PreviousPageInfo>(key, reconciledOptions);
        }

        public async Task SetPreviousPageInfoAsync(PreviousPageInfo value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.PreviousPageKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<int> GetInvalidUnlockAttemptsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.InvalidUnlockAttemptsKey(reconciledOptions.UserId);
            return await GetValueAsync<int>(key, reconciledOptions);
        }

        public async Task SetInvalidUnlockAttemptsAsync(int? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.InvalidUnlockAttemptsKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetLastBuildAsync()
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.LastBuildKey;
            return await GetValueAsync<string>(key, options);
        }

        public async Task SetLastBuildAsync(string value)
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.LastBuildKey;
            await SetValueAsync(key, value, options);
        }

        public async Task<bool?> GetDisableFaviconAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.DisableFaviconKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetDisableFaviconAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.DisableFaviconKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);

            // TODO remove this to restore per-account DisableFavicon support
            SetValueGloballyAsync(Constants.DisableFaviconKey, value, reconciledOptions).FireAndForget();
        }

        public async Task<bool?> GetDisableAutoTotpCopyAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.DisableAutoTotpCopyKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetDisableAutoTotpCopyAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.DisableAutoTotpCopyKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetInlineAutofillEnabledAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.InlineAutofillEnabledKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetInlineAutofillEnabledAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.InlineAutofillEnabledKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetAutofillDisableSavePromptAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.AutofillDisableSavePromptKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetAutofillDisableSavePromptAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.AutofillDisableSavePromptKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, Dictionary<string, object>>> GetLocalDataAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.LocalDataKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, Dictionary<string, object>>>(key, reconciledOptions);
        }

        public async Task SetLocalDataAsync(Dictionary<string, Dictionary<string, object>> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.LocalDataKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, CipherData>> GetEncryptedCiphersAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.CiphersKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, CipherData>>(key, reconciledOptions);
        }

        public async Task SetEncryptedCiphersAsync(Dictionary<string, CipherData> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.CiphersKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<int?> GetDefaultUriMatchAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.DefaultUriMatchKey(reconciledOptions.UserId);
            return await GetValueAsync<int?>(key, reconciledOptions);
        }

        public async Task SetDefaultUriMatchAsync(int? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.DefaultUriMatchKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<HashSet<string>> GetNeverDomainsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.NeverDomainsKey(reconciledOptions.UserId);
            return await GetValueAsync<HashSet<string>>(key, reconciledOptions);
        }

        public async Task SetNeverDomainsAsync(HashSet<string> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.NeverDomainsKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<int?> GetClearClipboardAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.ClearClipboardKey(reconciledOptions.UserId);
            return await GetValueAsync<int?>(key, reconciledOptions);
        }

        public async Task SetClearClipboardAsync(int? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.ClearClipboardKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, CollectionData>> GetEncryptedCollectionsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.CollectionsKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, CollectionData>>(key, reconciledOptions);
        }

        public async Task SetEncryptedCollectionsAsync(Dictionary<string, CollectionData> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.CollectionsKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool> GetPasswordRepromptAutofillAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.PasswordRepromptAutofillKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions) ?? false;
        }

        public async Task SetPasswordRepromptAutofillAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.PasswordRepromptAutofillKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool> GetPasswordVerifiedAutofillAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.PasswordVerifiedAutofillKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions) ?? false;
        }

        public async Task SetPasswordVerifiedAutofillAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.PasswordVerifiedAutofillKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<DateTime?> GetLastSyncAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.LastSyncKey(reconciledOptions.UserId);
            return await GetValueAsync<DateTime?>(key, reconciledOptions);
        }

        public async Task SetLastSyncAsync(DateTime? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.LastSyncKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetSecurityStampAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync())
            ))?.Profile?.Stamp;
        }

        public async Task SetSecurityStampAsync(string value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Profile.Stamp = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<bool> GetEmailVerifiedAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync())
            ))?.Profile?.EmailVerified ?? false;
        }

        public async Task SetEmailVerifiedAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Profile.EmailVerified = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<bool> GetSyncOnRefreshAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.SyncOnRefreshKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions) ?? false;
        }

        public async Task SetSyncOnRefreshAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.SyncOnRefreshKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetRememberedEmailAsync()
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.RememberedEmailKey;
            return await GetValueAsync<string>(key, options);
        }

        public async Task SetRememberedEmailAsync(string value)
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.RememberedEmailKey;
            await SetValueAsync(key, value, options);
        }

        public async Task<string> GetRememberedOrgIdentifierAsync()
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.RememberedOrgIdentifierKey;
            return await GetValueAsync<string>(key, options);
        }

        public async Task SetRememberedOrgIdentifierAsync(string value)
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.RememberedOrgIdentifierKey;
            await SetValueAsync(key, value, options);
        }

        public async Task<string> GetThemeAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.ThemeKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetThemeAsync(string value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.ThemeKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);

            // TODO remove this to restore per-account Theme support
            SetValueGloballyAsync(Constants.ThemeKey, value, reconciledOptions).FireAndForget();
        }

        public async Task<string> GetAutoDarkThemeAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.AutoDarkThemeKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetAutoDarkThemeAsync(string value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.AutoDarkThemeKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);

            // TODO remove this to restore per-account Theme support
            SetValueGloballyAsync(Constants.AutoDarkThemeKey, value, reconciledOptions).FireAndForget();
        }

        public async Task<bool?> GetAddSitePromptShownAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.AddSitePromptShownKey;
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetAddSitePromptShownAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.AddSitePromptShownKey;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetPushInitialPromptShownAsync()
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.PushInitialPromptShownKey;
            return await GetValueAsync<bool?>(key, options);
        }

        public async Task SetPushInitialPromptShownAsync(bool? value)
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.PushInitialPromptShownKey;
            await SetValueAsync(key, value, options);
        }

        public async Task<DateTime?> GetPushLastRegistrationDateAsync()
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.PushLastRegistrationDateKey;
            return await GetValueAsync<DateTime?>(key, options);
        }

        public async Task SetPushLastRegistrationDateAsync(DateTime? value)
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.PushLastRegistrationDateKey;
            await SetValueAsync(key, value, options);
        }

        public async Task<string> GetPushInstallationRegistrationErrorAsync()
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.PushInstallationRegistrationErrorKey;
            return await GetValueAsync<string>(key, options);
        }

        public async Task SetPushInstallationRegistrationErrorAsync(string value)
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.PushInstallationRegistrationErrorKey;
            await SetValueAsync(key, value, options);
        }

        public async Task<string> GetPushCurrentTokenAsync()
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.PushCurrentTokenKey;
            return await GetValueAsync<string>(key, options);
        }

        public async Task SetPushCurrentTokenAsync(string value)
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.PushCurrentTokenKey;
            await SetValueAsync(key, value, options);
        }

        public async Task<List<EventData>> GetEventCollectionAsync()
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.EventCollectionKey;
            return await GetValueAsync<List<EventData>>(key, options);
        }

        public async Task SetEventCollectionAsync(List<EventData> value)
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.EventCollectionKey;
            await SetValueAsync(key, value, options);
        }

        public async Task<Dictionary<string, FolderData>> GetEncryptedFoldersAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.FoldersKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, FolderData>>(key, reconciledOptions);
        }

        public async Task SetEncryptedFoldersAsync(Dictionary<string, FolderData> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.FoldersKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, PolicyData>> GetEncryptedPoliciesAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.PoliciesKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, PolicyData>>(key, reconciledOptions);
        }

        public async Task SetEncryptedPoliciesAsync(Dictionary<string, PolicyData> value, string userId)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.PoliciesKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetPushRegisteredTokenAsync()
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.PushRegisteredTokenKey;
            return await GetValueAsync<string>(key, options);
        }

        public async Task SetPushRegisteredTokenAsync(string value)
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.PushRegisteredTokenKey;
            await SetValueAsync(key, value, options);
        }

        public async Task<bool> GetUsesKeyConnectorAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.UsesKeyConnectorKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions) ?? false;
        }

        public async Task SetUsesKeyConnectorAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.UsesKeyConnectorKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, OrganizationData>> GetOrganizationsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.OrganizationsKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, OrganizationData>>(key, reconciledOptions);
        }

        public async Task SetOrganizationsAsync(Dictionary<string, OrganizationData> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.OrganizationsKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<PasswordGenerationOptions> GetPasswordGenerationOptionsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.PassGenOptionsKey(reconciledOptions.UserId);
            return await GetValueAsync<PasswordGenerationOptions>(key, reconciledOptions);
        }

        public async Task SetPasswordGenerationOptionsAsync(PasswordGenerationOptions value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.PassGenOptionsKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<UsernameGenerationOptions> GetUsernameGenerationOptionsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.UsernameGenOptionsKey(reconciledOptions.UserId);
            return await GetValueAsync<UsernameGenerationOptions>(key, reconciledOptions);
        }

        public async Task SetUsernameGenerationOptionsAsync(UsernameGenerationOptions value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.UsernameGenOptionsKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<List<GeneratedPasswordHistory>> GetEncryptedPasswordGenerationHistory(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.PassGenHistoryKey(reconciledOptions.UserId);
            return await GetValueAsync<List<GeneratedPasswordHistory>>(key, reconciledOptions);
        }

        public async Task SetEncryptedPasswordGenerationHistoryAsync(List<GeneratedPasswordHistory> value,
            string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.PassGenHistoryKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, SendData>> GetEncryptedSendsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.SendsKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, SendData>>(key, reconciledOptions);
        }

        public async Task SetEncryptedSendsAsync(Dictionary<string, SendData> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.SendsKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, object>> GetSettingsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.SettingsKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, object>>(key, reconciledOptions);
        }

        public async Task SetSettingsAsync(Dictionary<string, object> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.SettingsKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetAccessTokenAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync())
            ))?.Tokens?.AccessToken;
        }

        public async Task SetAccessTokenAsync(string value, bool skipTokenStorage, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(
                new StorageOptions { UserId = userId, SkipTokenStorage = skipTokenStorage },
                await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Tokens.AccessToken = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<string> GetRefreshTokenAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync())
            ))?.Tokens?.RefreshToken;
        }

        public async Task SetRefreshTokenAsync(string value, bool skipTokenStorage, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(
                new StorageOptions { UserId = userId, SkipTokenStorage = skipTokenStorage },
                await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Tokens.RefreshToken = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<string> GetTwoFactorTokenAsync(string email = null)
        {
            var reconciledOptions =
                ReconcileOptions(new StorageOptions { Email = email }, await GetDefaultStorageOptionsAsync());
            var key = Constants.TwoFactorTokenKey(reconciledOptions.Email);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetTwoFactorTokenAsync(string value, string email = null)
        {
            var reconciledOptions =
                ReconcileOptions(new StorageOptions { Email = email }, await GetDefaultStorageOptionsAsync());
            var key = Constants.TwoFactorTokenKey(reconciledOptions.Email);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool> GetApprovePasswordlessLoginsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.ApprovePasswordlessLoginsKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions) ?? false;
        }

        public async Task SetApprovePasswordlessLoginsAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var key = Constants.ApprovePasswordlessLoginsKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<PasswordlessRequestNotification> GetPasswordlessLoginNotificationAsync()
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.PasswordlessLoginNotificationKey;
            return await GetValueAsync<PasswordlessRequestNotification>(key, options);
        }

        public async Task SetPasswordlessLoginNotificationAsync(PasswordlessRequestNotification value)
        {
            var options = await GetDefaultStorageOptionsAsync();
            var key = Constants.PasswordlessLoginNotificationKey;
            await SetValueAsync(key, value, options);
        }
        // Helpers

        private async Task<T> GetValueAsync<T>(string key, StorageOptions options)
        {
            return await GetStorageService(options).GetAsync<T>(key);
        }

        private async Task SetValueAsync<T>(string key, T value, StorageOptions options)
        {
            if (value == null)
            {
                await GetStorageService(options).RemoveAsync(key);
                return;
            }
            await GetStorageService(options).SaveAsync(key, value);
        }

        private async Task SetValueGloballyAsync<T>(Func<string, string> keyPrefix, T value, StorageOptions options)
        {
            if (value == null)
            {
                // don't remove values globally
                return;
            }
            await CheckStateAsync();
            if (_state?.Accounts == null)
            {
                return;
            }
            // userId from options was already applied, skip those
            var userIdToSkip = options.UserId;
            foreach (var account in _state.Accounts)
            {
                var uid = account.Value?.Profile?.UserId;
                if (uid != null && uid != userIdToSkip)
                {
                    await SetValueAsync(keyPrefix(uid), value, options);
                }
            }
        }

        private IStorageService GetStorageService(StorageOptions options)
        {
            return options.UseSecureStorage.GetValueOrDefault(false) ? _secureStorageService : _storageService;
        }

        private async Task<Account> GetAccountAsync(StorageOptions options)
        {
            await CheckStateAsync();

            if (options?.UserId == null)
            {
                return null;
            }

            // Memory
            if (_state?.Accounts?.ContainsKey(options.UserId) ?? false)
            {
                if (_state.Accounts[options.UserId].VolatileData == null)
                {
                    _state.Accounts[options.UserId].VolatileData = new Account.AccountVolatileData();
                }
                return _state.Accounts[options.UserId];
            }

            // Storage
            var state = await GetStateFromStorageAsync();
            if (state?.Accounts?.ContainsKey(options.UserId) ?? false)
            {
                state.Accounts[options.UserId].VolatileData = new Account.AccountVolatileData();
                return state.Accounts[options.UserId];
            }

            return null;
        }

        private async Task SaveAccountAsync(Account account, StorageOptions options = null)
        {
            if (account?.Profile?.UserId == null)
            {
                throw new Exception("account?.Profile?.UserId cannot be null");
            }

            await CheckStateAsync();

            // Memory
            if (UseMemory(options))
            {
                if (_state.Accounts == null)
                {
                    _state.Accounts = new Dictionary<string, Account>();
                }
                _state.Accounts[account.Profile.UserId] = account;
            }

            // Storage
            if (UseDisk(options))
            {
                var state = await GetStateFromStorageAsync() ?? new State();
                if (state.Accounts == null)
                {
                    state.Accounts = new Dictionary<string, Account>();
                }

                // Use Account copy constructor to clone with keys excluded (for storage)
                state.Accounts[account.Profile.UserId] = new Account(account);

                // If we have a vault timeout and the action is log out, don't store token
                if (options?.SkipTokenStorage.GetValueOrDefault() ?? false)
                {
                    state.Accounts[account.Profile.UserId].Tokens.AccessToken = null;
                    state.Accounts[account.Profile.UserId].Tokens.RefreshToken = null;
                }

                await SaveStateToStorageAsync(state);
            }
        }

        private async Task RemoveAccountAsync(string userId, bool userInitiated)
        {
            if (string.IsNullOrWhiteSpace(userId))
            {
                throw new ArgumentNullException(nameof(userId));
            }

            var email = await GetEmailAsync(userId);

            // Memory
            if (_state?.Accounts?.ContainsKey(userId) ?? false)
            {
                if (userInitiated)
                {
                    _state.Accounts.Remove(userId);
                }
                else
                {
                    _state.Accounts[userId].Tokens.AccessToken = null;
                    _state.Accounts[userId].Tokens.RefreshToken = null;
                    _state.Accounts[userId].VolatileData = null;
                }
            }
            if (userInitiated && _state?.ActiveUserId == userId)
            {
                _state.ActiveUserId = null;
            }

            // Storage
            var stateModified = false;
            var state = await GetStateFromStorageAsync();
            if (state?.Accounts?.ContainsKey(userId) ?? false)
            {
                if (userInitiated)
                {
                    state.Accounts.Remove(userId);
                }
                else
                {
                    state.Accounts[userId].Tokens.AccessToken = null;
                    state.Accounts[userId].Tokens.RefreshToken = null;
                }
                stateModified = true;
            }
            if (userInitiated && state?.ActiveUserId == userId)
            {
                state.ActiveUserId = null;
                stateModified = true;
            }
            if (stateModified)
            {
                await SaveStateToStorageAsync(state);
            }

            // Non-state storage
            await SetBiometricUnlockAsync(null, userId);
            await SetProtectedPinAsync(null, userId);
            await SetPinProtectedAsync(null, userId);
            await SetKeyEncryptedAsync(null, userId);
            await SetKeyHashAsync(null, userId);
            await SetEncKeyEncryptedAsync(null, userId);
            await SetOrgKeysEncryptedAsync(null, userId);
            await SetPrivateKeyEncryptedAsync(null, userId);
            await SetLastActiveTimeAsync(null, userId);
            await SetPreviousPageInfoAsync(null, userId);
            await SetInvalidUnlockAttemptsAsync(null, userId);
            await SetLocalDataAsync(null, userId);
            await SetEncryptedCiphersAsync(null, userId);
            await SetEncryptedCollectionsAsync(null, userId);
            await SetLastSyncAsync(null, userId);
            await SetEncryptedFoldersAsync(null, userId);
            await SetEncryptedPoliciesAsync(null, userId);
            await SetUsesKeyConnectorAsync(null, userId);
            await SetOrganizationsAsync(null, userId);
            await SetEncryptedPasswordGenerationHistoryAsync(null, userId);
            await SetEncryptedSendsAsync(null, userId);
            await SetSettingsAsync(null, userId);
            await SetApprovePasswordlessLoginsAsync(null, userId);

            if (userInitiated)
            {
                // user initiated logout (not vault timeout or scaffolding new account) so remove remaining settings
                await SetAutofillBlacklistedUrisAsync(null, userId);
                await SetDisableFaviconAsync(null, userId);
                await SetDisableAutoTotpCopyAsync(null, userId);
                await SetInlineAutofillEnabledAsync(null, userId);
                await SetAutofillDisableSavePromptAsync(null, userId);
                await SetDefaultUriMatchAsync(null, userId);
                await SetNeverDomainsAsync(null, userId);
                await SetClearClipboardAsync(null, userId);
                await SetPasswordRepromptAutofillAsync(null, userId);
                await SetPasswordVerifiedAutofillAsync(null, userId);
                await SetSyncOnRefreshAsync(null, userId);
                await SetThemeAsync(null, userId);
                await SetAutoDarkThemeAsync(null, userId);
                await SetAddSitePromptShownAsync(null, userId);
                await SetPasswordGenerationOptionsAsync(null, userId);
                await SetApprovePasswordlessLoginsAsync(null, userId);
                await SetUsernameGenerationOptionsAsync(null, userId);
            }
        }

        private async Task ScaffoldNewAccountAsync(Account account)
        {
            await CheckStateAsync();
            var currentTheme = await GetThemeAsync();
            var currentAutoDarkTheme = await GetAutoDarkThemeAsync();
            var currentDisableFavicons = await GetDisableFaviconAsync();

            account.Settings.EnvironmentUrls = await GetPreAuthEnvironmentUrlsAsync();

            // Storage
            var state = await GetStateFromStorageAsync() ?? new State();
            if (state.Accounts == null)
            {
                state.Accounts = new Dictionary<string, Account>();
            }
            if (state.Accounts.ContainsKey(account.Profile.UserId))
            {
                // Run cleanup pass on existing account before proceeding
                await RemoveAccountAsync(account.Profile.UserId, false);
                var existingAccount = state.Accounts[account.Profile.UserId];
                account.Settings.VaultTimeout = existingAccount.Settings.VaultTimeout;
                account.Settings.VaultTimeoutAction = existingAccount.Settings.VaultTimeoutAction;
                account.Settings.ScreenCaptureAllowed = existingAccount.Settings.ScreenCaptureAllowed;
            }

            // New account defaults
            if (account.Settings.VaultTimeout == null)
            {
                account.Settings.VaultTimeout = 15;
            }
            if (account.Settings.VaultTimeoutAction == null)
            {
                account.Settings.VaultTimeoutAction = VaultTimeoutAction.Lock;
            }
            await SetThemeAsync(currentTheme, account.Profile.UserId);
            await SetAutoDarkThemeAsync(currentAutoDarkTheme, account.Profile.UserId);
            await SetDisableFaviconAsync(currentDisableFavicons, account.Profile.UserId);

            state.Accounts[account.Profile.UserId] = account;
            await SaveStateToStorageAsync(state);

            // Memory
            if (_state == null)
            {
                _state = state;
            }
            else
            {
                if (_state.Accounts == null)
                {
                    _state.Accounts = new Dictionary<string, Account>();
                }
                _state.Accounts[account.Profile.UserId] = account;
            }
        }

        private StorageOptions ReconcileOptions(StorageOptions requestedOptions, StorageOptions defaultOptions)
        {
            if (requestedOptions == null)
            {
                return defaultOptions;
            }
            requestedOptions.StorageLocation = requestedOptions.StorageLocation ?? defaultOptions.StorageLocation;
            requestedOptions.UseSecureStorage = requestedOptions.UseSecureStorage ?? defaultOptions.UseSecureStorage;
            requestedOptions.UserId = requestedOptions.UserId ?? defaultOptions.UserId;
            requestedOptions.Email = requestedOptions.Email ?? defaultOptions.Email;
            requestedOptions.SkipTokenStorage = requestedOptions.SkipTokenStorage ?? defaultOptions.SkipTokenStorage;
            return requestedOptions;
        }

        private async Task<StorageOptions> GetDefaultStorageOptionsAsync()
        {
            return new StorageOptions()
            {
                StorageLocation = StorageLocation.Both,
                UserId = await GetActiveUserIdAsync(),
            };
        }

        private async Task<StorageOptions> GetDefaultSecureStorageOptionsAsync()
        {
            return new StorageOptions()
            {
                StorageLocation = StorageLocation.Disk,
                UseSecureStorage = true,
                UserId = await GetActiveUserIdAsync(),
            };
        }

        private async Task<StorageOptions> GetDefaultInMemoryOptionsAsync()
        {
            return new StorageOptions()
            {
                StorageLocation = StorageLocation.Memory,
                UserId = await GetActiveUserIdAsync(),
            };
        }

        private bool UseMemory(StorageOptions options)
        {
            return options?.StorageLocation == StorageLocation.Memory ||
                   options?.StorageLocation == StorageLocation.Both;
        }

        private bool UseDisk(StorageOptions options)
        {
            return options?.StorageLocation == StorageLocation.Disk ||
                   options?.StorageLocation == StorageLocation.Both;
        }

        private async Task<State> GetStateFromStorageAsync()
        {
            return await _storageService.GetAsync<State>(Constants.StateKey);
        }

        private async Task SaveStateToStorageAsync(State state)
        {
            await _storageService.SaveAsync(Constants.StateKey, state);
        }

        private async Task<string> GetExtensionActiveUserIdFromStorageAsync()
        {
            return await _storageService.GetAsync<string>(Constants.iOSExtensionActiveUserIdKey);
        }

        public async Task SaveExtensionActiveUserIdToStorageAsync(string userId)
        {
            await _storageService.SaveAsync(Constants.iOSExtensionActiveUserIdKey, userId);
        }

        private async Task CheckStateAsync()
        {
            if (!_migrationChecked)
            {
                var migrationService = ServiceContainer.Resolve<IStateMigrationService>("stateMigrationService");
                if (await migrationService.NeedsMigration())
                {
                    await migrationService.Migrate();
                }
                _migrationChecked = true;
            }

            if (_state == null)
            {
                _state = await GetStateFromStorageAsync() ?? new State();
            }
        }

        private async Task ValidateUserAsync(string userId)
        {
            if (string.IsNullOrWhiteSpace(userId))
            {
                throw new ArgumentNullException(nameof(userId));
            }
            await CheckStateAsync();
            var accounts = _state?.Accounts;
            if (accounts == null || !accounts.Any())
            {
                throw new Exception("At least one account required to validate user");
            }
            foreach (var account in accounts)
            {
                if (account.Key == userId)
                {
                    // found match, user is valid
                    return;
                }
            }
            throw new Exception("User does not exist in account list");
        }
    }
}
