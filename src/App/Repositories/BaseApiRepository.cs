using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using Plugin.Connectivity.Abstractions;
using Bit.App.Abstractions;
using System.Net;
using Newtonsoft.Json.Linq;

namespace Bit.App.Repositories
{
    public abstract class BaseApiRepository
    {
        public BaseApiRepository(
            IConnectivity connectivity,
            IHttpService httpService,
            ITokenService tokenService)
        {
            Connectivity = connectivity;
            HttpService = httpService;
            TokenService = tokenService;
        }

        protected IConnectivity Connectivity { get; private set; }
        protected IHttpService HttpService { get; private set; }
        protected ITokenService TokenService { get; private set; }
        protected abstract string ApiRoute { get; }

        protected async Task<ApiResult> HandleTokenStateAsync()
        {
            return await HandleTokenStateAsync(
                () => ApiResult.Success(HttpStatusCode.OK),
                () => HandledWebException(),
                (r) => HandleErrorAsync(r));
        }

        protected async Task<ApiResult<T>> HandleTokenStateAsync<T>()
        {
            return await HandleTokenStateAsync(
                () => ApiResult<T>.Success(default(T), HttpStatusCode.OK),
                () => HandledWebException<T>(),
                (r) => HandleErrorAsync<T>(r));
        }

        private async Task<T> HandleTokenStateAsync<T>(Func<T> success, Func<T> webException,
            Func<HttpResponseMessage, Task<T>> error)
        {
            if(TokenService.TokenNeedsRefresh && !string.IsNullOrWhiteSpace(TokenService.RefreshToken))
            {
                using(var client = HttpService.IdentityClient)
                {
                    var requestMessage = new HttpRequestMessage
                    {
                        Method = HttpMethod.Post,
                        RequestUri = new Uri(string.Concat(client.BaseAddress, "/connect/token")),
                        Content = new FormUrlEncodedContent(new Dictionary<string, string>
                        {
                            { "grant_type", "refresh_token" },
                            { "client_id", "mobile" },
                            { "refresh_token", TokenService.RefreshToken }
                        })
                    };

                    try
                    {
                        var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                        if(!response.IsSuccessStatusCode)
                        {
                            if(response.StatusCode == HttpStatusCode.BadRequest)
                            {
                                response.StatusCode = HttpStatusCode.Unauthorized;
                            }

                            return await error.Invoke(response).ConfigureAwait(false);
                        }

                        var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                        var tokenResponse = JsonConvert.DeserializeObject<TokenResponse>(responseContent);
                        TokenService.Token = tokenResponse.AccessToken;
                        TokenService.RefreshToken = tokenResponse.RefreshToken;
                    }
                    catch
                    {
                        return webException.Invoke();
                    }
                }
            }

            return success.Invoke();
        }

        protected ApiResult HandledNotConnected()
        {
            return ApiResult.Failed(HttpStatusCode.RequestTimeout,
                new ApiError { Message = "Not connected to the internet." });
        }

        protected ApiResult<T> HandledNotConnected<T>()
        {
            return ApiResult<T>.Failed(HttpStatusCode.RequestTimeout,
                new ApiError { Message = "Not connected to the internet." });
        }

        protected ApiResult HandledWebException()
        {
            return ApiResult.Failed(HttpStatusCode.BadGateway,
                new ApiError { Message = "There is a problem connecting to the server." });
        }

        protected ApiResult<T> HandledWebException<T>()
        {
            return ApiResult<T>.Failed(HttpStatusCode.BadGateway,
                new ApiError { Message = "There is a problem connecting to the server." });
        }

        protected async Task<ApiResult<T>> HandleErrorAsync<T>(HttpResponseMessage response)
        {
            try
            {
                var errors = await ParseErrorsAsync(response).ConfigureAwait(false);
                return ApiResult<T>.Failed(response.StatusCode, errors.ToArray());
            }
            catch
            { }

            return ApiResult<T>.Failed(response.StatusCode,
                new ApiError { Message = "An unknown error has occurred." });
        }

        protected async Task<ApiResult> HandleErrorAsync(HttpResponseMessage response)
        {
            try
            {
                var errors = await ParseErrorsAsync(response).ConfigureAwait(false);
                return ApiResult.Failed(response.StatusCode, errors.ToArray());
            }
            catch
            { }

            return ApiResult.Failed(response.StatusCode,
                new ApiError { Message = "An unknown error has occurred." });
        }

        private async Task<List<ApiError>> ParseErrorsAsync(HttpResponseMessage response)
        {
            var errors = new List<ApiError>();
            var statusCode = (int)response.StatusCode;
            if(statusCode >= 400 && statusCode <= 500)
            {
                ErrorResponse errorResponseModel = null;

                var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                if(!string.IsNullOrWhiteSpace(responseContent))
                {
                    var errorResponse = JObject.Parse(responseContent);
                    if(errorResponse["ErrorModel"] != null && errorResponse["ErrorModel"]["Message"] != null)
                    {
                        errorResponseModel = errorResponse["ErrorModel"].ToObject<ErrorResponse>();
                    }
                    else if(errorResponse["Message"] != null)
                    {
                        errorResponseModel = errorResponse.ToObject<ErrorResponse>();
                    }
                }

                if(errorResponseModel != null)
                {
                    if((errorResponseModel.ValidationErrors?.Count ?? 0) > 0)
                    {
                        foreach(var valError in errorResponseModel.ValidationErrors)
                        {
                            foreach(var errorMessage in valError.Value)
                            {
                                errors.Add(new ApiError { Message = errorMessage });
                            }
                        }
                    }
                    else
                    {
                        errors.Add(new ApiError { Message = errorResponseModel.Message });
                    }
                }
            }

            if(errors.Count == 0)
            {
                errors.Add(new ApiError { Message = "An unknown error has occurred." });
            }

            return errors;
        }
    }
}
