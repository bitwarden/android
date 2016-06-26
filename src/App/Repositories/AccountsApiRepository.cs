using System;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;

namespace Bit.App.Repositories
{
    public class AccountsApiRepository : BaseApiRepository, IAccountsApiRepository
    {
        protected override string ApiRoute => "accounts";

        public virtual async Task<ApiResult> PostRegisterAsync(RegisterRequest requestObj)
        {
            var requestMessage = new TokenHttpRequestMessage(requestObj)
            {
                Method = HttpMethod.Post,
                RequestUri = new Uri(Client.BaseAddress, string.Concat(ApiRoute, "/register")),
            };

            var response = await Client.SendAsync(requestMessage);
            if(!response.IsSuccessStatusCode)
            {
                return await HandleErrorAsync(response);
            }

            return ApiResult.Success(response.StatusCode);
        }
    }
}
