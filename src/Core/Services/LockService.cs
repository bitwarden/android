using Bit.Core.Abstractions;
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

        // TODO: init timer?

        public async Task<bool> IsLockedAsync()
        {
            var hasKey = await _cryptoService.HasKeyAsync();
            if(hasKey && PinLocked)
            {
                return true;
            }
            return !hasKey;
        }

        public async Task CheckLockAsync()
        {
            if(false) // TODO: view is open?
            {
                return;
            }
            var authed = await _userService.IsAuthenticatedAsync();
            if(!authed)
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
            var lockOptionsSeconds = lockOption.Value * 60;
            var diff = DateTime.UtcNow - lastActive.Value;
            if(diff.TotalSeconds >= lockOptionsSeconds)
            {
                // need to lock now
                await LockAsync(true);
            }
        }

        public async Task LockAsync(bool allowSoftLock = false)
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
                    _messagingService.Send("locked");
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
            _messagingService.Send("locked");
            // TODO: locked callback?
        }

        public async Task SetLockOptionAsync(int lockOption)
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

        public async Task ClearAsync()
        {
            await _storageService.RemoveAsync(Constants.ProtectedPin);
        }
    }
}
