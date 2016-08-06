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
            var appIdService = Resolver.Resolve<IAppIdService>();
            if(authService.IsAuthenticated)
            {
                Headers.Add("Authorization", $"Bearer {authService.Token}");
            }
            if(!string.IsNullOrWhiteSpace(appIdService.AppId))
            {
                Headers.Add("DeviceIdentifier", appIdService.AppId);
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
