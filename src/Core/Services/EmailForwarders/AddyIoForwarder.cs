using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Newtonsoft.Json.Linq;

namespace Bit.Core.Services.EmailForwarders
{
    public class AddyIoForwarderOptions : ForwarderOptions
    {
        public string DomainName { get; set; }
    }

    public class AddyIoForwarder : BaseForwarder<AddyIoForwarderOptions>
    {
        protected override string RequestUri => "https://app.addy.io/api/v1/aliases";

        protected override bool CanGenerate(AddyIoForwarderOptions options)
        {
            return !string.IsNullOrWhiteSpace(options.ApiKey) && !string.IsNullOrWhiteSpace(options.DomainName);
        }

        protected override void ConfigureHeaders(HttpRequestHeaders headers, AddyIoForwarderOptions options)
        {
            headers.Add("Authorization", $"Bearer {options.ApiKey}");
        }

        protected override Task<HttpContent> GetContentAsync(IApiService apiService, AddyIoForwarderOptions options)
        {
            return Task.FromResult<HttpContent>(new FormUrlEncodedContent(new Dictionary<string, string>
            {
                ["domain"] = options.DomainName
            }));
        }

        protected override string HandleResponse(JObject result)
        {
            return result["data"]?["email"]?.ToString();
        }
    }
}
