using PushNotification.Plugin.Abstractions;

namespace Bit.App.Models.Api
{
    public class DeviceRequest
    {
        public DeviceType Type { get; set; }
        public string Name { get; set; }
        public string PushToken { get; set; }
    }
}
