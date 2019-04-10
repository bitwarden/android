using Bit.Core.Abstractions;
using Bit.Core.Enums;

namespace Bit.Core.Models.Request
{
    public class DeviceRequest
    {
        public DeviceRequest(string appId, IPlatformUtilsService platformUtilsService)
        {
            Type = platformUtilsService.GetDevice();
            Name = platformUtilsService.GetDeviceString();
            Identifier = appId;
            PushToken = null; // TODO?
        }

        public DeviceType? Type { get; set; }
        public string Name { get; set; }
        public string Identifier { get; set; }
        public string PushToken { get; set; }
    }
}
