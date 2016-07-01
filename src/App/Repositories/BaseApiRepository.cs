using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using Plugin.Connectivity.Abstractions;

namespace Bit.App.Repositories
{
    public abstract class BaseApiRepository
    {
        public BaseApiRepository(IConnectivity connectivity)
        {
            Connectivity = connectivity;
        }

        protected IConnectivity Connectivity { get; private set; }
        protected abstract string ApiRoute { get; }

        protected ApiResult HandledNotConnected()
        {
            return ApiResult.Failed(System.Net.HttpStatusCode.RequestTimeout, new ApiError { Message = "Not connected to the internet." });
        }

        protected ApiResult<T> HandledNotConnected<T>()
        {
            return ApiResult<T>.Failed(System.Net.HttpStatusCode.RequestTimeout, new ApiError { Message = "Not connected to the internet." });
        }

        protected async Task<ApiResult<T>> HandleErrorAsync<T>(HttpResponseMessage response)
        {
            try
            {
                var errors = await ParseErrorsAsync(response);
                return ApiResult<T>.Failed(response.StatusCode, errors.ToArray());
            }
            catch(JsonReaderException)
            { }

            return ApiResult<T>.Failed(response.StatusCode, new ApiError { Message = "An unknown error has occured." });
        }

        protected async Task<ApiResult> HandleErrorAsync(HttpResponseMessage response)
        {
            try
            {
                var errors = await ParseErrorsAsync(response);
                return ApiResult.Failed(response.StatusCode, errors.ToArray());
            }
            catch(JsonReaderException)
            { }

            return ApiResult.Failed(response.StatusCode, new ApiError { Message = "An unknown error has occured." });
        }

        private async Task<List<ApiError>> ParseErrorsAsync(HttpResponseMessage response)
        {
            var errors = new List<ApiError>();
            if(response.StatusCode == System.Net.HttpStatusCode.BadRequest)
            {
                var responseContent = await response.Content.ReadAsStringAsync();
                var errorResponseModel = JsonConvert.DeserializeObject<ErrorResponse>(responseContent);

                foreach(var valError in errorResponseModel.ValidationErrors)
                {
                    foreach(var errorMessage in valError.Value)
                    {
                        errors.Add(new ApiError { Message = errorMessage });
                    }
                }
            }

            return errors;
        }
    }
}
