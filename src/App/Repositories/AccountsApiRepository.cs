using System;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Plugin.Connectivity.Abstractions;
using System.Net;

namespace Bit.App.Repositories
{
    public class AccountsApiRepository : BaseApiRepository, IAccountsApiRepository
    {
        private static readonly DateTime _epoc = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);

        public AccountsApiRepository(
            IConnectivity connectivity,
            IHttpService httpService,
            ITokenService tokenService)
            : base(connectivity, httpService, tokenService)
        { }

        protected override string ApiRoute => "accounts";

        public virtual async Task<ApiResult> PostRegisterAsync(RegisterRequest requestObj)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected();
            }

            using(var client = HttpService.Client)
            {
                var requestMessage = new TokenHttpRequestMessage(requestObj)
                {
                    Method = HttpMethod.Post,
                    RequestUri = new Uri(client.BaseAddress, string.Concat(ApiRoute, "/register")),
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

            using(var client = HttpService.Client)
            {
                var requestMessage = new TokenHttpRequestMessage(requestObj)
                {
                    Method = HttpMethod.Post,
                    RequestUri = new Uri(client.BaseAddress, string.Concat(ApiRoute, "/password-hint")),
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

        public virtual async Task<ApiResult<DateTime?>> GetAccountRevisionDate()
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

            using(var client = HttpService.Client)
            {
                var requestMessage = new TokenHttpRequestMessage()
                {
                    Method = HttpMethod.Get,
                    RequestUri = new Uri(client.BaseAddress, string.Concat(ApiRoute, "/revision-date")),
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
                    return ApiResult<DateTime?>.Success(_epoc.AddMilliseconds(ms), response.StatusCode);
                }
                catch
                {
                    return HandledWebException<DateTime?>();
                }
            }
        }
    }
}
