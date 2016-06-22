using Bit.App.Abstractions;
using Plugin.DeviceInfo.Abstractions;
using PushNotification.Plugin.Abstractions;

namespace Bit.App.Models.Api
{
    public class DeviceRequest
    {
        public DeviceRequest() { }

        public DeviceRequest(IAppIdService appIdService, IDeviceInfo deviceInfo)
        {
            Identifier = appIdService.AppId;
            Name = deviceInfo.Model;
            Type = deviceInfo.Platform == Platform.Android ? DeviceType.Android : DeviceType.iOS;
        }

        public DeviceType Type { get; set; }
        public string Name { get; set; }
        public string Identifier { get; set; }
        public string PushToken { get; set; }
    }
}
