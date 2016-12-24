using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using Plugin.Connectivity.Abstractions;
using System.Net;

namespace Bit.App.Repositories
{
    public class FolderApiRepository : ApiRepository<FolderRequest, FolderResponse, string>, IFolderApiRepository
    {
        public FolderApiRepository(
            IConnectivity connectivity,
            IHttpService httpService)
            : base(connectivity, httpService)
        { }

        protected override string ApiRoute => "folders";

        public virtual async Task<ApiResult<ListResponse<FolderResponse>>> GetByRevisionDateAsync(DateTime since)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<ListResponse<FolderResponse>>();
            }

            using(var client = HttpService.Client)
            {
                var requestMessage = new TokenHttpRequestMessage()
                {
                    Method = HttpMethod.Get,
                    RequestUri = new Uri(client.BaseAddress, string.Concat(ApiRoute, "?since=", since)),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync<ListResponse<FolderResponse>>(response).ConfigureAwait(false);
                    }

                    var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    var responseObj = JsonConvert.DeserializeObject<ListResponse<FolderResponse>>(responseContent);
                    return ApiResult<ListResponse<FolderResponse>>.Success(responseObj, response.StatusCode);
                }
                catch(WebException)
                {
                    return HandledWebException<ListResponse<FolderResponse>>();
                }
            }
        }
    }
}
