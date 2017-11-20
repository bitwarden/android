using System;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Plugin.Connectivity.Abstractions;

namespace Bit.App.Repositories
{
    public class FolderApiRepository : ApiRepository<FolderRequest, FolderResponse, string>, IFolderApiRepository
    {
        public FolderApiRepository(
            IConnectivity connectivity,
            IHttpService httpService,
            ITokenService tokenService)
            : base(connectivity, httpService, tokenService)
        { }

        protected override string ApiRoute => "/folders";
    }
}
