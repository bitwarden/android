using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
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
                var responseContent = await response.Content.ReadAsStringAsync();
                var errorResponseModel = JsonConvert.DeserializeObject<ErrorResponse>(responseContent);

                var errors = new List<ApiError>();
                foreach(var valError in errorResponseModel.ValidationErrors)
                {
                    foreach(var errorMessage in valError.Value)
                    {
                        errors.Add(new ApiError { Message = errorMessage });
                    }
                }

                return ApiResult<T>.Failed(response.StatusCode, errors.ToArray());
            }
            catch(JsonReaderException)
            { }

            return ApiResult<T>.Failed(response.StatusCode, new ApiError { Message = "An unknown error has occured." });
        }
    }
}
