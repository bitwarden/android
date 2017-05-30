using Bit.App.Abstractions;
using PushNotification.Plugin.Abstractions;
using Xamarin.Forms;

namespace Bit.App.Models.Api
{
    public class DeviceRequest
    {
        public DeviceRequest() { }

        public DeviceRequest(IAppIdService appIdService, IDeviceInfoService deviceInfoService)
        {
            Identifier = appIdService.AppId;
            Name = deviceInfoService.Model;
            Type = Device.RuntimePlatform == Device.Android ? DeviceType.Android : DeviceType.iOS;
        }

        public DeviceType Type { get; set; }
        public string Name { get; set; }
        public string Identifier { get; set; }
        public string PushToken { get; set; }
    }
}
