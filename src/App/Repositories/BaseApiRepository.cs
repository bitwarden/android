using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using Plugin.Connectivity.Abstractions;
using Bit.App.Abstractions;
using System.Net;
using XLabs.Ioc;

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
            if(!string.IsNullOrWhiteSpace(TokenService.AuthBearer) && string.IsNullOrWhiteSpace(TokenService.Token))
            {
                // Migrate from old auth bearer to new access token

                var deviceInfoService = Resolver.Resolve<IDeviceInfoService>();
                var appIdService = Resolver.Resolve<IAppIdService>();

                using(var client = HttpService.Client)
                {
                    var requestMessage = new HttpRequestMessage
                    {
                        Method = HttpMethod.Post,
                        RequestUri = new Uri(client.BaseAddress, string.Concat(ApiRoute, "/token")),
                        Content = new FormUrlEncodedContent(new TokenRequest
                        {
                            Email = "abcdefgh",
                            MasterPasswordHash = "abcdefgh",
                            OldAuthBearer = TokenService.AuthBearer,
                            Device = new DeviceRequest(appIdService, deviceInfoService)
                        }.ToIdentityTokenRequest())
                    };

                    try
                    {
                        var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                        if(!response.IsSuccessStatusCode)
                        {
                            return await error.Invoke(response).ConfigureAwait(false);
                        }

                        var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                        var tokenResponse = JsonConvert.DeserializeObject<TokenResponse>(responseContent);
                        TokenService.Token = tokenResponse.AccessToken;
                        TokenService.RefreshToken = tokenResponse.RefreshToken;
                        TokenService.AuthBearer = null;
                    }
                    catch(WebException)
                    {
                        return webException.Invoke();
                    }
                }
            }
            else if(TokenService.TokenNeedsRefresh && !string.IsNullOrWhiteSpace(TokenService.RefreshToken))
            {
                using(var client = HttpService.Client)
                {
                    var requestMessage = new HttpRequestMessage
                    {
                        Method = HttpMethod.Post,
                        RequestUri = new Uri(client.BaseAddress, string.Concat(ApiRoute, "/token")),
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
                            return await error.Invoke(response).ConfigureAwait(false);
                        }

                        var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                        var tokenResponse = JsonConvert.DeserializeObject<TokenResponse>(responseContent);
                        TokenService.Token = tokenResponse.AccessToken;
                        TokenService.RefreshToken = tokenResponse.RefreshToken;
                    }
                    catch(WebException)
                    {
                        return webException.Invoke();
                    }
                }
            }
            else if(!string.IsNullOrWhiteSpace(TokenService.AuthBearer))
            {
                TokenService.AuthBearer = null;
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
                new ApiError { Message = "An unknown error has occured." });
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
                new ApiError { Message = "An unknown error has occured." });
        }

        private async Task<List<ApiError>> ParseErrorsAsync(HttpResponseMessage response)
        {
            var errors = new List<ApiError>();
            var statusCode = (int)response.StatusCode;
            if(statusCode >= 400 && statusCode <= 500)
            {
                var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                var errorResponseModel = JsonConvert.DeserializeObject<ErrorResponse>(responseContent);

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
                else
                {
                    errors.Add(new ApiError { Message = "An unknown error has occured." });
                }
            }

            return errors;
        }
    }
}
