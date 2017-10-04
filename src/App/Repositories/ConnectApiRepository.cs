using System;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using Plugin.Connectivity.Abstractions;
using Newtonsoft.Json.Linq;
using System.Collections.Generic;
using Bit.App.Enums;

namespace Bit.App.Repositories
{
    public class ConnectApiRepository : BaseApiRepository, IConnectApiRepository
    {
        public ConnectApiRepository(
            IConnectivity connectivity,
            IHttpService httpService,
            ITokenService tokenService)
            : base(connectivity, httpService, tokenService)
        { }

        protected override string ApiRoute => "/connect";

        public virtual async Task<ApiResult<TokenResponse>> PostTokenAsync(TokenRequest requestObj)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<TokenResponse>();
            }

            using(var client = HttpService.IdentityClient)
            {
                var requestMessage = new HttpRequestMessage
                {
                    Method = HttpMethod.Post,
                    RequestUri = new Uri(string.Concat(client.BaseAddress, ApiRoute, "/token")),
                    Content = new FormUrlEncodedContent(requestObj.ToIdentityTokenRequest())
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);

                    if(!response.IsSuccessStatusCode)
                    {
                        var errorResponse = JObject.Parse(responseContent);
                        if(errorResponse["TwoFactorProviders2"] != null)
                        {
                            TokenService.SetTwoFactorToken(requestObj.Email, null);

                            return ApiResult<TokenResponse>.Success(new TokenResponse
                            {
                                TwoFactorProviders2 =
                                    errorResponse["TwoFactorProviders2"]
                                    .ToObject<Dictionary<TwoFactorProviderType, Dictionary<string, object>>>()
                            }, response.StatusCode);
                        }

                        return await HandleErrorAsync<TokenResponse>(response).ConfigureAwait(false);
                    }

                    var responseObj = JsonConvert.DeserializeObject<TokenResponse>(responseContent);
                    return ApiResult<TokenResponse>.Success(responseObj, response.StatusCode);
                }
                catch
                {
                    return HandledWebException<TokenResponse>();
                }
            }
        }
    }
}
