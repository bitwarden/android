using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models.Data;

namespace Bit.App.Abstractions
{
    public interface ISiteRepository : IRepository<SiteData, string>
    {
        Task<IEnumerable<SiteData>> GetAllByUserIdAsync(string userId);
        Task<IEnumerable<SiteData>> GetAllByUserIdAsync(string userId, bool favorite);
    }
}
