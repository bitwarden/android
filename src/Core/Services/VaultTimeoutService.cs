using Bit.Core.Abstractions;
using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Enums;

namespace Bit.Core.Services
{
    public class VaultTimeoutService : IVaultTimeoutService
    {
        private readonly ICryptoService _cryptoService;
        private readonly IStateService _stateService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IFolderService _folderService;
        private readonly ICipherService _cipherService;
        private readonly ICollectionService _collectionService;
        private readonly ISearchService _searchService;
        private readonly IMessagingService _messagingService;
        private readonly ITokenService _tokenService;
        private readonly IPolicyService _policyService;
        private readonly IKeyConnectorService _keyConnectorService;
        private readonly Action<bool> _lockedCallback;
        private readonly Func<Tuple<string, bool, bool>, Task> _loggedOutCallback;

        public VaultTimeoutService(
            ICryptoService cryptoService,
            IStateService stateService,
            IPlatformUtilsService platformUtilsService,
            IFolderService folderService,
            ICipherService cipherService,
            ICollectionService collectionService,
            ISearchService searchService,
            IMessagingService messagingService,
            ITokenService tokenService,
            IPolicyService policyService,
            IKeyConnectorService keyConnectorService,
            Action<bool> lockedCallback,
            Func<Tuple<string, bool, bool>, Task> loggedOutCallback)
        {
            _cryptoService = cryptoService;
            _stateService = stateService;
            _platformUtilsService = platformUtilsService;
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

        public long? DelayLockAndLogoutMs { get; set; }

        public async Task<bool> IsLockedAsync(string userId = null)
        {
            var hasKey = await _cryptoService.HasKeyAsync(userId);
            if (hasKey)
            {
                var biometricSet = await IsBiometricLockSetAsync(userId);
                if (biometricSet && _stateService.BiometricLocked)
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

            if (await ShouldTimeoutAsync())
            {
                await ExecuteTimeoutActionAsync();
            }
        }

        public async Task<bool> ShouldTimeoutAsync(string userId = null)
        {
            var authed = await _stateService.IsAuthenticatedAsync(userId);
            if (!authed)
            {
                return false;
            }
            var vaultTimeoutAction = await _stateService.GetVaultTimeoutActionAsync(userId);
            if (vaultTimeoutAction == "lock" && await IsLockedAsync(userId))
            {
                return false;
            }
            var vaultTimeoutMinutes = await GetVaultTimeout(userId);
            if (vaultTimeoutMinutes < 0 || vaultTimeoutMinutes == null)
            {
                return false;
            }
            if (vaultTimeoutMinutes == 0 && !DelayLockAndLogoutMs.HasValue)
            {
                return true;
            }
            var lastActiveTime = await _stateService.GetLastActiveTimeAsync(userId);
            if (lastActiveTime == null)
            {
                return false;
            }
            var diffMs = _platformUtilsService.GetActiveTime() - lastActiveTime;
            if (DelayLockAndLogoutMs.HasValue && diffMs < DelayLockAndLogoutMs)
            {
                return false;
            }
            var vaultTimeoutMs = vaultTimeoutMinutes * 60000;
            return diffMs >= vaultTimeoutMs;
        }

        public async Task ExecuteTimeoutActionAsync(string userId = null)
        {
            var action = await _stateService.GetVaultTimeoutActionAsync(userId);
            if (action == "logOut")
            {
                await LogOutAsync(false, userId);
            }
            else
            {
                await LockAsync(true, false, userId);
            }
        }

        public async Task LockAsync(bool allowSoftLock = false, bool userInitiated = false, string userId = null)
        {
            var authed = await _stateService.IsAuthenticatedAsync(userId);
            if (!authed)
            {
                return;
            }

            if (await _keyConnectorService.GetUsesKeyConnector()) {
                var pinSet = await IsPinLockSetAsync(userId);
                var pinLock = (pinSet.Item1 && _stateService.GetPinProtectedAsync(userId) != null) || pinSet.Item2;

                if (!pinLock && !await IsBiometricLockSetAsync())
                {
                    await LogOutAsync(userInitiated, userId);
                    return;
                }
            }

            if (allowSoftLock)
            {
                _stateService.BiometricLocked = await IsBiometricLockSetAsync();
                if (_stateService.BiometricLocked)
                {
                    _messagingService.Send("locked", userInitiated);
                    _lockedCallback?.Invoke(userInitiated);
                    return;
                }
            }
            await Task.WhenAll(
                _cryptoService.ClearKeyAsync(userId),
                _cryptoService.ClearOrgKeysAsync(true, userId),
                _cryptoService.ClearKeyPairAsync(true, userId),
                _cryptoService.ClearEncKeyAsync(true, userId));

            _folderService.ClearCache();
            await _cipherService.ClearCacheAsync();
            _collectionService.ClearCache();
            _searchService.ClearIndex();
            _messagingService.Send("locked", userInitiated);
            _lockedCallback?.Invoke(userInitiated);
        }
        
        public async Task LogOutAsync(bool userInitiated = true, string userId = null)
        {
            if(_loggedOutCallback != null)
            {
                await _loggedOutCallback.Invoke(new Tuple<string, bool, bool>(userId, userInitiated, false));
            }
        }

        public async Task SetVaultTimeoutOptionsAsync(int? timeout, string action)
        {
            await _stateService.SetVaultTimeoutAsync(timeout);
            await _stateService.SetVaultTimeoutActionAsync(action);
            await _cryptoService.ToggleKeyAsync();
            await _tokenService.ToggleTokensAsync();
        }

        public async Task<Tuple<bool, bool>> IsPinLockSetAsync(string userId = null)
        {
            var protectedPin = await _stateService.GetProtectedPinAsync(userId);
            var pinProtectedKey = await _stateService.GetPinProtectedAsync(userId);
            return new Tuple<bool, bool>(protectedPin != null, pinProtectedKey != null);
        }

        public async Task<bool> IsBiometricLockSetAsync(string userId = null)
        {
            var biometricLock = await _stateService.GetBiometricUnlockAsync(userId);
            return biometricLock.GetValueOrDefault();
        }

        public async Task ClearAsync(string userId = null)
        {
            await _stateService.SetPinProtectedAsync(null, userId);
            await _stateService.SetProtectedPinAsync(null, userId);
        }

        public async Task<int?> GetVaultTimeout(string userId = null) {
            var vaultTimeout = await _stateService.GetVaultTimeoutAsync();

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
                    await _stateService.SetVaultTimeoutAsync(timeout);
                }

                return timeout;
            }

            return vaultTimeout;
        }
    }
}
