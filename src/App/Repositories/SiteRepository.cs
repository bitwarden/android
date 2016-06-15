using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Data;

namespace Bit.App.Repositories
{
    public class SiteRepository : Repository<SiteData, string>, ISiteRepository
    {
        public SiteRepository(ISqlService sqlService)
            : base(sqlService)
        { }

        public Task<IEnumerable<SiteData>> GetAllByUserIdAsync(string userId)
        {
            var sites = Connection.Table<SiteData>().Where(f => f.UserId == userId).Cast<SiteData>();
            return Task.FromResult(sites);
        }

        public Task<IEnumerable<SiteData>> GetAllByUserIdAsync(string userId, bool favorite)
        {
            var sites = Connection.Table<SiteData>().Where(f => f.UserId == userId && f.Favorite == favorite).Cast<SiteData>();
            return Task.FromResult(sites);
        }
    }
}
