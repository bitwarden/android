using System;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;

namespace Bit.App.Repositories
{
    public class AuthApiRepository : BaseApiRepository, IAuthApiRepository
    {
        protected override string ApiRoute => "auth";

        public virtual async Task<ApiResult<TokenResponse>> PostTokenAsync(TokenRequest requestObj)
        {
            var requestMessage = new TokenHttpRequestMessage(requestObj)
            {
                Method = HttpMethod.Post,
                RequestUri = new Uri(Client.BaseAddress, string.Concat(ApiRoute, "/token")),
            };

            var response = await Client.SendAsync(requestMessage);
            if(!response.IsSuccessStatusCode)
            {
                return await HandleErrorAsync<TokenResponse>(response);
            }

            var responseContent = await response.Content.ReadAsStringAsync();
            var responseObj = JsonConvert.DeserializeObject<TokenResponse>(responseContent);
            return ApiResult<TokenResponse>.Success(responseObj, response.StatusCode);
        }

        public virtual async Task<ApiResult<TokenResponse>> PostTokenTwoFactorAsync(TokenTwoFactorRequest requestObj)
        {
            var requestMessage = new TokenHttpRequestMessage(requestObj)
            {
                Method = HttpMethod.Post,
                RequestUri = new Uri(Client.BaseAddress, string.Concat(ApiRoute, "/token/two-factor")),
            };

            var response = await Client.SendAsync(requestMessage);
            if(!response.IsSuccessStatusCode)
            {
                return await HandleErrorAsync<TokenResponse>(response);
            }

            var responseContent = await response.Content.ReadAsStringAsync();
            var responseObj = JsonConvert.DeserializeObject<TokenResponse>(responseContent);
            return ApiResult<TokenResponse>.Success(responseObj, response.StatusCode);
        }
    }
}
