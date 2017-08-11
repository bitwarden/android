using System;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Plugin.Connectivity.Abstractions;
using Newtonsoft.Json;

namespace Bit.App.Repositories
{
    public class SettingsApiRepository : BaseApiRepository, ISettingsApiRepository
    {
        public SettingsApiRepository(
            IConnectivity connectivity,
            IHttpService httpService,
            ITokenService tokenService)
            : base(connectivity, httpService, tokenService)
        { }

        protected override string ApiRoute => "/settings";

        public virtual async Task<ApiResult<DomainsResponse>> GetDomains(bool excluded = false)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<DomainsResponse>();
            }

            var tokenStateResponse = await HandleTokenStateAsync<DomainsResponse>();
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
                        string.Concat(client.BaseAddress, ApiRoute, "/domains?excluded=", excluded.ToString().ToLowerInvariant())),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync<DomainsResponse>(response).ConfigureAwait(false);
                    }

                    var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    var responseObj = JsonConvert.DeserializeObject<DomainsResponse>(responseContent);
                    return ApiResult<DomainsResponse>.Success(responseObj, response.StatusCode);
                }
                catch
                {
                    return HandledWebException<DomainsResponse>();
                }
            }
        }
    }
}
