using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models.Api;

namespace Bit.App.Abstractions
{
    public interface ISiteApiRepository : IApiRepository<SiteRequest, SiteResponse, string>
    {
        Task<ApiResult<ListResponse<SiteResponse>>> GetByRevisionDateAsync(DateTime since);
    }
}