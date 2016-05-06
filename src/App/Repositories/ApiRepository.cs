using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;

namespace Bit.App.Repositories
{
    public abstract class ApiRepository<TRequest, TResponse, TId> : BaseApiRepository, IApiRepository<TRequest, TResponse, TId>
        where TId : IEquatable<TId>
        where TRequest : class
        where TResponse : class
    {
        public ApiRepository()
        { }

        public virtual async Task<ApiResult<TResponse>> GetByIdAsync(TId id)
        {
            var requestMessage = new TokenHttpRequestMessage()
            {
                Method = HttpMethod.Get,
                RequestUri = new Uri(Client.BaseAddress, string.Concat(ApiRoute, "/", id)),
            };

            var response = await Client.SendAsync(requestMessage);
            if(!response.IsSuccessStatusCode)
            {
                return await HandleErrorAsync<TResponse>(response);
            }

            var responseContent = await response.Content.ReadAsStringAsync();
            var responseObj = JsonConvert.DeserializeObject<TResponse>(responseContent);
            return ApiResult<TResponse>.Success(responseObj, response.StatusCode);
        }

        public virtual async Task<ApiResult<ListResponse<TResponse>>> GetAsync()
        {
            var requestMessage = new TokenHttpRequestMessage()
            {
                Method = HttpMethod.Get,
                RequestUri = new Uri(Client.BaseAddress, ApiRoute),
            };

            var response = await Client.SendAsync(requestMessage);
            if(!response.IsSuccessStatusCode)
            {
                return await HandleErrorAsync<ListResponse<TResponse>>(response);
            }

            var responseContent = await response.Content.ReadAsStringAsync();
            var responseObj = JsonConvert.DeserializeObject<ListResponse<TResponse>>(responseContent);
            return ApiResult<ListResponse<TResponse>>.Success(responseObj, response.StatusCode);
        }

        public virtual async Task<ApiResult<TResponse>> PostAsync(TRequest requestObj)
        {
            var requestMessage = new TokenHttpRequestMessage(requestObj)
            {
                Method = HttpMethod.Post,
                RequestUri = new Uri(Client.BaseAddress, ApiRoute),
            };

            var response = await Client.SendAsync(requestMessage);
            if(!response.IsSuccessStatusCode)
            {
                return await HandleErrorAsync<TResponse>(response);
            }

            var responseContent = await response.Content.ReadAsStringAsync();
            var responseObj = JsonConvert.DeserializeObject<TResponse>(responseContent);
            return ApiResult<TResponse>.Success(responseObj, response.StatusCode);
        }

        public virtual async Task<ApiResult<TResponse>> PutAsync(TId id, TRequest requestObj)
        {
            var requestMessage = new TokenHttpRequestMessage(requestObj)
            {
                Method = HttpMethod.Put,
                RequestUri = new Uri(Client.BaseAddress, string.Concat(ApiRoute, "/", id)),
            };

            var response = await Client.SendAsync(requestMessage);
            if(!response.IsSuccessStatusCode)
            {
                return await HandleErrorAsync<TResponse>(response);
            }

            var responseContent = await response.Content.ReadAsStringAsync();
            var responseObj = JsonConvert.DeserializeObject<TResponse>(responseContent);
            return ApiResult<TResponse>.Success(responseObj, response.StatusCode);
        }

        public virtual async Task<ApiResult<object>> DeleteAsync(TId id)
        {
            var requestMessage = new TokenHttpRequestMessage()
            {
                Method = HttpMethod.Delete,
                RequestUri = new Uri(Client.BaseAddress, string.Concat(ApiRoute, "/", id)),
            };

            var response = await Client.SendAsync(requestMessage);
            if(!response.IsSuccessStatusCode)
            {
                return await HandleErrorAsync<object>(response);
            }

            return ApiResult<object>.Success(null, response.StatusCode);
        }
    }
}
