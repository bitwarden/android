using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using System;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class LockService : ILockService
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
        private readonly Action<bool> _lockedCallback;

        public LockService(
            ICryptoService cryptoService,
            IUserService userService,
            IPlatformUtilsService platformUtilsService,
            IStorageService storageService,
            IFolderService folderService,
            ICipherService cipherService,
            ICollectionService collectionService,
            ISearchService searchService,
            IMessagingService messagingService,
            Action<bool> lockedCallback)
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
            _lockedCallback = lockedCallback;
        }

        public CipherString PinProtectedKey { get; set; } = null;
        public bool FingerprintLocked { get; set; } = true;

        public async Task<bool> IsLockedAsync()
        {
            var hasKey = await _cryptoService.HasKeyAsync();
            if (hasKey)
            {
                var fingerprintSet = await IsFingerprintLockSetAsync();
                if (fingerprintSet && FingerprintLocked)
                {
                    return true;
                }
            }
            return !hasKey;
        }

        public async Task CheckLockAsync()
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
            var lockOption = _platformUtilsService.LockTimeout();
            if (lockOption == null)
            {
                lockOption = await _storageService.GetAsync<int?>(Constants.LockOptionKey);
            }
            if (lockOption.GetValueOrDefault(-1) < 0)
            {
                return;
            }
            var lastActive = await _storageService.GetAsync<DateTime?>(Constants.LastActiveKey);
            if (lastActive == null)
            {
                return;
            }
            var diff = DateTime.UtcNow - lastActive.Value;
            if (diff.TotalSeconds >= lockOption.Value)
            {
                // need to lock now
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
            if (allowSoftLock)
            {
                FingerprintLocked = await IsFingerprintLockSetAsync();
                if (FingerprintLocked)
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
            _cipherService.ClearCache();
            _collectionService.ClearCache();
            _searchService.ClearIndex();
            _messagingService.Send("locked", userInitiated);
            _lockedCallback?.Invoke(userInitiated);
        }

        public async Task SetLockOptionAsync(int? lockOption)
        {
            await _storageService.SaveAsync(Constants.LockOptionKey, lockOption);
            await _cryptoService.ToggleKeyAsync();
        }

        public async Task<Tuple<bool, bool>> IsPinLockSetAsync()
        {
            var protectedPin = await _storageService.GetAsync<string>(Constants.ProtectedPin);
            var pinProtectedKey = await _storageService.GetAsync<string>(Constants.PinProtectedKey);
            return new Tuple<bool, bool>(protectedPin != null, pinProtectedKey != null);
        }

        public async Task<bool> IsFingerprintLockSetAsync()
        {
            var fingerprintLock = await _storageService.GetAsync<bool?>(Constants.FingerprintUnlockKey);
            return fingerprintLock.GetValueOrDefault();
        }

        public async Task ClearAsync()
        {
            PinProtectedKey = null;
            await _storageService.RemoveAsync(Constants.ProtectedPin);
        }
    }
}
