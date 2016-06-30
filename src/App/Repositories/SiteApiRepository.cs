using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;

namespace Bit.App.Repositories
{
    public class SiteApiRepository : ApiRepository<SiteRequest, SiteResponse, string>, ISiteApiRepository
    {
        protected override string ApiRoute => "sites";
    }
}
