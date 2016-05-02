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
        public SiteService(ISqlService sqlite)
            : base(sqlite) { }

        public new async Task<IEnumerable<Site>> GetAllAsync()
        {
            var data = await base.GetAllAsync();
            return data.Select(s => new Site(s));
        }

        public async Task SaveAsync(Site site)
        {
            var data = new SiteData(site);
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
