using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using Plugin.Connectivity.Abstractions;
using Bit.App.Abstractions;

namespace Bit.App.Repositories
{
    public abstract class BaseApiRepository
    {
        public BaseApiRepository(IConnectivity connectivity, IHttpService httpService)
        {
            Connectivity = connectivity;
            HttpService = httpService;
        }

        protected IConnectivity Connectivity { get; private set; }
        protected IHttpService HttpService { get; private set; }
        protected abstract string ApiRoute { get; }

        protected ApiResult HandledNotConnected()
        {
            return ApiResult.Failed(System.Net.HttpStatusCode.RequestTimeout,
                new ApiError { Message = "Not connected to the internet." });
        }

        protected ApiResult<T> HandledNotConnected<T>()
        {
            return ApiResult<T>.Failed(System.Net.HttpStatusCode.RequestTimeout,
                new ApiError { Message = "Not connected to the internet." });
        }

        protected ApiResult HandledWebException()
        {
            return ApiResult.Failed(System.Net.HttpStatusCode.BadGateway,
                new ApiError { Message = "There is a problem connecting to the server." });
        }

        protected ApiResult<T> HandledWebException<T>()
        {
            return ApiResult<T>.Failed(System.Net.HttpStatusCode.BadGateway,
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
