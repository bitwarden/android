using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Data;
using Plugin.Settings.Abstractions;

namespace Bit.App.Services
{
    public class SyncService : ISyncService
    {
        private const string LastSyncKey = "lastSync";

        private readonly IFolderApiRepository _folderApiRepository;
        private readonly ISiteApiRepository _siteApiRepository;
        private readonly IFolderRepository _folderRepository;
        private readonly ISiteRepository _siteRepository;
        private readonly IAuthService _authService;
        private readonly ISettings _settings;

        public SyncService(
            IFolderApiRepository folderApiRepository,
            ISiteApiRepository siteApiRepository,
            IFolderRepository folderRepository,
            ISiteRepository siteRepository,
            IAuthService authService,
            ISettings settings)
        {
            _folderApiRepository = folderApiRepository;
            _siteApiRepository = siteApiRepository;
            _folderRepository = folderRepository;
            _siteRepository = siteRepository;
            _authService = authService;
            _settings = settings;
        }

        public async Task<bool> SyncAsync()
        {
            var now = DateTime.UtcNow;
            var lastSync = _settings.GetValueOrDefault(LastSyncKey, now.AddYears(-100));

            var siteTask = SyncSitesAsync(lastSync);
            var folderTask = SyncFoldersAsync(lastSync);
            await Task.WhenAll(siteTask, folderTask);

            if(await siteTask && await folderTask && folderTask.Exception == null && siteTask.Exception == null)
            {
                _settings.AddOrUpdateValue(LastSyncKey, now);
                return true;
            }

            return false;
        }

        private async Task<bool> SyncFoldersAsync(DateTime lastSync)
        {
            var folderResponse = await _folderApiRepository.GetAsync();
            if(!folderResponse.Succeeded)
            {
                return false;
            }

            var serverFolders = folderResponse.Result.Data;
            var localFolders = await _folderRepository.GetAllByUserIdAsync(_authService.UserId);

            foreach(var serverFolder in serverFolders.Where(f => f.RevisionDate >= lastSync))
            {
                var data = new FolderData(serverFolder, _authService.UserId);
                var existingLocalFolder = localFolders.SingleOrDefault(f => f.Id == serverFolder.Id);
                if(existingLocalFolder == null)
                {
                    await _folderRepository.InsertAsync(data);
                }
                else
                {
                    await _folderRepository.UpdateAsync(data);
                }
            }

            foreach(var folder in localFolders.Where(localFolder => !serverFolders.Any(serverFolder => serverFolder.Id == localFolder.Id)))
            {
                await _folderRepository.DeleteAsync(folder.Id);
            }

            return true;
        }

        private async Task<bool> SyncSitesAsync(DateTime lastSync)
        {
            var siteResponse = await _siteApiRepository.GetAsync();
            if(!siteResponse.Succeeded)
            {
                return false;
            }

            var serverSites = siteResponse.Result.Data;
            var localSites = await _siteRepository.GetAllByUserIdAsync(_authService.UserId);

            foreach(var serverSite in serverSites.Where(s => s.RevisionDate >= lastSync))
            {
                var data = new SiteData(serverSite, _authService.UserId);
                var existingLocalSite = localSites.SingleOrDefault(s => s.Id == serverSite.Id);
                if(existingLocalSite == null)
                {
                    await _siteRepository.InsertAsync(data);
                }
                else
                {
                    await _siteRepository.UpdateAsync(data);
                }
            }

            foreach(var site in localSites.Where(localSite => !serverSites.Any(serverSite => serverSite.Id == localSite.Id)))
            {
                await _siteRepository.DeleteAsync(site.Id);
            }

            return true;
        }
    }
}
