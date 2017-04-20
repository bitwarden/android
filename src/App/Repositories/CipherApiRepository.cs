using System;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using Plugin.Connectivity.Abstractions;

namespace Bit.App.Repositories
{
    public class CipherApiRepository : BaseApiRepository, ICipherApiRepository
    {
        public CipherApiRepository(
            IConnectivity connectivity,
            IHttpService httpService,
            ITokenService tokenService)
            : base(connectivity, httpService, tokenService)
        { }

        protected override string ApiRoute => "ciphers";

        public virtual async Task<ApiResult<CipherResponse>> GetByIdAsync(string id)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<CipherResponse>();
            }

            var tokenStateResponse = await HandleTokenStateAsync<CipherResponse>();
            if(!tokenStateResponse.Succeeded)
            {
                return tokenStateResponse;
            }

            using(var client = HttpService.Client)
            {
                var requestMessage = new TokenHttpRequestMessage()
                {
                    Method = HttpMethod.Get,
                    RequestUri = new Uri(client.BaseAddress, string.Concat(ApiRoute, "/", id)),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync<CipherResponse>(response).ConfigureAwait(false);
                    }

                    var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    var responseObj = JsonConvert.DeserializeObject<CipherResponse>(responseContent);
                    return ApiResult<CipherResponse>.Success(responseObj, response.StatusCode);
                }
                catch
                {
                    return HandledWebException<CipherResponse>();
                }
            }
        }

        public virtual async Task<ApiResult<ListResponse<CipherResponse>>> GetAsync()
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<ListResponse<CipherResponse>>();
            }

            var tokenStateResponse = await HandleTokenStateAsync<ListResponse<CipherResponse>>();
            if(!tokenStateResponse.Succeeded)
            {
                return tokenStateResponse;
            }

            using(var client = HttpService.Client)
            {
                var requestMessage = new TokenHttpRequestMessage()
                {
                    Method = HttpMethod.Get,
                    RequestUri = new Uri(client.BaseAddress,
                        string.Format("{0}?includeFolders=false&includeShared=true", ApiRoute)),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync<ListResponse<CipherResponse>>(response).ConfigureAwait(false);
                    }

                    var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    var responseObj = JsonConvert.DeserializeObject<ListResponse<CipherResponse>>(responseContent);
                    return ApiResult<ListResponse<CipherResponse>>.Success(responseObj, response.StatusCode);
                }
                catch
                {
                    return HandledWebException<ListResponse<CipherResponse>>();
                }
            }
        }
    }
}
