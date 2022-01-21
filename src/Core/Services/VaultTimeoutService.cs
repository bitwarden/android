using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Enums;

namespace Bit.Core.Services
{
    public class VaultTimeoutService : IVaultTimeoutService
    {
        private readonly ICryptoService _cryptoService;
        private readonly IUserService _userService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IStorageService _storageService;
        private readonly IFolderService _folderService;
        private readonly ICipherService _cipherService;
        private readonly ICollectionService _collectionService;
        private readonly ISearchService _searchService;
        private readonly IMessagingService _messagingService;
        private readonly ITokenService _tokenService;
        private readonly IPolicyService _policyService;
        private readonly IKeyConnectorService _keyConnectorService;
        private readonly Action<bool> _lockedCallback;
        private readonly Func<bool, Task> _loggedOutCallback;

        public VaultTimeoutService(
            ICryptoService cryptoService,
            IUserService userService,
            IPlatformUtilsService platformUtilsService,
            IStorageService storageService,
            IFolderService folderService,
            ICipherService cipherService,
            ICollectionService collectionService,
            ISearchService searchService,
            IMessagingService messagingService,
            ITokenService tokenService,
            IPolicyService policyService,
            IKeyConnectorService keyConnectorService,
            Action<bool> lockedCallback,
            Func<bool, Task> loggedOutCallback)
        {
            _cryptoService = cryptoService;
            _userService = userService;
            _platformUtilsService = platformUtilsService;
            _storageService = storageService;
            _folderService = folderService;
            _cipherService = cipherService;
            _collectionService = collectionService;
            _searchService = searchService;
            _messagingService = messagingService;
            _tokenService = tokenService;
            _policyService = policyService;
            _keyConnectorService = keyConnectorService;
            _lockedCallback = lockedCallback;
            _loggedOutCallback = loggedOutCallback;
        }

        public EncString PinProtectedKey { get; set; } = null;
        public bool BiometricLocked { get; set; } = true;
        public long? DelayLockAndLogoutMs { get; set; }

        public async Task<bool> IsLockedAsync()
        {
            var hasKey = await _cryptoService.HasKeyAsync();
            if (hasKey)
            {
                var biometricSet = await IsBiometricLockSetAsync();
                if (biometricSet && BiometricLocked)
                {
                    return true;
                }
            }
            return !hasKey;
        }

        public async Task CheckVaultTimeoutAsync()
        {
            if (_platformUtilsService.IsViewOpen())
            {
                return;
            }
            var authed = await _userService.IsAuthenticatedAsync();
            if (!authed)
            {
                return;
            }
            if (await IsLockedAsync())
            {
                return;
            }
            var vaultTimeoutMinutes = await GetVaultTimeout();
            if (vaultTimeoutMinutes < 0 || vaultTimeoutMinutes == null)
            {
                return;
            }
            if (vaultTimeoutMinutes == 0 && !DelayLockAndLogoutMs.HasValue)
            {
                await LockOrLogout();
            }
            var lastActiveTime = await _storageService.GetAsync<long?>(Constants.LastActiveTimeKey);
            if (lastActiveTime == null)
            {
                return;
            }
            var diffMs = _platformUtilsService.GetActiveTime() - lastActiveTime;
            if (DelayLockAndLogoutMs.HasValue && diffMs < DelayLockAndLogoutMs)
            {
                return;
            }
            var vaultTimeoutMs = vaultTimeoutMinutes * 60000;
            if (diffMs >= vaultTimeoutMs)
            {
                await LockOrLogout();
            }

        }

        private async Task LockOrLogout()
        {
            // Pivot based on saved action
            var action = await _storageService.GetAsync<string>(Constants.VaultTimeoutActionKey);
            if (action == "logOut")
            {
                await LogOutAsync();
            }
            else
            {
                await LockAsync(true);
            }
        }

        public async Task LockAsync(bool allowSoftLock = false, bool userInitiated = false)
        {
            var authed = await _userService.IsAuthenticatedAsync();
            if (!authed)
            {
                return;
            }

            if (await _keyConnectorService.GetUsesKeyConnector()) {
                var pinSet = await IsPinLockSetAsync();
                var pinLock = (pinSet.Item1 && PinProtectedKey != null) || pinSet.Item2;

                if (!pinLock && !await IsBiometricLockSetAsync())
                {
                    await LogOutAsync();
                    return;
                }
            }

            if (allowSoftLock)
            {
                BiometricLocked = await IsBiometricLockSetAsync();
                if (BiometricLocked)
                {
                    _messagingService.Send("locked", userInitiated);
                    _lockedCallback?.Invoke(userInitiated);
                    return;
                }
            }
            await Task.WhenAll(
                _cryptoService.ClearKeyAsync(),
                _cryptoService.ClearOrgKeysAsync(true),
                _cryptoService.ClearKeyPairAsync(true),
                _cryptoService.ClearEncKeyAsync(true));

            _folderService.ClearCache();
            await _cipherService.ClearCacheAsync();
            _collectionService.ClearCache();
            _searchService.ClearIndex();
            _messagingService.Send("locked", userInitiated);
            _lockedCallback?.Invoke(userInitiated);
        }
        
        public async Task LogOutAsync()
        {
            if(_loggedOutCallback != null)
            {
                await _loggedOutCallback.Invoke(false);
            }
        }

        public async Task SetVaultTimeoutOptionsAsync(int? timeout, string action)
        {
            await _storageService.SaveAsync(Constants.VaultTimeoutKey, timeout);
            await _storageService.SaveAsync(Constants.VaultTimeoutActionKey, action);
            await _cryptoService.ToggleKeyAsync();
            await _tokenService.ToggleTokensAsync();
        }

        public async Task<Tuple<bool, bool>> IsPinLockSetAsync()
        {
            var protectedPin = await _storageService.GetAsync<string>(Constants.ProtectedPin);
            var pinProtectedKey = await _storageService.GetAsync<string>(Constants.PinProtectedKey);
            return new Tuple<bool, bool>(protectedPin != null, pinProtectedKey != null);
        }

        public async Task<bool> IsBiometricLockSetAsync()
        {
            var biometricLock = await _storageService.GetAsync<bool?>(Constants.BiometricUnlockKey);
            return biometricLock.GetValueOrDefault();
        }

        public async Task ClearAsync()
        {
            PinProtectedKey = null;
            await _storageService.RemoveAsync(Constants.ProtectedPin);
        }

        public async Task<int?> GetVaultTimeout() {
            var vaultTimeout = await _storageService.GetAsync<int?>(Constants.VaultTimeoutKey);

            if (await _policyService.PolicyAppliesToUser(PolicyType.MaximumVaultTimeout)) {
                var policy = (await _policyService.GetAll(PolicyType.MaximumVaultTimeout)).First();
                // Remove negative values, and ensure it's smaller than maximum allowed value according to policy
                var policyTimeout = _policyService.GetPolicyInt(policy, "minutes");
                if (!policyTimeout.HasValue)
                {
                    return vaultTimeout;
                }

                var timeout = vaultTimeout.HasValue ? Math.Min(vaultTimeout.Value, policyTimeout.Value) : policyTimeout.Value;

                if (timeout < 0) {
                    timeout = policyTimeout.Value;
                }

                // We really shouldn't need to set the value here, but multiple services relies on this value being correct.
                if (vaultTimeout != timeout) {
                    await _storageService.SaveAsync(Constants.VaultTimeoutKey, timeout);
                }

                return timeout;
            }

            return vaultTimeout;
        }
    }
}
