using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Data;
using Plugin.Settings.Abstractions;
using Bit.App.Models.Api;
using System.Collections.Generic;
using Xamarin.Forms;

namespace Bit.App.Services
{
    public class SyncService : ISyncService
    {
        private readonly ICipherApiRepository _cipherApiRepository;
        private readonly IFolderApiRepository _folderApiRepository;
        private readonly ISiteApiRepository _siteApiRepository;
        private readonly IFolderRepository _folderRepository;
        private readonly ISiteRepository _siteRepository;
        private readonly IAuthService _authService;
        private readonly ISettings _settings;

        public SyncService(
            ICipherApiRepository cipherApiRepository,
            IFolderApiRepository folderApiRepository,
            ISiteApiRepository siteApiRepository,
            IFolderRepository folderRepository,
            ISiteRepository siteRepository,
            IAuthService authService,
            ISettings settings)
        {
            _cipherApiRepository = cipherApiRepository;
            _folderApiRepository = folderApiRepository;
            _siteApiRepository = siteApiRepository;
            _folderRepository = folderRepository;
            _siteRepository = siteRepository;
            _authService = authService;
            _settings = settings;
        }

        public bool SyncInProgress { get; private set; }

        public async Task<bool> SyncAsync(string id)
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            SyncStarted();

            var cipher = await _cipherApiRepository.GetByIdAsync(id).ConfigureAwait(false);
            if(!cipher.Succeeded)
            {
                SyncCompleted(false);

                if(cipher.StatusCode == System.Net.HttpStatusCode.Forbidden
                    || cipher.StatusCode == System.Net.HttpStatusCode.Unauthorized)
                {
                    MessagingCenter.Send(Application.Current, "Logout", (string)null);
                }

                return false;
            }

            switch(cipher.Result.Type)
            {
                case Enums.CipherType.Folder:
                    var folderData = new FolderData(cipher.Result, _authService.UserId);
                    var existingLocalFolder = _folderRepository.GetByIdAsync(id);
                    if(existingLocalFolder == null)
                    {
                        await _folderRepository.InsertAsync(folderData).ConfigureAwait(false);
                    }
                    else
                    {
                        await _folderRepository.UpdateAsync(folderData).ConfigureAwait(false);
                    }
                    break;
                case Enums.CipherType.Site:
                    var siteData = new SiteData(cipher.Result, _authService.UserId);
                    var existingLocalSite = _siteRepository.GetByIdAsync(id);
                    if(existingLocalSite == null)
                    {
                        await _siteRepository.InsertAsync(siteData).ConfigureAwait(false);
                    }
                    else
                    {
                        await _siteRepository.UpdateAsync(siteData).ConfigureAwait(false);
                    }
                    break;
                default:
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

            await _folderRepository.DeleteWithSiteUpdateAsync(id, revisionDate).ConfigureAwait(false);
            SyncCompleted(true);
            return true;
        }

        public async Task<bool> SyncDeleteSiteAsync(string id)
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            SyncStarted();

            await _siteRepository.DeleteAsync(id).ConfigureAwait(false);
            SyncCompleted(true);
            return true;
        }

        public async Task<bool> FullSyncAsync()
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            SyncStarted();

            var now = DateTime.UtcNow;
            var ciphers = await _cipherApiRepository.GetAsync().ConfigureAwait(false);
            if(!ciphers.Succeeded)
            {
                SyncCompleted(false);

                if(ciphers.StatusCode == System.Net.HttpStatusCode.Forbidden
                    || ciphers.StatusCode == System.Net.HttpStatusCode.Unauthorized)
                {
                    MessagingCenter.Send(Application.Current, "Logout", (string)null);
                }

                return false;
            }

            var sites = ciphers.Result.Data.Where(c => c.Type == Enums.CipherType.Site).ToDictionary(s => s.Id);
            var folders = ciphers.Result.Data.Where(c => c.Type == Enums.CipherType.Folder).ToDictionary(f => f.Id);

            var siteTask = SyncSitesAsync(sites, true);
            var folderTask = SyncFoldersAsync(folders, true);
            await Task.WhenAll(siteTask, folderTask).ConfigureAwait(false);

            if(folderTask.Exception != null || siteTask.Exception != null)
            {
                SyncCompleted(false);
                return false;
            }

            _settings.AddOrUpdateValue(Constants.LastSync, now);
            SyncCompleted(true);
            return true;
        }

        public async Task<bool> IncrementalSyncAsync(TimeSpan syncThreshold)
        {
            DateTime? lastSync = _settings.GetValueOrDefault<DateTime?>(Constants.LastSync, null);
            if(lastSync != null && DateTime.UtcNow - lastSync.Value < syncThreshold)
            {
                return false;
            }

            return await IncrementalSyncAsync().ConfigureAwait(false);
        }

        public async Task<bool> IncrementalSyncAsync()
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            var now = DateTime.UtcNow;
            DateTime? lastSync = _settings.GetValueOrDefault<DateTime?>(Constants.LastSync, null);
            if(lastSync == null)
            {
                return await FullSyncAsync().ConfigureAwait(false);
            }

            SyncStarted();

            var ciphers = await _cipherApiRepository.GetByRevisionDateWithHistoryAsync(lastSync.Value).ConfigureAwait(false);
            if(!ciphers.Succeeded)
            {
                SyncCompleted(false);

                if(ciphers.StatusCode == System.Net.HttpStatusCode.Forbidden
                    || ciphers.StatusCode == System.Net.HttpStatusCode.Unauthorized)
                {
                    MessagingCenter.Send(Application.Current, "Logout", (string)null);
                }

                return false;
            }

            var sites = ciphers.Result.Revised.Where(c => c.Type == Enums.CipherType.Site).ToDictionary(s => s.Id);
            var folders = ciphers.Result.Revised.Where(c => c.Type == Enums.CipherType.Folder).ToDictionary(f => f.Id);

            var siteTask = SyncSitesAsync(sites, false);
            var folderTask = SyncFoldersAsync(folders, false);
            var deleteTask = DeleteCiphersAsync(ciphers.Result.Deleted);

            await Task.WhenAll(siteTask, folderTask, deleteTask).ConfigureAwait(false);
            if(folderTask.Exception != null || siteTask.Exception != null || deleteTask.Exception != null)
            {
                SyncCompleted(false);
                return false;
            }

            _settings.AddOrUpdateValue(Constants.LastSync, now);
            SyncCompleted(true);
            return true;
        }

        private async Task SyncFoldersAsync(IDictionary<string, CipherResponse> serverFolders, bool deleteMissing)
        {
            if(!_authService.IsAuthenticated)
            {
                return;
            }

            var localFolders = (await _folderRepository.GetAllByUserIdAsync(_authService.UserId).ConfigureAwait(false))
                .ToDictionary(f => f.Id);

            foreach(var serverFolder in serverFolders)
            {
                if(!_authService.IsAuthenticated)
                {
                    return;
                }

                var existingLocalFolder = localFolders.ContainsKey(serverFolder.Key) ? localFolders[serverFolder.Key] : null;
                if(existingLocalFolder == null)
                {
                    var data = new FolderData(serverFolder.Value, _authService.UserId);
                    await _folderRepository.InsertAsync(data).ConfigureAwait(false);
                }
                else if(existingLocalFolder.RevisionDateTime != serverFolder.Value.RevisionDate)
                {
                    var data = new FolderData(serverFolder.Value, _authService.UserId);
                    await _folderRepository.UpdateAsync(data).ConfigureAwait(false);
                }
            }

            if(!deleteMissing)
            {
                return;
            }

            foreach(var folder in localFolders.Where(localFolder => !serverFolders.ContainsKey(localFolder.Key)))
            {
                await _folderRepository.DeleteAsync(folder.Value.Id).ConfigureAwait(false);
            }
        }

        private async Task SyncSitesAsync(IDictionary<string, CipherResponse> serverSites, bool deleteMissing)
        {
            if(!_authService.IsAuthenticated)
            {
                return;
            }

            var localSites = (await _siteRepository.GetAllByUserIdAsync(_authService.UserId).ConfigureAwait(false))
                .ToDictionary(s => s.Id);

            foreach(var serverSite in serverSites)
            {
                if(!_authService.IsAuthenticated)
                {
                    return;
                }

                var existingLocalSite = localSites.ContainsKey(serverSite.Key) ? localSites[serverSite.Key] : null;
                if(existingLocalSite == null)
                {
                    var data = new SiteData(serverSite.Value, _authService.UserId);
                    await _siteRepository.InsertAsync(data).ConfigureAwait(false);
                }
                else if(existingLocalSite.RevisionDateTime != serverSite.Value.RevisionDate)
                {
                    var data = new SiteData(serverSite.Value, _authService.UserId);
                    await _siteRepository.UpdateAsync(data).ConfigureAwait(false);
                }
            }

            if(!deleteMissing)
            {
                return;
            }

            foreach(var site in localSites.Where(localSite => !serverSites.ContainsKey(localSite.Key)))
            {
                await _siteRepository.DeleteAsync(site.Value.Id).ConfigureAwait(false);
            }
        }

        private async Task DeleteCiphersAsync(IEnumerable<string> cipherIds)
        {
            var tasks = new List<Task>();
            foreach(var cipherId in cipherIds)
            {
                if(!_authService.IsAuthenticated)
                {
                    return;
                }

                tasks.Add(_siteRepository.DeleteAsync(cipherId));
                tasks.Add(_folderRepository.DeleteAsync(cipherId));
            }
            await Task.WhenAll(tasks).ConfigureAwait(false);
        }

        private void SyncStarted()
        {
            SyncInProgress = true;
            MessagingCenter.Send(Application.Current, "SyncStarted");
        }

        private void SyncCompleted(bool successfully)
        {
            SyncInProgress = false;
            MessagingCenter.Send(Application.Current, "SyncCompleted", successfully);
        }
    }
}
