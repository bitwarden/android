using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;

namespace Bit.Core.Services
{
    public enum PinLockType
    {
        Disabled,
        Persistent,
        Transient
    }

    public class VaultTimeoutService : IVaultTimeoutService
    {
        private readonly ICryptoService _cryptoService;
        private readonly IStateService _stateService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IFolderService _folderService;
        private readonly ICipherService _cipherService;
        private readonly ICollectionService _collectionService;
        private readonly ISearchService _searchService;
        private readonly ITokenService _tokenService;
        private readonly IUserVerificationService _userVerificationService;
        private readonly Func<Tuple<string, bool>, Task> _lockedCallback;
        private readonly Func<Tuple<string, bool, bool>, Task> _loggedOutCallback;

        public VaultTimeoutService(
            ICryptoService cryptoService,
            IStateService stateService,
            IPlatformUtilsService platformUtilsService,
            IFolderService folderService,
            ICipherService cipherService,
            ICollectionService collectionService,
            ISearchService searchService,
            ITokenService tokenService,
            IUserVerificationService userVerificationService,
            Func<Tuple<string, bool>, Task> lockedCallback,
            Func<Tuple<string, bool, bool>, Task> loggedOutCallback)
        {
            _cryptoService = cryptoService;
            _stateService = stateService;
            _platformUtilsService = platformUtilsService;
            _folderService = folderService;
            _cipherService = cipherService;
            _collectionService = collectionService;
            _searchService = searchService;
            _tokenService = tokenService;
            _userVerificationService = userVerificationService;
            _lockedCallback = lockedCallback;
            _loggedOutCallback = loggedOutCallback;
        }

        public long? DelayLockAndLogoutMs { get; set; }

        /// <summary>
        /// Determine if the current or provided account is locked.
        /// </summary>
        /// <param name="userId">
        /// Optional specified user, must be provided if not the current account.
        /// </param>
        public async Task<bool> IsLockedAsync(string userId = null)
        {
            // If biometrics are used, we can use the flag to determine locked state
            var biometricSet = await IsBiometricLockSetAsync(userId);
            if (biometricSet && await _stateService.GetBiometricLockedAsync(userId))
            {
                return true;
            }

            if (!await _cryptoService.HasUserKeyAsync(userId))
            {
                try
                {
                    // Filter out accounts without auto key
                    if (!await _cryptoService.HasAutoUnlockKeyAsync(userId))
                    {
                        return true;
                    }
                    // Inactive accounts with an auto key aren't locked, but we shouldn't set user key
                    if (userId != null && await _stateService.GetActiveUserIdAsync() != userId)
                    {
                        return false;
                    }
                    await _cryptoService.SetUserKeyAsync(await _cryptoService.GetAutoUnlockKeyAsync(userId), userId);
                }
                catch (LegacyUserException)
                {
                    // Legacy users must migrate on web vault before login
                    await LogOutAsync(false, userId);
                }

            }

            // Check again to verify auto key was set
            var hasKey = await _cryptoService.HasUserKeyAsync(userId);
            return !hasKey;
        }

        public async Task<bool> ShouldLockAsync(string userId = null)
        {
            return await ShouldTimeoutAsync(userId)
                   &&
                   await _stateService.GetVaultTimeoutActionAsync(userId) == VaultTimeoutAction.Lock;
        }

        public async Task<bool> IsLoggedOutByTimeoutAsync(string userId = null)
        {
            var authed = await _stateService.IsAuthenticatedAsync(userId);
            var email = await _stateService.GetEmailAsync(userId);
            return !authed && !string.IsNullOrWhiteSpace(email);
        }

        public async Task<bool> ShouldLogOutByTimeoutAsync(string userId = null)
        {
            return await ShouldTimeoutAsync(userId)
                   &&
                   await _stateService.GetVaultTimeoutActionAsync(userId) == VaultTimeoutAction.Logout;
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
            if (vaultTimeoutAction == VaultTimeoutAction.Lock && await IsLockedAsync(userId))
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
            if (action == VaultTimeoutAction.Logout)
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

            var isActiveAccount = await _stateService.IsActiveAccountAsync(userId);

            if (userId == null)
            {
                userId = await _stateService.GetActiveUserIdAsync();
            }

            if (!await _userVerificationService.HasMasterPasswordAsync())
            {
                var pinStatus = await GetPinLockTypeAsync(userId);
                var ephemeralPinSet = await _stateService.GetPinKeyEncryptedUserKeyEphemeralAsync()
                    ?? await _stateService.GetPinProtectedKeyAsync();
                var pinEnabled = (pinStatus == PinLockType.Transient && ephemeralPinSet != null) ||
                          pinStatus == PinLockType.Persistent;

                if (!pinEnabled && !await IsBiometricLockSetAsync())
                {
                    await LogOutAsync(userInitiated, userId);
                    return;
                }
            }

            if (allowSoftLock)
            {
                var isBiometricLockSet = await IsBiometricLockSetAsync(userId);
                await _stateService.SetBiometricLockedAsync(isBiometricLockSet, userId);
                if (isBiometricLockSet)
                {
                    _lockedCallback?.Invoke(new Tuple<string, bool>(userId, userInitiated));
                    return;
                }
            }
            await Task.WhenAll(
                _cryptoService.ClearUserKeyAsync(userId),
                _cryptoService.ClearMasterKeyAsync(userId),
                _stateService.SetUserKeyAutoUnlockAsync(null, userId),
                _cryptoService.ClearOrgKeysAsync(true, userId),
                _cryptoService.ClearKeyPairAsync(true, userId));

            if (isActiveAccount)
            {
                _folderService.ClearCache();
                await _cipherService.ClearCacheAsync();
                _collectionService.ClearCache();
                _searchService.ClearIndex();
            }
            _lockedCallback?.Invoke(new Tuple<string, bool>(userId, userInitiated));
        }

        public async Task LogOutAsync(bool userInitiated = true, string userId = null)
        {
            if (_loggedOutCallback != null)
            {
                await _loggedOutCallback.Invoke(new Tuple<string, bool, bool>(userId, userInitiated, false));
            }
        }

        public async Task SetVaultTimeoutOptionsAsync(int? timeout, VaultTimeoutAction? action)
        {
            await _stateService.SetVaultTimeoutAsync(timeout);
            await _stateService.SetVaultTimeoutActionAsync(action);
            await _cryptoService.RefreshKeysAsync();
            await _tokenService.ToggleTokensAsync();
        }

        public async Task<PinLockType> GetPinLockTypeAsync(string userId = null)
        {
            // we can't depend on only the protected pin being set because old
            // versions only used it for MP on Restart
            var isPinEnabled = await _stateService.GetProtectedPinAsync(userId) != null;
            var hasUserKeyPin = await _stateService.GetPinKeyEncryptedUserKeyAsync(userId) != null;
            var hasOldUserKeyPin = await _stateService.GetPinProtectedAsync(userId) != null;

            if (hasUserKeyPin || hasOldUserKeyPin)
            {
                return PinLockType.Persistent;
            }
            else if (isPinEnabled && !hasUserKeyPin && !hasOldUserKeyPin)
            {
                return PinLockType.Transient;
            }
            return PinLockType.Disabled;
        }

        public async Task<bool> IsBiometricLockSetAsync(string userId = null)
        {
            var biometricLock = await _stateService.GetBiometricUnlockAsync(userId);
            return biometricLock.GetValueOrDefault();
        }

        public async Task ClearAsync(string userId = null)
        {
            await _cryptoService.ClearPinKeysAsync(userId);
        }

        public async Task<int?> GetVaultTimeout(string userId = null)
        {
            return await _stateService.GetVaultTimeoutAsync(userId);
        }

        public async Task<VaultTimeoutAction?> GetVaultTimeoutAction(string userId = null)
        {
            return await _stateService.GetVaultTimeoutActionAsync(userId);
        }
    }
}
