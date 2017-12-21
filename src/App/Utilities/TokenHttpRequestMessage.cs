using System.Net.Http;
using System.Text;
using Bit.App.Abstractions;
using Bit.App.Enums;
using Bit.App.Utilities;
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
            var deviceInfoService = Resolver.Resolve<IDeviceInfoService>();

            if(!string.IsNullOrWhiteSpace(tokenService.Token))
            {
                Headers.Add("Authorization", $"Bearer {tokenService.Token}");
            }
            if(!string.IsNullOrWhiteSpace(appIdService.AppId))
            {
                Headers.Add("Device-Identifier", appIdService.AppId);
            }

            Headers.Add("Device-Type", ((int)Helpers.OnPlatform(iOS: DeviceType.iOS,
                Android: DeviceType.Android, Windows: DeviceType.UWP, platform: deviceInfoService.Type)).ToString());
        }

        public TokenHttpRequestMessage(object requestObject)
            : this()
        {
            var stringContent = JsonConvert.SerializeObject(requestObject);
            Content = new StringContent(stringContent, Encoding.UTF8, "application/json");
        }
    }
}
