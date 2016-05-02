using System;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using ModernHttpClient;
using Newtonsoft.Json;

namespace Bit.App.Services
{
    public class ApiService : IApiService
    {
        public ApiService()
        {
            Client = new HttpClient(new NativeMessageHandler());
            Client.BaseAddress = new Uri("https://api.bitwarden.com");
        }

        public HttpClient Client { get; set; }

        public async Task<ApiResult<T>> HandleErrorAsync<T>(HttpResponseMessage response)
        {
            var error = new ApiError
            {
                Message = "An unknown error has occured.",
                StatusCode = response.StatusCode
            };

            try
            {
                var responseContent = await response.Content.ReadAsStringAsync();
                var errorResponseModel = JsonConvert.DeserializeObject<ErrorResponse>(responseContent);
                error.Message = errorResponseModel.Message;
            }
            catch(JsonReaderException) { }

            return ApiResult<T>.Failed(error);
        }
    }
}
