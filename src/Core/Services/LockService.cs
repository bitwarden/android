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
            var logService = ServiceContainer.Resolve<ILogService>("logService");
            logService.Info("IsLockedAsync 1");
            var hasKey = await _cryptoService.HasKeyAsync();
            if(hasKey)
            {
                logService.Info("IsLockedAsync 2");
                if(PinLocked)
                {
                    logService.Info("IsLockedAsync 3");
                    return true;
                }
                else
                {
                    logService.Info("IsLockedAsync 4");
                    var fingerprintSet = await IsFingerprintLockSetAsync();
                    if(fingerprintSet && FingerprintLocked)
                    {
                        logService.Info("IsLockedAsync 5");
                        return true;
                    }
                }
            }
            logService.Info("IsLockedAsync 6");
            return !hasKey;
        }

        public async Task CheckLockAsync()
        {
            if(_platformUtilsService.IsViewOpen())
            {
                return;
            }
            var authed = await _userService.IsAuthenticatedAsync();
            if(!authed)
            {
                return;
            }
            if(await IsLockedAsync())
            {
                return;
            }
            var lockOption = _platformUtilsService.LockTimeout();
            if(lockOption == null)
            {
                lockOption = await _storageService.GetAsync<int?>(Constants.LockOptionKey);
            }
            if(lockOption.GetValueOrDefault(-1) < 0)
            {
                return;
            }
            var lastActive = await _storageService.GetAsync<DateTime?>(Constants.LastActiveKey);
            if(lastActive == null)
            {
                return;
            }
            var diff = DateTime.UtcNow - lastActive.Value;
            if(diff.TotalSeconds >= lockOption.Value)
            {
                // need to lock now
                await LockAsync(true);
            }
        }

        public async Task LockAsync(bool allowSoftLock = false, bool userInitiated = false)
        {
            var logService = ServiceContainer.Resolve<ILogService>("logService");
            logService.Info("LockAsync 1");
            var authed = await _userService.IsAuthenticatedAsync();
            if(!authed)
            {
                return;
            }
            if(allowSoftLock)
            {
                logService.Info("LockAsync 2");
                var pinSet = await IsPinLockSetAsync();
                if(pinSet.Item1)
                {
                    logService.Info("LockAsync PinLocked = true");
                    PinLocked = true;
                }
                FingerprintLocked = await IsFingerprintLockSetAsync();
                if(FingerprintLocked || PinLocked)
                {
                    logService.Info("LockAsync 4");
                    _messagingService.Send("locked", userInitiated);
                    // TODO: locked callback?
                    return;
                }
            }
            logService.Info("LockAsync 5");
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
