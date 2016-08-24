using Android.OS;
using Bit.App.Abstractions;

namespace Bit.Android.Services
{
    public class DeviceInfoService : IDeviceInfoService
    {
        public string Model => Build.Model;
        public int Version => (int)Build.VERSION.SdkInt;
    }
}