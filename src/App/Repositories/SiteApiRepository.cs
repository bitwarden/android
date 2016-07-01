using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using Plugin.Connectivity.Abstractions;

namespace Bit.App.Repositories
{
    public class SiteApiRepository : ApiRepository<SiteRequest, SiteResponse, string>, ISiteApiRepository
    {
        public SiteApiRepository(IConnectivity connectivity)
            : base(connectivity)
        { }

        protected override string ApiRoute => "sites";
    }
}
