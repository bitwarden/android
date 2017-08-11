using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using Plugin.Connectivity.Abstractions;

namespace Bit.App.Repositories
{
    public abstract class ApiRepository<TRequest, TResponse, TId> : BaseApiRepository, IApiRepository<TRequest, TResponse, TId>
        where TId : IEquatable<TId>
        where TRequest : class
        where TResponse : class
    {
        public ApiRepository(
            IConnectivity connectivity,
            IHttpService httpService,
            ITokenService tokenService)
            : base(connectivity, httpService, tokenService)
        { }

        public virtual async Task<ApiResult<TResponse>> GetByIdAsync(TId id)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<TResponse>();
            }

            var tokenStateResponse = await HandleTokenStateAsync<TResponse>();
            if(!tokenStateResponse.Succeeded)
            {
                return tokenStateResponse;
            }

            using(var client = HttpService.ApiClient)
            {
                var requestMessage = new TokenHttpRequestMessage()
                {
                    Method = HttpMethod.Get,
                    RequestUri = new Uri(string.Concat(client.BaseAddress, ApiRoute, "/", id)),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync<TResponse>(response).ConfigureAwait(false);
                    }

                    var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    var responseObj = JsonConvert.DeserializeObject<TResponse>(responseContent);
                    return ApiResult<TResponse>.Success(responseObj, response.StatusCode);
                }
                catch
                {
                    return HandledWebException<TResponse>();
                }
            }
        }

        public virtual async Task<ApiResult<ListResponse<TResponse>>> GetAsync()
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<ListResponse<TResponse>>();
            }

            var tokenStateResponse = await HandleTokenStateAsync<ListResponse<TResponse>>();
            if(!tokenStateResponse.Succeeded)
            {
                return tokenStateResponse;
            }

            using(var client = HttpService.ApiClient)
            {
                var requestMessage = new TokenHttpRequestMessage()
                {
                    Method = HttpMethod.Get,
                    RequestUri = new Uri(string.Concat(client.BaseAddress, ApiRoute)),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync<ListResponse<TResponse>>(response).ConfigureAwait(false);
                    }

                    var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    var responseObj = JsonConvert.DeserializeObject<ListResponse<TResponse>>(responseContent);
                    return ApiResult<ListResponse<TResponse>>.Success(responseObj, response.StatusCode);
                }
                catch
                {
                    return HandledWebException<ListResponse<TResponse>>();
                }
            }
        }

        public virtual async Task<ApiResult<TResponse>> PostAsync(TRequest requestObj)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<TResponse>();
            }

            var tokenStateResponse = await HandleTokenStateAsync<TResponse>();
            if(!tokenStateResponse.Succeeded)
            {
                return tokenStateResponse;
            }

            using(var client = HttpService.ApiClient)
            {
                var requestMessage = new TokenHttpRequestMessage(requestObj)
                {
                    Method = HttpMethod.Post,
                    RequestUri = new Uri(string.Concat(client.BaseAddress, ApiRoute)),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync<TResponse>(response).ConfigureAwait(false);
                    }

                    var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    var responseObj = JsonConvert.DeserializeObject<TResponse>(responseContent);
                    return ApiResult<TResponse>.Success(responseObj, response.StatusCode);
                }
                catch
                {
                    return HandledWebException<TResponse>();
                }
            }
        }

        public virtual async Task<ApiResult<TResponse>> PutAsync(TId id, TRequest requestObj)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<TResponse>();
            }

            var tokenStateResponse = await HandleTokenStateAsync<TResponse>();
            if(!tokenStateResponse.Succeeded)
            {
                return tokenStateResponse;
            }

            using(var client = HttpService.ApiClient)
            {
                var requestMessage = new TokenHttpRequestMessage(requestObj)
                {
                    Method = HttpMethod.Put,
                    RequestUri = new Uri(string.Concat(client.BaseAddress, ApiRoute, "/", id)),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync<TResponse>(response).ConfigureAwait(false);
                    }

                    var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    var responseObj = JsonConvert.DeserializeObject<TResponse>(responseContent);
                    return ApiResult<TResponse>.Success(responseObj, response.StatusCode);
                }
                catch
                {
                    return HandledWebException<TResponse>();
                }
            }
        }

        public virtual async Task<ApiResult> DeleteAsync(TId id)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected();
            }

            var tokenStateResponse = await HandleTokenStateAsync();
            if(!tokenStateResponse.Succeeded)
            {
                return tokenStateResponse;
            }

            using(var client = HttpService.ApiClient)
            {
                var requestMessage = new TokenHttpRequestMessage()
                {
                    Method = HttpMethod.Delete,
                    RequestUri = new Uri(string.Concat(client.BaseAddress, ApiRoute, "/", id)),
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
    }
}
