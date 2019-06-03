using Bit.Core.Abstractions;
using Bit.Core.Utilities;
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

        public LockService(
            ICryptoService cryptoService,
            IUserService userService,
            IPlatformUtilsService platformUtilsService,
            IStorageService storageService,
            IFolderService folderService,
            ICipherService cipherService,
            ICollectionService collectionService,
            ISearchService searchService,
            IMessagingService messagingService)
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
        }

        public bool PinLocked { get; set; }
        public bool FingerprintLocked { get; set; } = true;

        public async Task<bool> IsLockedAsync()
        {
            var hasKey = await _cryptoService.HasKeyAsync();
            if(hasKey)
            {
                if(PinLocked)
                {
                    return true;
                }
                else
                {
                    var fingerprintSet = await IsFingerprintLockSetAsync();
                    if(fingerprintSet && FingerprintLocked)
                    {
                        return true;
                    }
                }
            }
            return !hasKey;
        }

        public async Task CheckLockAsync()
        {
            var logService = ServiceContainer.Resolve<ILogService>("logService");
            logService.Info("CheckLockAsync 1");
            if(_platformUtilsService.IsViewOpen())
            {
                logService.Info("CheckLockAsync 2");
                return;
            }
            var authed = await _userService.IsAuthenticatedAsync();
            if(!authed)
            {
                logService.Info("CheckLockAsync 3");
                return;
            }
            if(await IsLockedAsync())
            {
                logService.Info("CheckLockAsync 4");
                return;
            }
            logService.Info("CheckLockAsync 5");
            var lockOption = _platformUtilsService.LockTimeout();
            if(lockOption == null)
            {
                logService.Info("CheckLockAsync 6");
                lockOption = await _storageService.GetAsync<int?>(Constants.LockOptionKey);
            }
            logService.Info("CheckLockAsync 7");
            if(lockOption.GetValueOrDefault(-1) < 0)
            {
                logService.Info("CheckLockAsync 8");
                return;
            }
            logService.Info("CheckLockAsync 9");
            var lastActive = await _storageService.GetAsync<DateTime?>(Constants.LastActiveKey);
            if(lastActive == null)
            {
                logService.Info("CheckLockAsync 10");
                return;
            }
            logService.Info("CheckLockAsync 11");
            var diff = DateTime.UtcNow - lastActive.Value;
            if(diff.TotalSeconds >= lockOption.Value)
            {
                logService.Info("CheckLockAsync 12");
                // need to lock now
                await LockAsync(true);
            }
            logService.Info("CheckLockAsync 13");
        }

        public async Task LockAsync(bool allowSoftLock = false, bool userInitiated = false)
        {
            var authed = await _userService.IsAuthenticatedAsync();
            if(!authed)
            {
                return;
            }
            if(allowSoftLock)
            {
                var pinSet = await IsPinLockSetAsync();
                if(pinSet.Item1)
                {
                    PinLocked = true;
                }
                if(await IsFingerprintLockSetAsync())
                {
                    FingerprintLocked = true;
                }
                if(FingerprintLocked || PinLocked)
                {
                    _messagingService.Send("locked", userInitiated);
                    // TODO: locked callback?
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
            // TODO: locked callback?
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
            await _storageService.RemoveAsync(Constants.ProtectedPin);
        }
    }
}
