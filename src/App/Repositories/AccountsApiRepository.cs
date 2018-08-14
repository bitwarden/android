using System;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Plugin.Connectivity.Abstractions;
using Newtonsoft.Json;
using Bit.App.Utilities;

namespace Bit.App.Repositories
{
    public class AccountsApiRepository : BaseApiRepository, IAccountsApiRepository
    {
        public AccountsApiRepository(
            IConnectivity connectivity,
            IHttpService httpService,
            ITokenService tokenService)
            : base(connectivity, httpService, tokenService)
        { }

        protected override string ApiRoute => "/accounts";

        public virtual async Task<ApiResult<PreloginResponse>> PostPreloginAsync(PreloginRequest requestObj)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<PreloginResponse>();
            }

            using(var client = HttpService.ApiClient)
            {
                var requestMessage = new TokenHttpRequestMessage(requestObj)
                {
                    Method = HttpMethod.Post,
                    RequestUri = new Uri(string.Concat(client.BaseAddress, ApiRoute, "/prelogin")),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync<PreloginResponse>(response).ConfigureAwait(false);
                    }

                    var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    var responseObj = JsonConvert.DeserializeObject<PreloginResponse>(responseContent);
                    return ApiResult<PreloginResponse>.Success(responseObj, response.StatusCode);
                }
                catch
                {
                    return HandledWebException<PreloginResponse>();
                }
            }
        }

        public virtual async Task<ApiResult> PostRegisterAsync(RegisterRequest requestObj)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected();
            }

            using(var client = HttpService.ApiClient)
            {
                var requestMessage = new TokenHttpRequestMessage(requestObj)
                {
                    Method = HttpMethod.Post,
                    RequestUri = new Uri(string.Concat(client.BaseAddress, ApiRoute, "/register")),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync(response).ConfigureAwait(false);
                    }

                    return ApiResult.Success(response.StatusCode);
                }
                catch
                {
                    return HandledWebException();
                }
            }
        }

        public virtual async Task<ApiResult> PostPasswordHintAsync(PasswordHintRequest requestObj)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected();
            }

            using(var client = HttpService.ApiClient)
            {
                var requestMessage = new TokenHttpRequestMessage(requestObj)
                {
                    Method = HttpMethod.Post,
                    RequestUri = new Uri(string.Concat(client.BaseAddress, ApiRoute, "/password-hint")),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync(response).ConfigureAwait(false);
                    }

                    return ApiResult.Success(response.StatusCode);
                }
                catch
                {
                    return HandledWebException();
                }
            }
        }

        public virtual async Task<ApiResult<DateTime?>> GetAccountRevisionDateAsync()
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<DateTime?>();
            }

            var tokenStateResponse = await HandleTokenStateAsync<DateTime?>();
            if(!tokenStateResponse.Succeeded)
            {
                return tokenStateResponse;
            }

            using(var client = HttpService.ApiClient)
            {
                var requestMessage = new TokenHttpRequestMessage()
                {
                    Method = HttpMethod.Get,
                    RequestUri = new Uri(string.Concat(client.BaseAddress, ApiRoute, "/revision-date")),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync<DateTime?>(response).ConfigureAwait(false);
                    }

                    var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    if(responseContent.Contains("null"))
                    {
                        return ApiResult<DateTime?>.Success(null, response.StatusCode);
                    }

                    long ms;
                    if(!long.TryParse(responseContent, out ms))
                    {
                        return await HandleErrorAsync<DateTime?>(response).ConfigureAwait(false);
                    }
                    return ApiResult<DateTime?>.Success(Helpers.Epoc.AddMilliseconds(ms), response.StatusCode);
                }
                catch
                {
                    return HandledWebException<DateTime?>();
                }
            }
        }

        public virtual async Task<ApiResult<ProfileResponse>> GetProfileAsync()
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<ProfileResponse>();
            }

            var tokenStateResponse = await HandleTokenStateAsync<ProfileResponse>();
            if(!tokenStateResponse.Succeeded)
            {
                return tokenStateResponse;
            }

            using(var client = HttpService.ApiClient)
            {
                var requestMessage = new TokenHttpRequestMessage()
                {
                    Method = HttpMethod.Get,
                    RequestUri = new Uri(string.Concat(client.BaseAddress, ApiRoute, "/profile")),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync<ProfileResponse>(response).ConfigureAwait(false);
                    }

                    var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    var responseObj = JsonConvert.DeserializeObject<ProfileResponse>(responseContent);
                    return ApiResult<ProfileResponse>.Success(responseObj, response.StatusCode);
                }
                catch
                {
                    return HandledWebException<ProfileResponse>();
                }
            }
        }
    }
}
