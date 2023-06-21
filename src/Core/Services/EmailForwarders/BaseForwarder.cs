using System;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Newtonsoft.Json.Linq;

namespace Bit.Core.Services.EmailForwarders
{
    public abstract class BaseForwarder<T>
        where T : ForwarderOptions
    {
        protected abstract string RequestUri { get; }

        public async Task<string> GenerateAsync(IApiService apiService, T options)
        {
            if (!CanGenerate(options))
            {
                return Constants.DefaultUsernameGenerated;
            }

            using (var requestMessage = new HttpRequestMessage())
            {
                requestMessage.Version = new Version(1, 0);
                requestMessage.Method = HttpMethod.Post;
                requestMessage.RequestUri = new Uri(RequestUri);
                requestMessage.Headers.Add("Accept", "application/json");

                ConfigureHeaders(requestMessage.Headers, options);
                requestMessage.Content = await GetContentAsync(apiService, options);

                try
                {
                    var response = await apiService.SendAsync(requestMessage);

                    var responseJsonString = await response.Content.ReadAsStringAsync();

                    return HandleResponse(JObject.Parse(responseJsonString));
                }
                catch (ApiException ex)
                {
                    if (IsRequestSecretInvalid(ex))
                    {
                        throw new ForwardedEmailInvalidSecretException(ex);
                    }

                    throw;
                }
            }
        }

        protected virtual bool CanGenerate(T options) => !string.IsNullOrWhiteSpace(options.ApiKey);

        protected abstract void ConfigureHeaders(HttpRequestHeaders headers, T options);

        protected abstract Task<HttpContent> GetContentAsync(IApiService apiService, T options);

        protected abstract string HandleResponse(JObject result);

        protected virtual bool IsRequestSecretInvalid(ApiException ex) => ex.Error?.StatusCode == System.Net.HttpStatusCode.Unauthorized;
    }
}
