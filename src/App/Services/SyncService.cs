using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Data;
using Plugin.Settings.Abstractions;
using Bit.App.Models.Api;
using System.Collections.Generic;
using Xamarin.Forms;
using Newtonsoft.Json;
using Bit.App.Models;

namespace Bit.App.Services
{
    public class SyncService : ISyncService
    {
        private readonly ICipherApiRepository _cipherApiRepository;
        private readonly IFolderApiRepository _folderApiRepository;
        private readonly IAccountsApiRepository _accountsApiRepository;
        private readonly ISettingsApiRepository _settingsApiRepository;
        private readonly ISyncApiRepository _syncApiRepository;
        private readonly IFolderRepository _folderRepository;
        private readonly ICollectionRepository _collectionRepository;
        private readonly ICipherCollectionRepository _cipherCollectionRepository;
        private readonly ICipherService _cipherService;
        private readonly IAttachmentRepository _attachmentRepository;
        private readonly ISettingsRepository _settingsRepository;
        private readonly IAuthService _authService;
        private readonly ICryptoService _cryptoService;
        private readonly ISettings _settings;
        private readonly IAppSettingsService _appSettingsService;

        public SyncService(
            ICipherApiRepository cipherApiRepository,
            IFolderApiRepository folderApiRepository,
            IAccountsApiRepository accountsApiRepository,
            ISettingsApiRepository settingsApiRepository,
            ISyncApiRepository syncApiRepository,
            IFolderRepository folderRepository,
            ICollectionRepository collectionRepository,
            ICipherCollectionRepository cipherCollectionRepository,
            ICipherService cipherService,
            IAttachmentRepository attachmentRepository,
            ISettingsRepository settingsRepository,
            IAuthService authService,
            ICryptoService cryptoService,
            ISettings settings,
            IAppSettingsService appSettingsService)
        {
            _cipherApiRepository = cipherApiRepository;
            _folderApiRepository = folderApiRepository;
            _accountsApiRepository = accountsApiRepository;
            _settingsApiRepository = settingsApiRepository;
            _syncApiRepository = syncApiRepository;
            _folderRepository = folderRepository;
            _collectionRepository = collectionRepository;
            _cipherCollectionRepository = cipherCollectionRepository;
            _cipherService = cipherService;
            _attachmentRepository = attachmentRepository;
            _settingsRepository = settingsRepository;
            _authService = authService;
            _cryptoService = cryptoService;
            _settings = settings;
            _appSettingsService = appSettingsService;
        }

        public bool SyncInProgress { get; private set; }

        public async Task<bool> SyncCipherAsync(string id)
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            SyncStarted();

            var cipher = await _cipherApiRepository.GetByIdAsync(id).ConfigureAwait(false);
            if(!CheckSuccess(cipher))
            {
                return false;
            }

            try
            {
                var cipherData = new CipherData(cipher.Result, _authService.UserId);
                await _cipherService.UpsertDataAsync(cipherData).ConfigureAwait(false);

                var localAttachments = (await _attachmentRepository.GetAllByCipherIdAsync(cipherData.Id)
                    .ConfigureAwait(false));

                if(cipher.Result.Attachments != null)
                {
                    var attachmentData = cipher.Result.Attachments.Select(a => new AttachmentData(a, cipherData.Id));
                    await _cipherService.UpsertAttachmentDataAsync(attachmentData).ConfigureAwait(false);
                }

                if(localAttachments != null)
                {
                    foreach(var attachment in localAttachments
                        .Where(a => !cipher.Result.Attachments.Any(sa => sa.Id == a.Id)))
                    {
                        try
                        {
                            await _cipherService.DeleteAttachmentDataAsync(attachment.Id).ConfigureAwait(false);
                        }
                        catch(SQLite.SQLiteException) { }
                    }
                }
            }
            catch(SQLite.SQLiteException)
            {
                SyncCompleted(false);
                return false;
            }

            SyncCompleted(true);
            return true;
        }

        public async Task<bool> SyncFolderAsync(string id)
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            SyncStarted();

            var folder = await _folderApiRepository.GetByIdAsync(id).ConfigureAwait(false);
            if(!CheckSuccess(folder))
            {
                return false;
            }

            try
            {
                var folderData = new FolderData(folder.Result, _authService.UserId);
                await _folderRepository.UpsertAsync(folderData).ConfigureAwait(false);
            }
            catch(SQLite.SQLiteException)
            {
                SyncCompleted(false);
                return false;
            }

            SyncCompleted(true);
            return true;
        }

        public async Task<bool> SyncDeleteFolderAsync(string id, DateTime revisionDate)
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            SyncStarted();

            try
            {
                await _folderRepository.DeleteWithCipherUpdateAsync(id, revisionDate).ConfigureAwait(false);
                SyncCompleted(true);
                return true;
            }
            catch(SQLite.SQLiteException)
            {
                SyncCompleted(false);
                return false;
            }
        }

        public async Task<bool> SyncDeleteCipherAsync(string id)
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            SyncStarted();

            try
            {
                await _cipherService.DeleteDataAsync(id).ConfigureAwait(false);
                SyncCompleted(true);
                return true;
            }
            catch(SQLite.SQLiteException)
            {
                SyncCompleted(false);
                return false;
            }
        }

        public async Task<bool> SyncSettingsAsync()
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            SyncStarted();

            var domains = await _settingsApiRepository.GetDomains(false).ConfigureAwait(false);
            if(!CheckSuccess(domains))
            {
                return false;
            }

            await SyncDomainsAsync(domains.Result);

            SyncCompleted(true);
            return true;
        }

        public async Task<bool> SyncProfileAsync()
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            SyncStarted();

            var profile = await _accountsApiRepository.GetProfileAsync().ConfigureAwait(false);
            if(!CheckSuccess(profile, !string.IsNullOrWhiteSpace(_appSettingsService.SecurityStamp) &&
                _appSettingsService.SecurityStamp != profile.Result.SecurityStamp))
            {
                return false;
            }

            await SyncProfileKeysAsync(profile.Result);

            SyncCompleted(true);
            return true;
        }

        public async Task<bool> FullSyncAsync(TimeSpan syncThreshold, bool forceSync = false)
        {
            var lastSync = _settings.GetValueOrDefault(Constants.LastSync, DateTime.MinValue);
            if(DateTime.UtcNow - lastSync < syncThreshold)
            {
                return false;
            }

            return await FullSyncAsync(forceSync).ConfigureAwait(false);
        }

        public async Task<bool> FullSyncAsync(bool forceSync = false)
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            if(!forceSync && !(await NeedsToSyncAsync()))
            {
                _settings.AddOrUpdateValue(Constants.LastSync, DateTime.UtcNow);
                return false;
            }

            SyncStarted();

            var now = DateTime.UtcNow;

            var syncResponse = await _syncApiRepository.Get();
            if(!CheckSuccess(syncResponse,
                !string.IsNullOrWhiteSpace(_appSettingsService.SecurityStamp) &&
                syncResponse.Result?.Profile != null &&
                _appSettingsService.SecurityStamp != syncResponse.Result.Profile.SecurityStamp))
            {
                return false;
            }

            var ciphersDict = syncResponse.Result.Ciphers.ToDictionary(s => s.Id);
            var foldersDict = syncResponse.Result.Folders.ToDictionary(f => f.Id);
            var collectionsDict = syncResponse.Result.Collections?.ToDictionary(c => c.Id);

            var cipherTask = SyncCiphersAsync(ciphersDict);
            var folderTask = SyncFoldersAsync(foldersDict);
            var collectionsTask = SyncCollectionsAsync(collectionsDict);
            var domainsTask = SyncDomainsAsync(syncResponse.Result.Domains);
            var profileTask = SyncProfileKeysAsync(syncResponse.Result.Profile);
            await Task.WhenAll(cipherTask, folderTask, collectionsTask, domainsTask, profileTask).ConfigureAwait(false);

            if(folderTask.Exception != null || cipherTask.Exception != null || collectionsTask.Exception != null ||
                domainsTask.Exception != null || profileTask.Exception != null)
            {
                SyncCompleted(false);
                return false;
            }

            _settings.AddOrUpdateValue(Constants.LastSync, now);
            SyncCompleted(true);
            return true;
        }

        private async Task<bool> NeedsToSyncAsync()
        {
            if(!_settings.Contains(Constants.LastSync))
            {
                return true;
            }
            var lastSync = _settings.GetValueOrDefault(Constants.LastSync, DateTime.MinValue);

            var accountRevisionDate = await _accountsApiRepository.GetAccountRevisionDateAsync();
            if(accountRevisionDate.Succeeded && accountRevisionDate.Result.HasValue &&
                accountRevisionDate.Result.Value > lastSync)
            {
                return true;
            }

            if(Application.Current != null && (accountRevisionDate.StatusCode == System.Net.HttpStatusCode.Forbidden
                || accountRevisionDate.StatusCode == System.Net.HttpStatusCode.Unauthorized))
            {
                _authService.LogOut();
            }

            return false;
        }

        private async Task SyncFoldersAsync(IDictionary<string, FolderResponse> serverFolders)
        {
            if(!_authService.IsAuthenticated)
            {
                return;
            }

            var localFolders = (await _folderRepository.GetAllByUserIdAsync(_authService.UserId)
                .ConfigureAwait(false))
                .GroupBy(f => f.Id)
                .Select(f => f.First())
                .ToDictionary(f => f.Id);

            foreach(var serverFolder in serverFolders)
            {
                if(!_authService.IsAuthenticated)
                {
                    return;
                }

                try
                {
                    var data = new FolderData(serverFolder.Value, _authService.UserId);
                    await _folderRepository.UpsertAsync(data).ConfigureAwait(false);
                }
                catch(SQLite.SQLiteException) { }
            }

            foreach(var folder in localFolders.Where(localFolder => !serverFolders.ContainsKey(localFolder.Key)))
            {
                try
                {
                    await _folderRepository.DeleteAsync(folder.Value.Id).ConfigureAwait(false);
                }
                catch(SQLite.SQLiteException) { }
            }
        }

        private async Task SyncCollectionsAsync(IDictionary<string, CollectionResponse> serverCollections)
        {
            if(!_authService.IsAuthenticated)
            {
                return;
            }

            var localCollections = (await _collectionRepository.GetAllByUserIdAsync(_authService.UserId)
                .ConfigureAwait(false))
                .GroupBy(f => f.Id)
                .Select(f => f.First())
                .ToDictionary(f => f.Id);

            if(serverCollections != null)
            {
                foreach(var serverCollection in serverCollections)
                {
                    if(!_authService.IsAuthenticated)
                    {
                        return;
                    }

                    try
                    {
                        var data = new CollectionData(serverCollection.Value, _authService.UserId);
                        await _collectionRepository.UpsertAsync(data).ConfigureAwait(false);
                    }
                    catch(SQLite.SQLiteException) { }
                }
            }

            foreach(var collection in localCollections.Where(lc => !serverCollections.ContainsKey(lc.Key)))
            {
                try
                {
                    await _collectionRepository.DeleteAsync(collection.Value.Id).ConfigureAwait(false);
                }
                catch(SQLite.SQLiteException) { }
            }
        }

        private async Task SyncCiphersAsync(IDictionary<string, CipherResponse> serverCiphers)
        {
            if(!_authService.IsAuthenticated)
            {
                return;
            }

            var localCiphers = (await _cipherService.GetAllAsync()
                .ConfigureAwait(false))
                .GroupBy(s => s.Id)
                .Select(s => s.First())
                .ToDictionary(s => s.Id);

            var localAttachments = (await _attachmentRepository.GetAllByUserIdAsync(_authService.UserId)
                .ConfigureAwait(false))
                .GroupBy(a => a.LoginId)
                .ToDictionary(g => g.Key);

            var cipherCollections = new List<CipherCollectionData>();
            foreach(var serverCipher in serverCiphers)
            {
                if(!_authService.IsAuthenticated)
                {
                    return;
                }

                var collectionForThisCipher = serverCipher.Value.CollectionIds?.Select(cid => new CipherCollectionData
                {
                    CipherId = serverCipher.Value.Id,
                    CollectionId = cid,
                    UserId = _authService.UserId
                }).ToList();

                if(collectionForThisCipher != null && collectionForThisCipher.Any())
                {
                    cipherCollections.AddRange(collectionForThisCipher);
                }

                try
                {
                    var localCipher = localCiphers.ContainsKey(serverCipher.Value.Id) ?
                        localCiphers[serverCipher.Value.Id] : null;

                    var data = new CipherData(serverCipher.Value, _authService.UserId);
                    await _cipherService.UpsertDataAsync(data).ConfigureAwait(false);

                    if(serverCipher.Value.Attachments != null)
                    {
                        var attachmentData = serverCipher.Value.Attachments.Select(a => new AttachmentData(a, data.Id));
                        await _cipherService.UpsertAttachmentDataAsync(attachmentData).ConfigureAwait(false);
                    }

                    if(localCipher != null && localAttachments != null && localAttachments.ContainsKey(localCipher.Id))
                    {
                        foreach(var attachment in localAttachments[localCipher.Id]
                            .Where(a => !serverCipher.Value.Attachments.Any(sa => sa.Id == a.Id)))
                        {
                            try
                            {
                                await _cipherService.DeleteAttachmentDataAsync(attachment.Id).ConfigureAwait(false);
                            }
                            catch(SQLite.SQLiteException) { }
                        }
                    }
                }
                catch(SQLite.SQLiteException) { }
            }

            foreach(var cipher in localCiphers.Where(local => !serverCiphers.ContainsKey(local.Key)))
            {
                try
                {
                    await _cipherService.DeleteDataAsync(cipher.Value.Id).ConfigureAwait(false);
                }
                catch(SQLite.SQLiteException) { }
            }

            try
            {
                await _cipherCollectionRepository.DeleteByUserIdAsync(_authService.UserId).ConfigureAwait(false);
            }
            catch(SQLite.SQLiteException) { }

            foreach(var cipherCollection in cipherCollections)
            {
                try
                {
                    await _cipherCollectionRepository.InsertAsync(cipherCollection).ConfigureAwait(false);
                }
                catch(SQLite.SQLiteException) { }
            }
        }

        private async Task SyncDomainsAsync(DomainsResponse serverDomains)
        {
            if(serverDomains == null)
            {
                return;
            }

            var eqDomains = new List<IEnumerable<string>>();
            if(serverDomains.EquivalentDomains != null)
            {
                eqDomains.AddRange(serverDomains.EquivalentDomains);
            }

            if(serverDomains.GlobalEquivalentDomains != null)
            {
                eqDomains.AddRange(serverDomains.GlobalEquivalentDomains.Select(d => d.Domains));
            }

            try
            {
                await _settingsRepository.UpsertAsync(new SettingsData
                {
                    Id = _authService.UserId,
                    EquivalentDomains = JsonConvert.SerializeObject(eqDomains)
                });
            }
            catch(SQLite.SQLiteException) { }
        }

        private Task SyncProfileKeysAsync(ProfileResponse profile)
        {
            if(profile == null)
            {
                return Task.FromResult(0);
            }

            if(!string.IsNullOrWhiteSpace(profile.Key))
            {
                _cryptoService.SetEncKey(new CipherString(profile.Key));
            }

            if(!string.IsNullOrWhiteSpace(profile.PrivateKey))
            {
                _cryptoService.SetPrivateKey(new CipherString(profile.PrivateKey));
            }

            if(!string.IsNullOrWhiteSpace(profile.SecurityStamp))
            {
                _appSettingsService.SecurityStamp = profile.SecurityStamp;
            }

            _cryptoService.SetOrgKeys(profile);
            _appSettingsService.OrganizationGivesPremium = profile.Organizations?.Any(o => o.UsersGetPremium) ?? false;
            return Task.FromResult(0);
        }

        private void SyncStarted()
        {
            if(Application.Current == null)
            {
                return;
            }

            SyncInProgress = true;
            MessagingCenter.Send(Application.Current, "SyncStarted");
        }

        private void SyncCompleted(bool successfully)
        {
            if(Application.Current == null)
            {
                return;
            }

            SyncInProgress = false;
            MessagingCenter.Send(Application.Current, "SyncCompleted", successfully);
        }

        private bool CheckSuccess<T>(ApiResult<T> result, bool logout = false)
        {
            if(!result.Succeeded || logout)
            {
                SyncCompleted(false);

                if(Application.Current != null && (logout ||
                    result.StatusCode == System.Net.HttpStatusCode.Forbidden ||
                    result.StatusCode == System.Net.HttpStatusCode.Unauthorized))
                {
                    _authService.LogOut();
                }

                return false;
            }

            return true;
        }
    }
}
