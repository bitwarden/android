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
        public AccountsApiRepository(
            IConnectivity connectivity,
            IHttpService httpService)
            : base(connectivity, httpService)
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
                catch(WebException)
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
                catch(WebException)
                {
                    return HandledWebException();
                }
            }
        }
    }
}
