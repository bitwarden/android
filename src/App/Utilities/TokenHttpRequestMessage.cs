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
            var tokenService = Resolver.Resolve<ITokenService>();
            var appIdService = Resolver.Resolve<IAppIdService>();

            if(!string.IsNullOrWhiteSpace(tokenService.Token))
            {
                Headers.Add("Authorization", $"Bearer3 {tokenService.Token}");
            }
            if(!string.IsNullOrWhiteSpace(appIdService.AppId))
            {
                Headers.Add("Device-Identifier", appIdService.AppId);
            }
        }

        public TokenHttpRequestMessage(object requestObject)
            : this()
        {
            var stringContent = JsonConvert.SerializeObject(requestObject);
            Content = new StringContent(stringContent, Encoding.UTF8, "application/json");
        }
    }
}
