using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using BwRegion = Bit.Core.Enums.Region;

namespace Bit.Core.Services
{
    public class StateService : IStateService
    {
        // TODO: Refactor this removing all storage services and use the IStorageMediatorService instead

        private readonly IStorageService _storageService;
        private readonly IStorageService _secureStorageService;
        private readonly IStorageMediatorService _storageMediatorService;
        private readonly IMessagingService _messagingService;

        private State _state;
        private bool _migrationChecked;

        public List<AccountView> AccountViews { get; set; }

        public StateService(IStorageService storageService,
                            IStorageService secureStorageService,
                            IStorageMediatorService storageMediatorService,
                            IMessagingService messagingService)
        {
            _storageService = storageService;
            _secureStorageService = secureStorageService;
            _storageMediatorService = storageMediatorService;
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

        public async Task<T> GetActiveUserCustomDataAsync<T>(Func<Account, T> dataMapper)
        {
            var userId = await GetActiveUserIdAsync();
            var account = await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync())
            );
            return dataMapper(account);
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

            await SetLastUserShouldConnectToWatchAsync();
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

        public async Task<UserKey> GetUserKeyBiometricUnlockAsync(string userId = null)
        {
            var keyB64 = await _storageMediatorService.GetAsync<string>(
                await ComposeKeyAsync(Constants.UserKeyBiometricUnlockKey, userId), true);
            return keyB64 == null ? null : new UserKey(Convert.FromBase64String(keyB64));
        }

        public async Task SetUserKeyBiometricUnlockAsync(UserKey value, string userId = null)
        {
            await _storageMediatorService.SaveAsync(
                await ComposeKeyAsync(Constants.UserKeyBiometricUnlockKey, userId), value?.KeyB64, true);
        }

        public async Task<bool?> GetBiometricUnlockAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<bool?>(Constants.BiometricUnlockKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        public async Task SetBiometricUnlockAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.BiometricUnlockKey(reconciledOptions.UserId), value, reconciledOptions);
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

        public async Task<string> GetSystemBiometricIntegrityState(string bioIntegritySrcKey)
        {
            return await GetValueAsync<string>(bioIntegritySrcKey, await GetDefaultStorageOptionsAsync());
        }

        public async Task SetSystemBiometricIntegrityState(string bioIntegritySrcKey, string systemBioIntegrityState)
        {
            await SetValueAsync(bioIntegritySrcKey, systemBioIntegrityState, await GetDefaultStorageOptionsAsync());
        }

        public async Task<bool> IsAccountBiometricIntegrityValidAsync(string bioIntegritySrcKey, string userId = null)
        {
            var systemBioIntegrityState = await GetSystemBiometricIntegrityState(bioIntegritySrcKey);
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<bool?>(
                Constants.AccountBiometricIntegrityValidKey(reconciledOptions.UserId, systemBioIntegrityState),
                reconciledOptions) ?? false;
        }

        public async Task SetAccountBiometricIntegrityValidAsync(string bioIntegritySrcKey, string userId = null)
        {
            var systemBioIntegrityState = await GetSystemBiometricIntegrityState(bioIntegritySrcKey);
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(
                Constants.AccountBiometricIntegrityValidKey(reconciledOptions.UserId, systemBioIntegrityState),
                true, reconciledOptions);
        }

        public async Task<UserKey> GetUserKeyAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultInMemoryOptionsAsync())
            ))?.VolatileData?.UserKey;
        }

        public async Task SetUserKeyAsync(UserKey value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultInMemoryOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.VolatileData.UserKey = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<MasterKey> GetMasterKeyAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultInMemoryOptionsAsync())
            ))?.VolatileData?.MasterKey;
        }

        public async Task SetMasterKeyAsync(MasterKey value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultInMemoryOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.VolatileData.MasterKey = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<string> GetMasterKeyEncryptedUserKeyAsync(string userId = null)
        {
            return await _storageMediatorService.GetAsync<string>(
                await ComposeKeyAsync(Constants.MasterKeyEncryptedUserKeyKey, userId), false);
        }

        public async Task SetMasterKeyEncryptedUserKeyAsync(string value, string userId = null)
        {
            await _storageMediatorService.SaveAsync(
                await ComposeKeyAsync(Constants.MasterKeyEncryptedUserKeyKey, userId), value, false);
        }

        public async Task<UserKey> GetUserKeyAutoUnlockAsync(string userId = null)
        {
            var keyB64 = await _storageMediatorService.GetAsync<string>(
                await ComposeKeyAsync(Constants.UserKeyAutoUnlockKey, userId), true);
            return keyB64 == null ? null : new UserKey(Convert.FromBase64String(keyB64));
        }

        public async Task SetUserKeyAutoUnlockAsync(UserKey value, string userId = null)
        {
            await _storageMediatorService.SaveAsync(
                await ComposeKeyAsync(Constants.UserKeyAutoUnlockKey, userId), value?.KeyB64, true);
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

        public async Task SetPersonalPremiumAsync(bool value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            if (account?.Profile == null || account.Profile.HasPremiumPersonally.GetValueOrDefault() == value)
            {
                return;
            }

            account.Profile.HasPremiumPersonally = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<string> GetProtectedPinAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<string>(Constants.ProtectedPinKey(reconciledOptions.UserId), reconciledOptions);
        }

        public async Task SetProtectedPinAsync(string value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.ProtectedPinKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<EncString> GetPinKeyEncryptedUserKeyAsync(string userId = null)
        {
            var key = await _storageMediatorService.GetAsync<string>(
                await ComposeKeyAsync(Constants.PinKeyEncryptedUserKeyKey, userId), false);
            return key != null ? new EncString(key) : null;
        }

        public async Task SetPinKeyEncryptedUserKeyAsync(EncString value, string userId = null)
        {
            await _storageMediatorService.SaveAsync(
                await ComposeKeyAsync(Constants.PinKeyEncryptedUserKeyKey, userId), value?.EncryptedString, false);
        }

        public async Task<EncString> GetPinKeyEncryptedUserKeyEphemeralAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultInMemoryOptionsAsync())
            ))?.VolatileData?.PinKeyEncryptedUserKeyEphemeral;
        }

        public async Task SetPinKeyEncryptedUserKeyEphemeralAsync(EncString value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultInMemoryOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.VolatileData.PinKeyEncryptedUserKeyEphemeral = value;
            await SaveAccountAsync(account, reconciledOptions);
        }


        public async Task SetKdfConfigurationAsync(KdfConfig config, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Profile.KdfType = config.Type;
            account.Profile.KdfIterations = config.Iterations;
            account.Profile.KdfMemory = config.Memory;
            account.Profile.KdfParallelism = config.Parallelism;
            await SaveAccountAsync(account, reconciledOptions);
        }


        public async Task<string> GetKeyHashAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<string>(Constants.KeyHashKey(reconciledOptions.UserId), reconciledOptions);
        }

        public async Task SetKeyHashAsync(string value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.KeyHashKey(reconciledOptions.UserId), value, reconciledOptions);
        }


        public async Task<Dictionary<string, string>> GetOrgKeysEncryptedAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<Dictionary<string, string>>(Constants.EncOrgKeysKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        public async Task SetOrgKeysEncryptedAsync(Dictionary<string, string> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.EncOrgKeysKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<string> GetPrivateKeyEncryptedAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<string>(Constants.EncPrivateKeyKey(reconciledOptions.UserId), reconciledOptions);
        }

        public async Task SetPrivateKeyEncryptedAsync(string value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.EncPrivateKeyKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<SymmetricCryptoKey> GetDeviceKeyAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var deviceKeyB64 = await _storageMediatorService.GetAsync<string>(Constants.DeviceKeyKey(reconciledOptions.UserId), true);
            if (string.IsNullOrEmpty(deviceKeyB64))
            {
                return null;
            }
            return new SymmetricCryptoKey(Convert.FromBase64String(deviceKeyB64));
        }

        public async Task SetDeviceKeyAsync(SymmetricCryptoKey value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await _storageMediatorService.SaveAsync(Constants.DeviceKeyKey(reconciledOptions.UserId), value?.KeyB64, true);
        }

        public async Task<List<string>> GetAutofillBlacklistedUrisAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<List<string>>(Constants.AutofillBlacklistedUrisKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        public async Task SetAutofillBlacklistedUrisAsync(List<string> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.AutofillBlacklistedUrisKey(reconciledOptions.UserId), value,
                reconciledOptions);
        }

        public async Task<bool?> GetAutofillTileAddedAsync()
        {
            return await GetValueAsync<bool?>(Constants.AutofillTileAddedKey, await GetDefaultStorageOptionsAsync());
        }

        public async Task SetAutofillTileAddedAsync(bool? value)
        {
            await SetValueAsync(Constants.AutofillTileAddedKey, value, await GetDefaultStorageOptionsAsync());
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
            return await GetValueAsync<long?>(Constants.LastActiveTimeKey(reconciledOptions.UserId), reconciledOptions);
        }

        public async Task SetLastActiveTimeAsync(long? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.LastActiveTimeKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<int?> GetVaultTimeoutAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<int?>(Constants.VaultTimeoutKey(reconciledOptions.UserId), reconciledOptions);
        }

        public async Task SetVaultTimeoutAsync(int? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.VaultTimeoutKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<VaultTimeoutAction?> GetVaultTimeoutActionAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<VaultTimeoutAction?>(Constants.VaultTimeoutActionKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        public async Task SetVaultTimeoutActionAsync(VaultTimeoutAction? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.VaultTimeoutActionKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<bool> GetScreenCaptureAllowedAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<bool?>(Constants.ScreenCaptureAllowedKey(reconciledOptions.UserId),
                reconciledOptions) ?? CoreHelpers.InDebugMode();
        }

        public async Task SetScreenCaptureAllowedAsync(bool value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.ScreenCaptureAllowedKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<DateTime?> GetLastFileCacheClearAsync()
        {
            return await GetValueAsync<DateTime?>(Constants.LastFileCacheClearKey,
                await GetDefaultStorageOptionsAsync());
        }

        public async Task SetLastFileCacheClearAsync(DateTime? value)
        {
            await SetValueAsync(Constants.LastFileCacheClearKey, value, await GetDefaultStorageOptionsAsync());
        }

        public async Task<PreviousPageInfo> GetPreviousPageInfoAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<PreviousPageInfo>(Constants.PreviousPageKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        public async Task SetPreviousPageInfoAsync(PreviousPageInfo value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.PreviousPageKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<int> GetInvalidUnlockAttemptsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<int>(Constants.InvalidUnlockAttemptsKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        public async Task SetInvalidUnlockAttemptsAsync(int? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.InvalidUnlockAttemptsKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<string> GetLastBuildAsync()
        {
            return await GetValueAsync<string>(Constants.LastBuildKey, await GetDefaultStorageOptionsAsync());
        }

        public async Task SetLastBuildAsync(string value)
        {
            await SetValueAsync(Constants.LastBuildKey, value, await GetDefaultStorageOptionsAsync());
        }

        // TODO: [PS-961] Fix negative function names
        public async Task<bool?> GetDisableFaviconAsync()
        {
            return await GetValueAsync<bool?>(Constants.DisableFaviconKey, await GetDefaultStorageOptionsAsync());
        }

        // TODO: [PS-961] Fix negative function names
        public async Task SetDisableFaviconAsync(bool? value)
        {
            await SetValueAsync(Constants.DisableFaviconKey, value, await GetDefaultStorageOptionsAsync());
        }

        // TODO: [PS-961] Fix negative function names
        public async Task<bool?> GetDisableAutoTotpCopyAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<bool?>(Constants.DisableAutoTotpCopyKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        // TODO: [PS-961] Fix negative function names
        public async Task SetDisableAutoTotpCopyAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.DisableAutoTotpCopyKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<bool?> GetInlineAutofillEnabledAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<bool?>(Constants.InlineAutofillEnabledKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        public async Task SetInlineAutofillEnabledAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.InlineAutofillEnabledKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<bool?> GetAutofillDisableSavePromptAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<bool?>(Constants.AutofillDisableSavePromptKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        public async Task SetAutofillDisableSavePromptAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.AutofillDisableSavePromptKey(reconciledOptions.UserId), value,
                reconciledOptions);
        }

        public async Task<Dictionary<string, Dictionary<string, object>>> GetCiphersLocalDataAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<Dictionary<string, Dictionary<string, object>>>(
                Constants.CiphersLocalDataKey(reconciledOptions.UserId), reconciledOptions);
        }

        public async Task SetCiphersLocalDataAsync(Dictionary<string, Dictionary<string, object>> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.CiphersLocalDataKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<Dictionary<string, CipherData>> GetEncryptedCiphersAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<Dictionary<string, CipherData>>(Constants.CiphersKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        public async Task SetEncryptedCiphersAsync(Dictionary<string, CipherData> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.CiphersKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<int?> GetDefaultUriMatchAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<int?>(Constants.DefaultUriMatchKey(reconciledOptions.UserId), reconciledOptions);
        }

        public async Task SetDefaultUriMatchAsync(int? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.DefaultUriMatchKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<int?> GetClearClipboardAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<int?>(Constants.ClearClipboardKey(reconciledOptions.UserId), reconciledOptions);
        }

        public async Task SetClearClipboardAsync(int? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.ClearClipboardKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<Dictionary<string, CollectionData>> GetEncryptedCollectionsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<Dictionary<string, CollectionData>>(
                Constants.CollectionsKey(reconciledOptions.UserId), reconciledOptions);
        }

        public async Task SetEncryptedCollectionsAsync(Dictionary<string, CollectionData> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.CollectionsKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<bool> GetPasswordRepromptAutofillAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<bool?>(Constants.PasswordRepromptAutofillKey(reconciledOptions.UserId),
                reconciledOptions) ?? false;
        }

        public async Task SetPasswordRepromptAutofillAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.PasswordRepromptAutofillKey(reconciledOptions.UserId), value,
                reconciledOptions);
        }

        public async Task<bool> GetPasswordVerifiedAutofillAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<bool?>(Constants.PasswordVerifiedAutofillKey(reconciledOptions.UserId),
                reconciledOptions) ?? false;
        }

        public async Task SetPasswordVerifiedAutofillAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.PasswordVerifiedAutofillKey(reconciledOptions.UserId), value,
                reconciledOptions);
        }

        public async Task<DateTime?> GetLastSyncAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<DateTime?>(Constants.LastSyncKey(reconciledOptions.UserId), reconciledOptions);
        }

        public async Task SetLastSyncAsync(DateTime? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.LastSyncKey(reconciledOptions.UserId), value, reconciledOptions);
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
            return await GetValueAsync<bool?>(Constants.SyncOnRefreshKey(reconciledOptions.UserId),
                reconciledOptions) ?? false;
        }

        public async Task SetSyncOnRefreshAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.SyncOnRefreshKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<string> GetRememberedEmailAsync()
        {
            return await GetValueAsync<string>(Constants.RememberedEmailKey, await GetDefaultStorageOptionsAsync());
        }

        public async Task SetRememberedEmailAsync(string value)
        {
            await SetValueAsync(Constants.RememberedEmailKey, value, await GetDefaultStorageOptionsAsync());
        }

        public async Task<string> GetRememberedOrgIdentifierAsync()
        {
            return await GetValueAsync<string>(Constants.RememberedOrgIdentifierKey,
                await GetDefaultStorageOptionsAsync());
        }

        public async Task SetRememberedOrgIdentifierAsync(string value)
        {
            await SetValueAsync(Constants.RememberedOrgIdentifierKey, value, await GetDefaultStorageOptionsAsync());
        }

        public async Task<string> GetThemeAsync()
        {
            return await GetValueAsync<string>(Constants.ThemeKey, await GetDefaultStorageOptionsAsync());
        }

        public async Task SetThemeAsync(string value)
        {
            await SetValueAsync(Constants.ThemeKey, value, await GetDefaultStorageOptionsAsync());
        }

        public async Task<string> GetAutoDarkThemeAsync()
        {
            return await GetValueAsync<string>(Constants.AutoDarkThemeKey, await GetDefaultStorageOptionsAsync());
        }

        public async Task SetAutoDarkThemeAsync(string value)
        {
            await SetValueAsync(Constants.AutoDarkThemeKey, value, await GetDefaultStorageOptionsAsync());
        }

        public string GetLocale()
        {
            return _storageMediatorService.Get<string>(Constants.AppLocaleKey);
        }

        public void SetLocale(string locale)
        {
            _storageMediatorService.Save(Constants.AppLocaleKey, locale);
        }

        public async Task<bool?> GetAddSitePromptShownAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<bool?>(Constants.AddSitePromptShownKey, reconciledOptions);
        }

        public async Task SetAddSitePromptShownAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.AddSitePromptShownKey, value, reconciledOptions);
        }

        public async Task<bool?> GetPushInitialPromptShownAsync()
        {
            return await GetValueAsync<bool?>(Constants.PushInitialPromptShownKey,
                await GetDefaultStorageOptionsAsync());
        }

        public async Task SetPushInitialPromptShownAsync(bool? value)
        {
            await SetValueAsync(Constants.PushInitialPromptShownKey, value, await GetDefaultStorageOptionsAsync());
        }

        public async Task<DateTime?> GetPushLastRegistrationDateAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<DateTime?>(Constants.PushLastRegistrationDateKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        public async Task SetPushLastRegistrationDateAsync(DateTime? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.PushLastRegistrationDateKey(reconciledOptions.UserId), value,
                reconciledOptions);
        }

        public async Task<string> GetPushInstallationRegistrationErrorAsync()
        {
            return await GetValueAsync<string>(Constants.PushInstallationRegistrationErrorKey,
                await GetDefaultStorageOptionsAsync());
        }

        public async Task SetPushInstallationRegistrationErrorAsync(string value)
        {
            await SetValueAsync(Constants.PushInstallationRegistrationErrorKey, value,
                await GetDefaultStorageOptionsAsync());
        }

        public async Task<string> GetPushCurrentTokenAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<string>(Constants.PushCurrentTokenKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        public async Task SetPushCurrentTokenAsync(string value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.PushCurrentTokenKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<List<EventData>> GetEventCollectionAsync()
        {
            return await GetValueAsync<List<EventData>>(Constants.EventCollectionKey,
                await GetDefaultStorageOptionsAsync());
        }

        public async Task SetEventCollectionAsync(List<EventData> value)
        {
            await SetValueAsync(Constants.EventCollectionKey, value, await GetDefaultStorageOptionsAsync());
        }

        public async Task<Dictionary<string, FolderData>> GetEncryptedFoldersAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<Dictionary<string, FolderData>>(Constants.FoldersKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        public async Task SetEncryptedFoldersAsync(Dictionary<string, FolderData> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.FoldersKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<Dictionary<string, PolicyData>> GetEncryptedPoliciesAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<Dictionary<string, PolicyData>>(Constants.PoliciesKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        public async Task SetEncryptedPoliciesAsync(Dictionary<string, PolicyData> value, string userId)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.PoliciesKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<string> GetPushRegisteredTokenAsync()
        {
            return await GetValueAsync<string>(Constants.PushRegisteredTokenKey, await GetDefaultStorageOptionsAsync());
        }

        public async Task SetPushRegisteredTokenAsync(string value)
        {
            await SetValueAsync(Constants.PushRegisteredTokenKey, value, await GetDefaultStorageOptionsAsync());
        }

        public async Task<bool> GetUsesKeyConnectorAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<bool?>(Constants.UsesKeyConnectorKey(reconciledOptions.UserId),
                reconciledOptions) ?? false;
        }

        public async Task SetUsesKeyConnectorAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.UsesKeyConnectorKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<ForcePasswordResetReason?> GetForcePasswordResetReasonAsync(string userId = null)
        {
            var reconcileOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return (await GetAccountAsync(reconcileOptions))?.Profile?.ForcePasswordResetReason;
        }

        public async Task SetForcePasswordResetReasonAsync(ForcePasswordResetReason? value, string userId = null)
        {
            var reconcileOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconcileOptions);
            account.Profile.ForcePasswordResetReason = value;
            await SaveAccountAsync(account, reconcileOptions);
        }

        public async Task<Dictionary<string, OrganizationData>> GetOrganizationsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<Dictionary<string, OrganizationData>>(
                Constants.OrganizationsKey(reconciledOptions.UserId), reconciledOptions);
        }

        public async Task SetOrganizationsAsync(Dictionary<string, OrganizationData> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.OrganizationsKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<PasswordGenerationOptions> GetPasswordGenerationOptionsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<PasswordGenerationOptions>(Constants.PassGenOptionsKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        public async Task SetPasswordGenerationOptionsAsync(PasswordGenerationOptions value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.PassGenOptionsKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<UsernameGenerationOptions> GetUsernameGenerationOptionsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<UsernameGenerationOptions>(
                Constants.UsernameGenOptionsKey(reconciledOptions.UserId), reconciledOptions);
        }

        public async Task SetUsernameGenerationOptionsAsync(UsernameGenerationOptions value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.UsernameGenOptionsKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<List<GeneratedPasswordHistory>> GetEncryptedPasswordGenerationHistory(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<List<GeneratedPasswordHistory>>(
                Constants.PassGenHistoryKey(reconciledOptions.UserId), reconciledOptions);
        }

        public async Task SetEncryptedPasswordGenerationHistoryAsync(List<GeneratedPasswordHistory> value,
            string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.PassGenHistoryKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<Dictionary<string, SendData>> GetEncryptedSendsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<Dictionary<string, SendData>>(Constants.SendsKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        public async Task SetEncryptedSendsAsync(Dictionary<string, SendData> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.SendsKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        public async Task<Dictionary<string, object>> GetSettingsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<Dictionary<string, object>>(Constants.SettingsKey(reconciledOptions.UserId),
                reconciledOptions);
        }

        public async Task SetSettingsAsync(Dictionary<string, object> value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.SettingsKey(reconciledOptions.UserId), value, reconciledOptions);
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
            return await GetValueAsync<string>(Constants.TwoFactorTokenKey(reconciledOptions.Email), reconciledOptions);
        }

        public async Task SetTwoFactorTokenAsync(string value, string email = null)
        {
            var reconciledOptions =
                ReconcileOptions(new StorageOptions { Email = email }, await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.TwoFactorTokenKey(reconciledOptions.Email), value, reconciledOptions);
        }

        public async Task<bool> GetApprovePasswordlessLoginsAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<bool?>(Constants.ApprovePasswordlessLoginsKey(reconciledOptions.UserId),
                reconciledOptions) ?? false;
        }

        public async Task SetApprovePasswordlessLoginsAsync(bool? value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.ApprovePasswordlessLoginsKey(reconciledOptions.UserId), value,
                reconciledOptions);
        }

        public async Task<PasswordlessRequestNotification> GetPasswordlessLoginNotificationAsync()
        {
            return await GetValueAsync<PasswordlessRequestNotification>(Constants.PasswordlessLoginNotificationKey,
                await GetDefaultStorageOptionsAsync());
        }

        public async Task SetPasswordlessLoginNotificationAsync(PasswordlessRequestNotification value)
        {
            await SetValueAsync(Constants.PasswordlessLoginNotificationKey, value,
                await GetDefaultStorageOptionsAsync());
        }

        public async Task SetAvatarColorAsync(string value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Profile.AvatarColor = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<string> GetAvatarColorAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync())
            ))?.Profile?.AvatarColor;
        }

        public async Task<string> GetPreLoginEmailAsync()
        {
            var options = await GetDefaultStorageOptionsAsync();
            return await GetValueAsync<string>(Constants.PreLoginEmailKey, options);
        }

        public async Task SetPreLoginEmailAsync(string value)
        {
            var options = await GetDefaultStorageOptionsAsync();
            await SetValueAsync(Constants.PreLoginEmailKey, value, options);
        }

        public async Task<AccountDecryptionOptions> GetAccountDecryptionOptions(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync())
            ))?.Profile?.UserDecryptionOptions;
        }

        public async Task<bool> GetShouldTrustDeviceAsync()
        {
            return await _storageMediatorService.GetAsync<bool>(Constants.ShouldTrustDevice);
        }

        public async Task SetShouldTrustDeviceAsync(bool value)
        {
            await _storageMediatorService.SaveAsync(Constants.ShouldTrustDevice, value);
        }

        public async Task<PendingAdminAuthRequest> GetPendingAdminAuthRequestAsync(string userId = null)
        {
            try
            {
                // GetAsync will throw an ArgumentException exception if there isn't a value to deserialize 
                return await _storageMediatorService.GetAsync<PendingAdminAuthRequest>(await ComposeKeyAsync(Constants.PendingAdminAuthRequest, userId), true);
            }
            catch (ArgumentException)
            {
                return null;
            }

        }

        public async Task SetPendingAdminAuthRequestAsync(PendingAdminAuthRequest value, string userId = null)
        {
            await _storageMediatorService.SaveAsync(await ComposeKeyAsync(Constants.PendingAdminAuthRequest, userId), value, true);
        }

        public ConfigResponse GetConfigs()
        {
            return _storageMediatorService.Get<ConfigResponse>(Constants.ConfigsKey);
        }

        public void SetConfigs(ConfigResponse value)
        {
            _storageMediatorService.Save(Constants.ConfigsKey, value);
        }

        public async Task SetUserHasMasterPasswordAsync(bool value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Profile.UserDecryptionOptions.HasMasterPassword = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<BwRegion?> GetActiveUserRegionAsync()
        {
            return await GetActiveUserCustomDataAsync(a => a?.Settings?.Region);
        }

        public async Task<BwRegion?> GetPreAuthRegionAsync()
        {
            return await _storageMediatorService.GetAsync<BwRegion?>(Constants.RegionEnvironment);
        }

        public async Task SetPreAuthRegionAsync(BwRegion value)
        {
            await _storageMediatorService.SaveAsync(Constants.RegionEnvironment, value);
        }

        public async Task<bool> GetShouldCheckOrganizationUnassignedItemsAsync(string userId = null)
        {
            return await _storageMediatorService.GetAsync<bool?>(await ComposeKeyAsync(Constants.ShouldCheckOrganizationUnassignedItemsKey, userId)) ?? true;
        }

        public async Task SetShouldCheckOrganizationUnassignedItemsAsync(bool shouldCheck, string userId = null)
        {
            await _storageMediatorService.SaveAsync<bool?>(await ComposeKeyAsync(Constants.ShouldCheckOrganizationUnassignedItemsKey, userId), shouldCheck);
        }

        // Helpers

        [Obsolete("Use IStorageMediatorService instead")]
        private async Task<T> GetValueAsync<T>(string key, StorageOptions options)
        {
            return await GetStorageService(options).GetAsync<T>(key);
        }

        [Obsolete("Use IStorageMediatorService instead")]
        private async Task SetValueAsync<T>(string key, T value, StorageOptions options)
        {
            if (value == null)
            {
                await GetStorageService(options).RemoveAsync(key);
                return;
            }
            await GetStorageService(options).SaveAsync(key, value);
        }

        [Obsolete("Use IStorageMediatorService instead")]
        private IStorageService GetStorageService(StorageOptions options)
        {
            return options.UseSecureStorage.GetValueOrDefault(false) ? _secureStorageService : _storageService;
        }

        private async Task<string> ComposeKeyAsync(Func<string, string> userDependantKey, string userId = null)
        {
            return userDependantKey(userId ?? await GetActiveUserIdAsync());
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
            await Task.WhenAll(
                SetUserKeyAutoUnlockAsync(null, userId),
                SetUserKeyBiometricUnlockAsync(null, userId),
                SetProtectedPinAsync(null, userId),
                SetPinKeyEncryptedUserKeyAsync(null, userId),
                SetKeyHashAsync(null, userId),
                SetOrgKeysEncryptedAsync(null, userId),
                SetPrivateKeyEncryptedAsync(null, userId),
                SetLastActiveTimeAsync(null, userId),
                SetPreviousPageInfoAsync(null, userId),
                SetInvalidUnlockAttemptsAsync(null, userId),
                SetCiphersLocalDataAsync(null, userId),
                SetEncryptedCiphersAsync(null, userId),
                SetEncryptedCollectionsAsync(null, userId),
                SetLastSyncAsync(null, userId),
                SetEncryptedFoldersAsync(null, userId),
                SetEncryptedPoliciesAsync(null, userId),
                SetUsesKeyConnectorAsync(null, userId),
                SetOrganizationsAsync(null, userId),
                SetEncryptedPasswordGenerationHistoryAsync(null, userId),
                SetEncryptedSendsAsync(null, userId),
                SetSettingsAsync(null, userId),
                SetEncKeyEncryptedAsync(null, userId),
                SetKeyEncryptedAsync(null, userId),
                SetPinProtectedAsync(null, userId));
        }

        private async Task ScaffoldNewAccountAsync(Account account)
        {
            await CheckStateAsync();

            account.Settings.EnvironmentUrls = await GetPreAuthEnvironmentUrlsAsync();
            account.Settings.Region = await GetPreAuthRegionAsync();

            // Storage
            var state = await GetStateFromStorageAsync() ?? new State();
            if (state.Accounts == null)
            {
                state.Accounts = new Dictionary<string, Account>();
            }

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

            // Check if account has logged in before by checking a guaranteed non-null pref
            if (await GetVaultTimeoutActionAsync(account.Profile.UserId) == null)
            {
                // Account has never logged in, set defaults
                await SetVaultTimeoutAsync(Constants.VaultTimeoutDefault, account.Profile.UserId);
                await SetVaultTimeoutActionAsync(VaultTimeoutAction.Lock, account.Profile.UserId);
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

        /// <summary>
        /// Gets the default options for storage.
        /// If it's only used for composing the constant key with the user id
        /// then use <see cref="ComposeKeyAsync(Func{string, string}, string)"/> instead
        /// which saves time if the user id is already known
        /// </summary>
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

        public async Task ReloadStateAsync()
        {
            _state = await GetStateFromStorageAsync() ?? new State();
        }

        private async Task CheckStateAsync()
        {
            if (!_migrationChecked)
            {
                var migrationService = ServiceContainer.Resolve<IStateMigrationService>();
                await migrationService.MigrateIfNeededAsync();
                _migrationChecked = true;
            }

            if (_state == null)
            {
                await ReloadStateAsync();
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

        public async Task<bool> GetShouldConnectToWatchAsync(string userId = null)
        {
            var reconciledOptions =
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<bool?>(Constants.ShouldConnectToWatchKey(reconciledOptions.UserId),
                reconciledOptions) ?? false;
        }

        public async Task SetShouldConnectToWatchAsync(bool shouldConnect, string userId = null)
        {
            var reconciledOptions =
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.ShouldConnectToWatchKey(reconciledOptions.UserId), shouldConnect,
                reconciledOptions);
            await SetLastUserShouldConnectToWatchAsync(shouldConnect);
        }

        public async Task<bool> GetLastUserShouldConnectToWatchAsync()
        {
            return await GetValueAsync<bool?>(Constants.LastUserShouldConnectToWatchKey,
                await GetDefaultStorageOptionsAsync()) ?? false;
        }

        private async Task SetLastUserShouldConnectToWatchAsync(bool? shouldConnect = null)
        {
            await SetValueAsync(Constants.LastUserShouldConnectToWatchKey,
                shouldConnect ?? await GetShouldConnectToWatchAsync(), await GetDefaultStorageOptionsAsync());
        }

        [Obsolete("Use GetPinKeyEncryptedUserKeyAsync instead, left for migration purposes")]
        public async Task<string> GetPinProtectedAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<string>(Constants.PinProtectedKey(reconciledOptions.UserId), reconciledOptions);
        }

        [Obsolete("Use SetPinKeyEncryptedUserKeyAsync instead")]
        public async Task SetPinProtectedAsync(string value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.PinProtectedKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        [Obsolete("Use GetPinKeyEncryptedUserKeyEphemeralAsync instead, left for migration purposes")]
        public async Task<EncString> GetPinProtectedKeyAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultInMemoryOptionsAsync())
            ))?.VolatileData?.PinProtectedKey;
        }

        [Obsolete("Use SetPinKeyEncryptedUserKeyEphemeralAsync instead")]
        public async Task SetPinProtectedKeyAsync(EncString value, string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultInMemoryOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.VolatileData.PinProtectedKey = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        [Obsolete("Use GetMasterKeyEncryptedUserKeyAsync instead, left for migration purposes")]
        public async Task<string> GetEncKeyEncryptedAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            return await GetValueAsync<string>(Constants.EncKeyKey(reconciledOptions.UserId), reconciledOptions);
        }

        [Obsolete("Use SetMasterKeyEncryptedUserKeyAsync instead, left for migration purposes")]
        public async Task SetEncKeyEncryptedAsync(string value, string userId)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultStorageOptionsAsync());
            await SetValueAsync(Constants.EncKeyKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        [Obsolete("Left for migration purposes")]
        public async Task SetKeyEncryptedAsync(string value, string userId)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultSecureStorageOptionsAsync());
            await SetValueAsync(Constants.KeyKey(reconciledOptions.UserId), value, reconciledOptions);
        }

        [Obsolete("Use GetUserKeyAutoUnlock instead, left for migration purposes")]
        public async Task<string> GetKeyEncryptedAsync(string userId = null)
        {
            var reconciledOptions = ReconcileOptions(new StorageOptions { UserId = userId },
                await GetDefaultSecureStorageOptionsAsync());
            return await GetValueAsync<string>(Constants.KeyKey(reconciledOptions.UserId), reconciledOptions);
        }

        [Obsolete("Use GetMasterKeyAsync instead, left for migration purposes")]
        public async Task<SymmetricCryptoKey> GetKeyDecryptedAsync(string userId = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(new StorageOptions { UserId = userId }, await GetDefaultInMemoryOptionsAsync())
            ))?.VolatileData?.Key;
        }
    }
}
