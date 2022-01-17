using System;
using Bit.Core.Abstractions;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Newtonsoft.Json;

namespace Bit.Core.Services
{
    public class StateService : IStateService
    {
        private readonly IStorageService _storageService;
        private readonly IStorageService _secureStorageService;

        private State _state;

        public bool BiometricLocked { get; set; }

        public ExtendedObservableCollection<AccountView> Accounts { get; set; }

        public StateService(IStorageService storageService, IStorageService secureStorageService)
        {
            _storageService = storageService;
            _secureStorageService = secureStorageService;
        }

        public async Task<string> GetActiveUserIdAsync(StorageOptions options = null)
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

        public async Task SetActiveUserAsync(string userId)
        {
            await ValidateUserAsync(userId);
            await CheckStateAsync(false);
            var state = await GetStateFromStorageAsync();
            state.ActiveUserId = userId;
            await SaveStateToStorageAsync(state);
            _state.ActiveUserId = userId;
            await RefreshAccountList();
            
            // Update pre-auth settings based on now-active user
            var rememberEmail = await GetRememberEmailAsync();
            if (rememberEmail.GetValueOrDefault(true))
            {
                await SetRememberedEmailAsync(await GetEmailAsync());
            }
            await SetPreAuthEnvironmentUrlsAsync(await GetEnvironmentUrlsAsync());
        }

        public async Task<bool> IsAuthenticatedAsync(StorageOptions options = null)
        {
            return await GetAccessTokenAsync(options) != null && await GetActiveUserIdAsync(options) != null;
        }

        public async Task<bool> HasMultipleAccountsAsync()
        {
            await CheckStateAsync(false);
            return _state.Accounts?.Count > 1;
        }

        public async Task AddAccountAsync(Account account)
        {
            await ScaffoldNewAccountAsync(account);
            await SetActiveUserAsync(account.Profile.UserId);
        }

        public async Task CleanAsync(string userId)
        {
            if (_state?.Accounts != null && (userId == null || userId == await GetActiveUserIdAsync()))
            {
                // Find the next user to make active
                foreach (var account in _state.Accounts)
                {
                    var uid = account.Value?.Profile?.UserId;
                    if (uid == null)
                    {
                        continue;
                    }
                    if (await IsAuthenticatedAsync(new StorageOptions { UserId = uid }))
                    {
                        await SetActiveUserAsync(uid);
                        break;
                    }
                    await SetActiveUserAsync(null);
                }
            }

            await RemoveAccountAsync(userId);
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

        public async Task<EnvironmentUrlData> GetEnvironmentUrlsAsync(StorageOptions options = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(options, await GetDefaultStorageOptionsAsync())
            ))?.Settings?.EnvironmentUrls;
        }

        public async Task SetEnvironmentUrlsAsync(EnvironmentUrlData value, StorageOptions options = null)
        {
            var reconciledOptions =
                ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Settings.EnvironmentUrls = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<bool?> GetBiometricUnlockAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.BiometricUnlockKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetBiometricUnlockAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.BiometricUnlockKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool> CanAccessPremiumAsync(StorageOptions options = null)
        {
            if (!await IsAuthenticatedAsync(options))
            {
                return false;
            }

            var account = await GetAccountAsync(
                ReconcileOptions(options, await GetDefaultStorageOptionsAsync()));
            if (account.Profile.HasPremiumPersonally.GetValueOrDefault())
            {
                return true;
            }

            var userId = account.Profile?.UserId;
            if (userId == null)
            {
                userId = await GetActiveUserIdAsync();
            }
            var organizationService = ServiceContainer.Resolve<IOrganizationService>("organizationService");
            var organizations = await organizationService.GetAllAsync(userId);
            return organizations?.Any(o => o.UsersGetPremium && o.Enabled) ?? false;
        }

        public async Task<string> GetProtectedPinAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultSecureStorageOptionsAsync());
            var key = Constants.ProtectedPinKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetProtectedPinAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultSecureStorageOptionsAsync());
            var key = Constants.ProtectedPinKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetPinProtectedAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PinProtectedKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetPinProtectedAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PinProtectedKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<EncString> GetPinProtectedCachedAsync(StorageOptions options = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(options, await GetDefaultStorageOptionsAsync())
            ))?.Settings?.PinProtected;
        }

        public async Task SetPinProtectedCachedAsync(EncString value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Settings.PinProtected = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<KdfType?> GetKdfTypeAsync(StorageOptions options = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(options, await GetDefaultStorageOptionsAsync())
            ))?.Profile?.KdfType;
        }

        public async Task SetKdfTypeAsync(KdfType? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Profile.KdfType = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<int?> GetKdfIterationsAsync(StorageOptions options = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(options, await GetDefaultStorageOptionsAsync())
            ))?.Profile?.KdfIterations;
        }

        public async Task SetKdfIterationsAsync(int? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Profile.KdfIterations = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<string> GetKeyEncryptedAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultSecureStorageOptionsAsync());
            var key = Constants.KeyKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetKeyEncryptedAsync(string value, StorageOptions options)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultSecureStorageOptionsAsync());
            var key = Constants.KeyKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<SymmetricCryptoKey> GetKeyDecryptedAsync(StorageOptions options = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(options, await GetDefaultInMemoryOptionsAsync())
            ))?.Keys?.Key;
        }

        public async Task SetKeyDecryptedAsync(SymmetricCryptoKey value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultInMemoryOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Keys.Key = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<string> GetKeyHashAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.KeyHashKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetKeyHashAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.KeyHashKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetKeyHashCachedAsync(StorageOptions options = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(options, await GetDefaultStorageOptionsAsync())
            ))?.Keys?.KeyHash;
        }

        public async Task SetKeyHashCachedAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Keys.KeyHash = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<string> GetEncKeyEncryptedAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.EncKeyKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetEncKeyEncryptedAsync(string value, StorageOptions options)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.EncKeyKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<SymmetricCryptoKey> GetEncKeyDecryptedAsync(StorageOptions options = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(options, await GetDefaultInMemoryOptionsAsync())
            ))?.Keys?.EncKey;
        }

        public async Task SetEncKeyDecryptedAsync(SymmetricCryptoKey value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultInMemoryOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Keys.EncKey = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<Dictionary<string, string>> GetOrgKeysEncryptedAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.EncOrgKeysKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, string>>(key, reconciledOptions);
        }

        public async Task SetOrgKeysEncryptedAsync(Dictionary<string, string> value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.EncOrgKeysKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, SymmetricCryptoKey>> GetOrgKeysDecryptedAsync(
            StorageOptions options = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(options, await GetDefaultInMemoryOptionsAsync())
            ))?.Keys?.OrganizationKeys;
        }

        public async Task SetOrgKeysDecryptedAsync(Dictionary<string, SymmetricCryptoKey> value,
            StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultInMemoryOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Keys.OrganizationKeys = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<byte[]> GetPublicKeyAsync(StorageOptions options = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(options, await GetDefaultStorageOptionsAsync())
            ))?.Keys?.PublicKey;
        }

        public async Task SetPublicKeyAsync(byte[] value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Keys.PublicKey = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<string> GetPrivateKeyEncryptedAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.EncPrivateKeyKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetPrivateKeyEncryptedAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.EncPrivateKeyKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<byte[]> GetPrivateKeyDecryptedAsync(StorageOptions options = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(options, await GetDefaultInMemoryOptionsAsync())
            ))?.Keys?.PrivateKey;
        }

        public async Task SetPrivateKeyDecryptedAsync(byte[] value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultInMemoryOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Keys.PrivateKey = value;
            await SaveAccountAsync(account);
        }

        public async Task<List<string>> GetAutofillBlacklistedUrisAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.AutofillBlacklistedUrisKey(reconciledOptions.UserId);
            return await GetValueAsync<List<string>>(key, reconciledOptions);
        }

        public async Task SetAutofillBlacklistedUrisAsync(List<string> value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.AutofillBlacklistedUrisKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetAutofillTileAddedAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.AutofillTileAdded;
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetAutofillTileAddedAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.AutofillTileAdded;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetEmailAsync(StorageOptions options = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(options, await GetDefaultStorageOptionsAsync())
            ))?.Profile?.Email;
        }

        public async Task<long?> GetLastActiveTimeAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.LastActiveTimeKey(reconciledOptions.UserId);
            return await GetValueAsync<long?>(key, reconciledOptions);
        }

        public async Task SetLastActiveTimeAsync(long? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.LastActiveTimeKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<int?> GetVaultTimeoutAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.VaultTimeoutKey(reconciledOptions.UserId);
            return await GetValueAsync<int?>(key, reconciledOptions) ?? 15;
        }

        public async Task SetVaultTimeoutAsync(int? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.VaultTimeoutKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetVaultTimeoutActionAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.VaultTimeoutActionKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions) ?? "lock";
        }

        public async Task SetVaultTimeoutActionAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.VaultTimeoutActionKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<DateTime?> GetLastFileCacheClearAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.LastFileCacheClearKey;
            return await GetValueAsync<DateTime?>(key, reconciledOptions);
        }

        public async Task SetLastFileCacheClearAsync(DateTime? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.LastFileCacheClearKey;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<PreviousPageInfo> GetPreviousPageInfoAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PreviousPageKey(reconciledOptions.UserId);
            return await GetValueAsync<PreviousPageInfo>(key, reconciledOptions);
        }

        public async Task SetPreviousPageInfoAsync(PreviousPageInfo value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PreviousPageKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<int> GetInvalidUnlockAttemptsAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.InvalidUnlockAttemptsKey(reconciledOptions.UserId);
            return await GetValueAsync<int>(key, reconciledOptions);
        }

        public async Task SetInvalidUnlockAttemptsAsync(int? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.InvalidUnlockAttemptsKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetLastBuildAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.LastBuildKey;
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetLastBuildAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.LastBuildKey;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetDisableFaviconAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.DisableFaviconKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetDisableFaviconAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.DisableFaviconKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetDisableAutoTotpCopyAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.DisableAutoTotpCopyKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetDisableAutoTotpCopyAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.DisableAutoTotpCopyKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetInlineAutofillEnabledAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.InlineAutofillEnabledKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetInlineAutofillEnabledAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.InlineAutofillEnabledKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetAutofillDisableSavePromptAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.AutofillDisableSavePromptKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetAutofillDisableSavePromptAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.AutofillDisableSavePromptKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, Dictionary<string, object>>> GetLocalDataAsync(
            StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.LocalDataKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, Dictionary<string, object>>>(key, reconciledOptions);
        }

        public async Task SetLocalDataAsync(Dictionary<string, Dictionary<string, object>> value,
            StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.LocalDataKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, CipherData>> GetEncryptedCiphersAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.CiphersKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, CipherData>>(key, reconciledOptions);
        }

        public async Task SetEncryptedCiphersAsync(Dictionary<string, CipherData> value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.CiphersKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<int?> GetDefaultUriMatchAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.DefaultUriMatchKey(reconciledOptions.UserId);
            return await GetValueAsync<int?>(key, reconciledOptions);
        }

        public async Task SetDefaultUriMatchAsync(int? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.DefaultUriMatchKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<HashSet<string>> GetNeverDomainsAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.NeverDomainsKey(reconciledOptions.UserId);
            return await GetValueAsync<HashSet<string>>(key, reconciledOptions);
        }

        public async Task SetNeverDomainsAsync(HashSet<string> value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.NeverDomainsKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<int?> GetClearClipboardAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.ClearClipboardKey(reconciledOptions.UserId);
            return await GetValueAsync<int?>(key, reconciledOptions);
        }

        public async Task SetClearClipboardAsync(int? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.ClearClipboardKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, CollectionData>> GetEncryptedCollectionsAsync(
            StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.CollectionsKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, CollectionData>>(key, reconciledOptions);
        }

        public async Task SetEncryptedCollectionsAsync(Dictionary<string, CollectionData> value,
            StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.CollectionsKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool> GetPasswordRepromptAutofillAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PasswordRepromptAutofillKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions) ?? false;
        }

        public async Task SetPasswordRepromptAutofillAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PasswordRepromptAutofillKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool> GetPasswordVerifiedAutofillAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PasswordVerifiedAutofillKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions) ?? false;
        }

        public async Task SetPasswordVerifiedAutofillAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PasswordVerifiedAutofillKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<DateTime?> GetLastSyncAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.LastSyncKey(reconciledOptions.UserId);
            return await GetValueAsync<DateTime?>(key, reconciledOptions);
        }

        public async Task SetLastSyncAsync(DateTime? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.LastSyncKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetSecurityStampAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.SecurityStampKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetSecurityStampAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.SecurityStampKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool> GetEmailVerifiedAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.EmailVerifiedKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions) ?? false;
        }

        public async Task SetEmailVerifiedAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.EmailVerifiedKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool> GetForcePasswordReset(StorageOptions options)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.ForcePasswordResetKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions) ?? false;
        }

        public async Task SetForcePasswordResetAsync(bool? value, StorageOptions options)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.ForcePasswordResetKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool> GetSyncOnRefreshAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.SyncOnRefreshKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions) ?? false;
        }

        public async Task SetSyncOnRefreshAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.SyncOnRefreshKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetRememberedEmailAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.RememberedEmailKey;
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetRememberedEmailAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.RememberedEmailKey;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetRememberEmailAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.RememberEmailKey;
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetRememberEmailAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.RememberEmailKey;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetRememberedOrgIdentifierAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.RememberedOrgIdentifierKey;
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetRememberedOrgIdentifierAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.RememberedOrgIdentifierKey;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetRememberOrgIdentifierAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.RememberOrgIdentifierKey;
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetRememberOrgIdentifierAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.RememberOrgIdentifierKey;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetThemeAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.ThemeKey(reconciledOptions.UserId);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetThemeAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.ThemeKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetAddSitePromptShownAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.AddSitePromptShownKey;
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetAddSitePromptShownAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.AddSitePromptShownKey;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetMigratedFromV1Async(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.MigratedFromV1;
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetMigratedFromV1Async(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.MigratedFromV1;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetMigratedFromV1AutofillPromptShownAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.MigratedFromV1AutofillPromptShown;
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetMigratedFromV1AutofillPromptShownAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.MigratedFromV1AutofillPromptShown;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetTriedV1ResyncAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.TriedV1Resync;
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetTriedV1ResyncAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.TriedV1Resync;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetPushInitialPromptShownAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PushInitialPromptShownKey;
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetPushInitialPromptShownAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PushInitialPromptShownKey;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<DateTime?> GetPushLastRegistrationDateAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PushLastRegistrationDateKey;
            return await GetValueAsync<DateTime?>(key, reconciledOptions);
        }

        public async Task SetPushLastRegistrationDateAsync(DateTime? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PushLastRegistrationDateKey;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetPushCurrentTokenAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PushCurrentTokenKey;
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetPushCurrentTokenAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PushCurrentTokenKey;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<List<EventData>> GetEventCollectionAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.EventCollectionKey;
            return await GetValueAsync<List<EventData>>(key, reconciledOptions);
        }

        public async Task SetEventCollectionAsync(List<EventData> value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.EventCollectionKey;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, FolderData>> GetEncryptedFoldersAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.FoldersKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, FolderData>>(key, reconciledOptions);
        }

        public async Task SetEncryptedFoldersAsync(Dictionary<string, FolderData> value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.FoldersKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, PolicyData>> GetEncryptedPoliciesAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PoliciesKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, PolicyData>>(key, reconciledOptions);
        }

        public async Task SetEncryptedPoliciesAsync(Dictionary<string, PolicyData> value, StorageOptions options)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PoliciesKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetPushRegisteredTokenAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PushRegisteredTokenKey;
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetPushRegisteredTokenAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PushRegisteredTokenKey;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetAppExtensionStartedAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.AppExtensionStartedKey;
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetAppExtensionStartedAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.AppExtensionStartedKey;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool?> GetAppExtensionActivatedAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.AppExtensionActivatedKey;
            return await GetValueAsync<bool?>(key, reconciledOptions);
        }

        public async Task SetAppExtensionActivatedAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.AppExtensionActivatedKey;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetAppIdAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.AppIdKey;
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetAppIdAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.AppIdKey;
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<bool> GetUsesKeyConnectorAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.UsesKeyConnectorKey(reconciledOptions.UserId);
            return await GetValueAsync<bool?>(key, reconciledOptions) ?? false;
        }

        public async Task SetUsesKeyConnectorAsync(bool? value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.UsesKeyConnectorKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, OrganizationData>> GetOrganizationsAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.OrganizationsKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, OrganizationData>>(key, reconciledOptions);
        }

        public async Task SetOrganizationsAsync(Dictionary<string, OrganizationData> value,
            StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.OrganizationsKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<PasswordGenerationOptions> GetPasswordGenerationOptionsAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PassGenOptionsKey(reconciledOptions.UserId);
            return await GetValueAsync<PasswordGenerationOptions>(key, reconciledOptions);
        }

        public async Task SetPasswordGenerationOptionsAsync(PasswordGenerationOptions value,
            StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PassGenOptionsKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<List<GeneratedPasswordHistory>> GetEncryptedPasswordGenerationHistory(
            StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PassGenHistoryKey(reconciledOptions.UserId);
            return await GetValueAsync<List<GeneratedPasswordHistory>>(key, reconciledOptions);
        }

        public async Task SetEncryptedPasswordGenerationHistoryAsync(List<GeneratedPasswordHistory> value,
            StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.PassGenHistoryKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, SendData>> GetEncryptedSendsAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.SendsKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, SendData>>(key, reconciledOptions);
        }

        public async Task SetEncryptedSendsAsync(Dictionary<string, SendData> value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.SendsKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<Dictionary<string, object>> GetSettingsAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.SettingsKey(reconciledOptions.UserId);
            return await GetValueAsync<Dictionary<string, object>>(key, reconciledOptions);
        }

        public async Task SetSettingsAsync(Dictionary<string, object> value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.SettingsKey(reconciledOptions.UserId);
            await SetValueAsync(key, value, reconciledOptions);
        }

        public async Task<string> GetAccessTokenAsync(StorageOptions options = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(options, await GetDefaultStorageOptionsAsync())
            ))?.Tokens?.AccessToken;
        }

        public async Task SetAccessTokenAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Tokens.AccessToken = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<string> GetRefreshTokenAsync(StorageOptions options = null)
        {
            return (await GetAccountAsync(
                ReconcileOptions(options, await GetDefaultStorageOptionsAsync())
            ))?.Tokens?.RefreshToken;
        }

        public async Task SetRefreshTokenAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var account = await GetAccountAsync(reconciledOptions);
            account.Tokens.RefreshToken = value;
            await SaveAccountAsync(account, reconciledOptions);
        }

        public async Task<string> GetTwoFactorTokenAsync(StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.TwoFactorTokenKey(reconciledOptions.Email);
            return await GetValueAsync<string>(key, reconciledOptions);
        }

        public async Task SetTwoFactorTokenAsync(string value, StorageOptions options = null)
        {
            var reconciledOptions = ReconcileOptions(options, await GetDefaultStorageOptionsAsync());
            var key = Constants.TwoFactorTokenKey(reconciledOptions.Email);
            await SetValueAsync(key, value, reconciledOptions);
        }

        // Helpers

        private async Task<T> GetValueAsync<T>(string key, StorageOptions options)
        {
            var value = await GetStorageService(options).GetAsync<T>(key);
            Log("GET", options, key, JsonConvert.SerializeObject(value));
            return value;
        }

        private async Task SetValueAsync<T>(string key, T value, StorageOptions options)
        {
            if (value == null)
            {
                Log("REMOVE", options, key, null);
                await GetStorageService(options).RemoveAsync(key);
                return;
            }
            Log("SET", options, key, JsonConvert.SerializeObject(value));
            await GetStorageService(options).SaveAsync(key, value);
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
                if (_state.Accounts[options.UserId].Keys == null)
                {
                    _state.Accounts[options.UserId].Keys = new Account.AccountKeys();
                }
                return _state.Accounts[options.UserId];
            }

            // Storage
            _state = await GetStateFromStorageAsync();
            if (_state?.Accounts?.ContainsKey(options.UserId) ?? false)
            {
                if (_state.Accounts[options.UserId].Keys == null)
                {
                    _state.Accounts[options.UserId].Keys = new Account.AccountKeys();
                }
                return _state.Accounts[options.UserId];
            }

            return null;
        }

        private async Task SaveAccountAsync(Account account, StorageOptions options = null)
        {
            if (account?.Profile?.UserId == null)
            {
                throw new Exception("account?.Profile?.UserId cannot be null");
            }

            await CheckStateAsync(false);

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
                if (await SkipTokenStorageAsync())
                {
                    state.Accounts[account.Profile.UserId].Tokens.AccessToken = null;
                    state.Accounts[account.Profile.UserId].Tokens.RefreshToken = null;
                }

                await SaveStateToStorageAsync(state);
            }

            await RefreshAccountList();
        }

        private async Task<bool> SkipTokenStorageAsync()
        {
            var timeout = await GetVaultTimeoutAsync();
            var action = await GetVaultTimeoutActionAsync();
            return timeout.HasValue && action == "logOut";
        }

        private async Task RemoveAccountAsync(string userId)
        {
            if (userId == null)
            {
                throw new Exception("userId cannot be null");
            }

            await CheckStateAsync(false);

            // Memory
            if (_state?.Accounts?.ContainsKey(userId) ?? false)
            {
                _state?.Accounts?.Remove(userId);
            }

            // Storage
            var state = await GetStateFromStorageAsync();
            if (state?.Accounts?.ContainsKey(userId) ?? false)
            {
                state.Accounts.Remove(userId);
                await SaveStateToStorageAsync(state);
            }

            // Secure Storage
            var options = new StorageOptions
            {
                UserId = userId,
                UseSecureStorage = true,
            };
            await SetProtectedPinAsync(null, options);
            await SetKeyEncryptedAsync(null, options);
            
            await RefreshAccountList();
        }

        private async Task ScaffoldNewAccountAsync(Account account)
        {
            await CheckStateAsync(false);

            account.Settings.EnvironmentUrls = await GetPreAuthEnvironmentUrlsAsync();

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
        }

        private StorageOptions ReconcileOptions(StorageOptions requestedOptions,
            StorageOptions defaultOptions)
        {
            if (requestedOptions == null)
            {
                return defaultOptions;
            }
            requestedOptions.StorageLocation = requestedOptions.StorageLocation ?? defaultOptions.StorageLocation;
            requestedOptions.UseSecureStorage = requestedOptions.UseSecureStorage ?? defaultOptions.UseSecureStorage;
            requestedOptions.UserId = requestedOptions.UserId ?? defaultOptions.UserId;
            requestedOptions.Email = requestedOptions.Email ?? defaultOptions.Email;
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
            var state = await _storageService.GetAsync<State>(Constants.StateKey);
            Debug.WriteLine(JsonConvert.SerializeObject(state, Formatting.Indented),
                ">>> GetStateFromStorageAsync()");
            return state;
        }

        private async Task SaveStateToStorageAsync(State state)
        {
            await _storageService.SaveAsync(Constants.StateKey, state);
            Debug.WriteLine(JsonConvert.SerializeObject(state, Formatting.Indented),
                ">>> SaveStateToStorageAsync()");
        }

        private async Task PruneInMemoryAccountsAsync()
        {
            // TODO not sure if needed due to pref storage of settings
            // We preserve settings for logged out accounts, but we don't want to consider them when thinking about
            // active account state
            var accounts = new Dictionary<string, Account>();
            foreach (var account in _state.Accounts)
            {
                if (await IsAuthenticatedAsync(new StorageOptions { UserId = account.Value.Profile.UserId }))
                {
                    accounts.Add(account.Key, account.Value);
                }
            }
            _state.Accounts = accounts;
        }

        private async Task CheckStateAsync(bool includeAccountRefresh = true)
        {
            // TODO perform migration if necessary

            if (_state == null)
            {
                _state = await GetStateFromStorageAsync() ?? new State();
            }
            if (includeAccountRefresh)
            {
                await RefreshAccountList();
            }
        }

        private async Task RefreshAccountList()
        {
            Accounts = new ExtendedObservableCollection<AccountView>();
            var accountList = _state?.Accounts?.Values.ToList();
            if (accountList == null)
            {
                return;
            }
            foreach (var account in accountList)
            {
                var accountView = new AccountView(account);
                if (accountView.UserId == _state.ActiveUserId)
                {
                    accountView.AuthStatus = AuthenticationStatus.Active;
                }
                else
                {
                    var vaultTimeService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
                    if (await vaultTimeService.IsLockedAsync(accountView.UserId))
                    // {
                    //     var action = await GetVaultTimeoutActionAsync(new StorageOptions { UserId = accountView.UserId });
                    //     if (action == "logOut")
                    //     {
                    //         accountView.AuthStatus = AuthenticationStatus.LoggedOut;
                    //     }
                    //     else
                    //     {
                    //         accountView.AuthStatus = AuthenticationStatus.Locked;
                    //     }
                    // }
                    // else
                    // {
                        accountView.AuthStatus = AuthenticationStatus.Unlocked;
                    // }
                }
                Accounts.Add(accountView);
            }
        }

        private async Task ValidateUserAsync(string userId)
        {
            if (string.IsNullOrEmpty(userId))
            {
                throw new Exception("userId cannot be null or empty");
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

        private void Log(string tag, StorageOptions options, string key, string value)
        {
            var text = options?.UseSecureStorage ?? false ? "SECURE / " : "";
            text += "Key: " + key + " / ";
            if (value != null)
            {
                text += "Value: " + value;
            }
            Debug.WriteLine(text, ">>> " + tag);
        }
    }
}
