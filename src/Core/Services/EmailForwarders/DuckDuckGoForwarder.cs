using System.Net.Http;
using System.Net.Http.Headers;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Newtonsoft.Json.Linq;

namespace Bit.Core.Services.EmailForwarders
{
    public class DuckDuckGoForwarder : BaseForwarder<ForwarderOptions>
    {
        protected override string RequestUri => "https://quack.duckduckgo.com/api/email/addresses";

        protected override void ConfigureHeaders(HttpRequestHeaders headers, ForwarderOptions options)
        {
            headers.Add("Authorization", $"Bearer {options.ApiKey}");
        }

        protected override Task<HttpContent> GetContentAsync(IApiService apiService, ForwarderOptions options) => Task.FromResult<HttpContent>(null);

        protected override string HandleResponse(JObject result)
        {
            return $"{result["address"]?.ToString()}@duck.com";
        }
    }
}
