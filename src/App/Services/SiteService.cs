using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Models.Data;

namespace Bit.App.Services
{
    public class SiteService : Repository<SiteData, int>, ISiteService
    {
        private readonly IAuthService _authService;
        private readonly IFolderService _folderService;

        public SiteService(
            ISqlService sqlService,
            IAuthService authService,
            IFolderService folderService)
            : base(sqlService)
        {
            _authService = authService;
            _folderService = folderService;
        }

        public new Task<IEnumerable<Site>> GetAllAsync()
        {
            var data = Connection.Table<SiteData>().Where(f => f.UserId == _authService.UserId).Cast<SiteData>();
            return Task.FromResult(data.Select(s => new Site(s)));
        }

        public async Task SaveAsync(Site site)
        {
            var data = new SiteData(site, _authService.UserId);
            data.RevisionDateTime = DateTime.UtcNow;

            if(site.FolderId.HasValue && site.ServerFolderId == null)
            {
                var folder = await _folderService.GetByIdAsync(site.FolderId.Value);
                if(folder != null)
                {
                    site.ServerFolderId = folder.ServerId;
                }
            }

            if(site.Id == 0)
            {
                await CreateAsync(data);
            }
            else
            {
                await ReplaceAsync(data);
            }

            site.Id = data.Id;
        }
    }
}
