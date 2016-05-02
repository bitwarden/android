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

        public SiteService(
            ISqlService sqlService,
            IAuthService authService)
            : base(sqlService)
        {
            _authService = authService;
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
