using System.Net.Http;
using System.Text;
using Bit.App.Abstractions;
using Newtonsoft.Json;
using XLabs.Ioc;

namespace Bit.App
{
    public class TokenHttpRequestMessage : HttpRequestMessage
    {
        public TokenHttpRequestMessage()
        {
            var authService = Resolver.Resolve<IAuthService>();
            Headers.Add("Authorization", $"Bearer {authService.Token}");
        }

        public TokenHttpRequestMessage(object requestObject)
            : this()
        {
            var stringContent = JsonConvert.SerializeObject(requestObject);
            Content = new StringContent(stringContent, Encoding.UTF8, "application/json");
        }
    }
}
