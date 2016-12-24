using System;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using Plugin.Connectivity.Abstractions;
using System.Net;

namespace Bit.App.Repositories
{
    public class AuthApiRepository : BaseApiRepository, IAuthApiRepository
    {
        public AuthApiRepository(
            IConnectivity connectivity,
            IHttpService httpService)
            : base(connectivity, httpService)
        { }

        protected override string ApiRoute => "auth";

        public virtual async Task<ApiResult<TokenResponse>> PostTokenAsync(TokenRequest requestObj)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<TokenResponse>();
            }

            using(var client = HttpService.Client)
            {
                var requestMessage = new TokenHttpRequestMessage(requestObj)
                {
                    Method = HttpMethod.Post,
                    RequestUri = new Uri(client.BaseAddress, string.Concat(ApiRoute, "/token")),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync<TokenResponse>(response).ConfigureAwait(false);
                    }

                    var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    var responseObj = JsonConvert.DeserializeObject<TokenResponse>(responseContent);
                    return ApiResult<TokenResponse>.Success(responseObj, response.StatusCode);
                }
                catch(WebException e)
                {
                    return HandledWebException<TokenResponse>();
                }
            }
        }

        public virtual async Task<ApiResult<TokenResponse>> PostTokenTwoFactorAsync(TokenTwoFactorRequest requestObj)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<TokenResponse>();
            }

            using(var client = HttpService.Client)
            {
                var requestMessage = new TokenHttpRequestMessage(requestObj)
                {
                    Method = HttpMethod.Post,
                    RequestUri = new Uri(client.BaseAddress, string.Concat(ApiRoute, "/token/two-factor")),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync<TokenResponse>(response).ConfigureAwait(false);
                    }

                    var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    var responseObj = JsonConvert.DeserializeObject<TokenResponse>(responseContent);
                    return ApiResult<TokenResponse>.Success(responseObj, response.StatusCode);
                }
                catch(WebException)
                {
                    return HandledWebException<TokenResponse>();
                }
            }
        }
    }
}
