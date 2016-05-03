using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models;
using Bit.App.Models.Api;

namespace Bit.App.Abstractions
{
    public interface ISiteService
    {
        Task<IEnumerable<Site>> GetAllAsync();
        Task<ApiResult<SiteResponse>> SaveAsync(Site site);
    }
}
