using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;

namespace Bit.App.Repositories
{
    public class CipherApiRepository : BaseApiRepository, ICipherApiRepository
    {
        protected override string ApiRoute => "ciphers";

        public virtual async Task<ApiResult<CipherResponse>> GetByIdAsync(string id)
        {
            var requestMessage = new TokenHttpRequestMessage()
            {
                Method = HttpMethod.Get,
                RequestUri = new Uri(Client.BaseAddress, string.Concat(ApiRoute, "/", id)),
            };

            var response = await Client.SendAsync(requestMessage);
            if(!response.IsSuccessStatusCode)
            {
                return await HandleErrorAsync<CipherResponse>(response);
            }

            var responseContent = await response.Content.ReadAsStringAsync();
            var responseObj = JsonConvert.DeserializeObject<CipherResponse>(responseContent);
            return ApiResult<CipherResponse>.Success(responseObj, response.StatusCode);
        }

        public virtual async Task<ApiResult<ListResponse<CipherResponse>>> GetAsync()
        {
            var requestMessage = new TokenHttpRequestMessage()
            {
                Method = HttpMethod.Get,
                RequestUri = new Uri(Client.BaseAddress, ApiRoute),
            };

            var response = await Client.SendAsync(requestMessage);
            if(!response.IsSuccessStatusCode)
            {
                return await HandleErrorAsync<ListResponse<CipherResponse>>(response);
            }

            var responseContent = await response.Content.ReadAsStringAsync();
            var responseObj = JsonConvert.DeserializeObject<ListResponse<CipherResponse>>(responseContent);
            return ApiResult<ListResponse<CipherResponse>>.Success(responseObj, response.StatusCode);
        }

        public virtual async Task<ApiResult<CipherHistoryResponse>> GetByRevisionDateWithHistoryAsync(DateTime since)
        {
            var requestMessage = new TokenHttpRequestMessage()
            {
                Method = HttpMethod.Get,
                RequestUri = new Uri(Client.BaseAddress, string.Concat(ApiRoute, "/history", "?since=", since)),
            };

            var response = await Client.SendAsync(requestMessage);
            if(!response.IsSuccessStatusCode)
            {
                return await HandleErrorAsync<CipherHistoryResponse>(response);
            }

            var responseContent = await response.Content.ReadAsStringAsync();
            var responseObj = JsonConvert.DeserializeObject<CipherHistoryResponse>(responseContent);
            return ApiResult<CipherHistoryResponse>.Success(responseObj, response.StatusCode);
        }
    }
}
