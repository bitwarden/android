using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Threading.Tasks;
using Bit.App.Models.Api;
using ModernHttpClient;
using Newtonsoft.Json;

namespace Bit.App.Repositories
{
    public abstract class BaseApiRepository
    {
        public BaseApiRepository()
        {
            Client = new HttpClient(new NativeMessageHandler());
            Client.BaseAddress = new Uri("https://api.bitwarden.com");
            Client.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
        }

        protected virtual HttpClient Client { get; private set; }
        protected abstract string ApiRoute { get; }

        public async Task<ApiResult<T>> HandleErrorAsync<T>(HttpResponseMessage response)
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

        public async Task<ApiResult> HandleErrorAsync(HttpResponseMessage response)
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
