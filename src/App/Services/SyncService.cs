using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Data;

namespace Bit.App.Services
{
    public class SyncService : ISyncService
    {
        private readonly IFolderApiRepository _folderApiRepository;
        private readonly ISiteApiRepository _siteApiRepository;
        private readonly IFolderRepository _folderRepository;
        private readonly ISiteRepository _siteRepository;
        private readonly IAuthService _authService;

        public SyncService(
            IFolderApiRepository folderApiRepository,
            ISiteApiRepository siteApiRepository,
            IFolderRepository folderRepository,
            ISiteRepository siteRepository,
            IAuthService authService)
        {
            _folderApiRepository = folderApiRepository;
            _siteApiRepository = siteApiRepository;
            _folderRepository = folderRepository;
            _siteRepository = siteRepository;
            _authService = authService;
        }

        public async Task<bool> SyncAsync()
        {
            // TODO: store now in settings and only fetch from last time stored
            var now = DateTime.UtcNow.AddYears(-100);

            var siteTask = await SyncSitesAsync(now);
            var folderTask = await SyncFoldersAsync(now);

            return siteTask && folderTask;
        }

        private async Task<bool> SyncFoldersAsync(DateTime now)
        {
            var folderResponse = await _folderApiRepository.GetAsync();
            if(!folderResponse.Succeeded)
            {
                return false;
            }

            var serverFolders = folderResponse.Result.Data;
            var folders = await _folderRepository.GetAllByUserIdAsync(_authService.UserId);

            foreach(var serverFolder in serverFolders.Where(f => f.RevisionDate >= now))
            {
                var data = new FolderData(serverFolder, _authService.UserId);
                var existingLocalFolder = folders.SingleOrDefault(f => f.Id == serverFolder.Id);
                if(existingLocalFolder == null)
                {
                    await _folderRepository.InsertAsync(data);
                }
                else
                {
                    await _folderRepository.UpdateAsync(data);
                }
            }

            foreach(var folder in folders.Where(localFolder => !serverFolders.Any(serverFolder => serverFolder.Id == localFolder.Id)))
            {
                await _folderRepository.DeleteAsync(folder.Id);
            }

            return true;
        }

        private async Task<bool> SyncSitesAsync(DateTime now)
        {
            var siteResponse = await _siteApiRepository.GetAsync();
            if(!siteResponse.Succeeded)
            {
                return false;
            }

            var serverSites = siteResponse.Result.Data;
            var sites = await _siteRepository.GetAllByUserIdAsync(_authService.UserId);

            foreach(var serverSite in serverSites.Where(s => s.RevisionDate >= now))
            {
                var data = new SiteData(serverSite, _authService.UserId);
                var existingLocalSite = sites.SingleOrDefault(s => s.Id == serverSite.Id);
                if(existingLocalSite == null)
                {
                    await _siteRepository.InsertAsync(data);
                }
                else
                {
                    await _siteRepository.UpdateAsync(data);
                }
            }

            foreach(var site in sites.Where(localSite => !serverSites.Any(serverSite => serverSite.Id == localSite.Id)))
            {
                await _siteRepository.DeleteAsync(site.Id);
            }

            return true;
        }
    }
}
