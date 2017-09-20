using System;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Plugin.Connectivity.Abstractions;
using Newtonsoft.Json;

namespace Bit.App.Repositories
{
    public class SyncApiRepository : BaseApiRepository, ISyncApiRepository
    {
        public SyncApiRepository(
            IConnectivity connectivity,
            IHttpService httpService,
            ITokenService tokenService)
            : base(connectivity, httpService, tokenService)
        { }

        protected override string ApiRoute => "sync";

        public virtual async Task<ApiResult<SyncResponse>> Get()
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<SyncResponse>();
            }

            var tokenStateResponse = await HandleTokenStateAsync<SyncResponse>();
            if(!tokenStateResponse.Succeeded)
            {
                return tokenStateResponse;
            }

            using(var client = HttpService.ApiClient)
            {
                var requestMessage = new TokenHttpRequestMessage()
                {
                    Method = HttpMethod.Get,
                    RequestUri = new Uri(
                        string.Concat(client.BaseAddress, ApiRoute)),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync<SyncResponse>(response).ConfigureAwait(false);
                    }

                    var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    var responseObj = JsonConvert.DeserializeObject<SyncResponse>(responseContent);
                    return ApiResult<SyncResponse>.Success(responseObj, response.StatusCode);
                }
                catch
                {
                    return HandledWebException<SyncResponse>();
                }
            }
        }
    }
}
