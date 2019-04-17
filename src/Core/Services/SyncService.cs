using Bit.Core.Abstractions;
using Bit.Core.Models.Data;
using Bit.Core.Models.Response;
using Bit.Core.Utilities;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class SyncService
    {
        private const string Keys_LastSyncFormat = "lastSync_{0}";

        private readonly IUserService _userService;
        private readonly IApiService _apiService;
        private readonly ISettingsService _settingsService;
        private readonly IFolderService _folderService;
        private readonly ICipherService _cipherService;
        private readonly ICryptoService _cryptoService;
        private readonly ICollectionService _collectionService;
        private readonly IStorageService _storageService;
        private readonly IMessagingService _messagingService;

        public SyncService(
            IUserService userService,
            IApiService apiService,
            ISettingsService settingsService,
            IFolderService folderService,
            ICipherService cipherService,
            ICryptoService cryptoService,
            ICollectionService collectionService,
            IStorageService storageService,
            IMessagingService messagingService)
        {
            _userService = userService;
            _apiService = apiService;
            _settingsService = settingsService;
            _folderService = folderService;
            _cipherService = cipherService;
            _cryptoService = cryptoService;
            _collectionService = collectionService;
            _storageService = storageService;
            _messagingService = messagingService;
        }

        public bool SyncInProgress { get; set; }

        public async Task<DateTime?> GetLastSyncAsync()
        {
            var userId = await _userService.GetUserIdAsync();
            if(userId == null)
            {
                return null;
            }
            return await _storageService.GetAsync<DateTime?>(string.Format(Keys_LastSyncFormat, userId));
        }

        public async Task SetLastSync(DateTime date)
        {
            var userId = await _userService.GetUserIdAsync();
            if(userId == null)
            {
                return;
            }
            await _storageService.SaveAsync(string.Format(Keys_LastSyncFormat, userId), date);
        }

        // Helpers

        private void SyncStarted()
        {
            SyncInProgress = true;
            _messagingService.Send("syncStarted");
        }

        private bool SyncCompelted(bool successfully)
        {
            SyncInProgress = false;
            _messagingService.Send("syncCompleted", new Dictionary<string, object> { ["successfully"] = successfully });
            return successfully;
        }

        private async Task<Tuple<bool, bool>> NeedsSyncingAsync(bool forceSync)
        {
            if(forceSync)
            {
                return new Tuple<bool, bool>(true, false);
            }
            var lastSync = await GetLastSyncAsync();
            if(lastSync == null || lastSync == DateTime.MinValue)
            {
                return new Tuple<bool, bool>(true, false);
            }
            try
            {
                var response = await _apiService.GetAccountRevisionDateAsync();
                var d = CoreHelpers.Epoc.AddMilliseconds(response);
                if(d <= lastSync.Value)
                {
                    return new Tuple<bool, bool>(false, false);
                }
                return new Tuple<bool, bool>(true, false);
            }
            catch
            {
                return new Tuple<bool, bool>(false, true);
            }
        }

        private async Task SyncProfileAsync(ProfileResponse response)
        {
            var stamp = await _userService.GetSecurityStampAsync();
            if(stamp != null && stamp != response.SecurityStamp)
            {
                // TODO logout callback
                throw new Exception("Stamp has changed.");
            }
            await _cryptoService.SetEncKeyAsync(response.Key);
            await _cryptoService.SetEncPrivateKeyAsync(response.PrivateKey);
            await _cryptoService.SetOrgKeysAsync(response.Organizations);
            await _userService.SetSecurityStampAsync(response.SecurityStamp);
            var organizations = response.Organizations.ToDictionary(o => o.Id, o => new OrganizationData(o));
            await _userService.ReplaceOrganizationsAsync(organizations);
        }

        private async Task SyncFoldersAsync(string userId, List<FolderResponse> response)
        {
            var folders = response.ToDictionary(f => f.Id, f => new FolderData(f, userId));
            await _folderService.ReplaceAsync(folders);
        }

        private async Task SyncCollectionsAsync(List<CollectionDetailsResponse> response)
        {
            var collections = response.ToDictionary(c => c.Id, c => new CollectionData(c));
            await _collectionService.ReplaceAsync(collections);
        }

        private async Task SyncCiphersAsync(string userId, List<CipherResponse> response)
        {
            var ciphers = response.ToDictionary(c => c.Id, c => new CipherData(c, userId));
            await _cipherService.ReplaceAsync(ciphers);
        }

        private async Task SyncSettingsAsync(string userId, DomainsResponse response)
        {
            var eqDomains = new List<List<string>>();
            if(response != null && response.EquivalentDomains != null)
            {
                eqDomains = eqDomains.Concat(response.EquivalentDomains).ToList();
            }
            if(response != null && response.GlobalEquivalentDomains != null)
            {
                foreach(var global in response.GlobalEquivalentDomains)
                {
                    if(global.Domains.Any())
                    {
                        eqDomains.Add(global.Domains);
                    }
                }
            }
            await _settingsService.SetEquivalentDomainsAsync(eqDomains);
        }
    }
}
