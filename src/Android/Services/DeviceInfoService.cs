using Android.App;
using Android.Nfc;
using Android.OS;
using Bit.App.Abstractions;

namespace Bit.Android.Services
{
    public class DeviceInfoService : IDeviceInfoService
    {
        public string Model => Build.Model;
        public int Version => (int)Build.VERSION.SdkInt;
        public float Scale
        {
            get
            {
                var density = Application.Context.Resources.DisplayMetrics.Density;
                if(density <= 0.75)
                {
                    return 0.75f;
                }
                else if(density <= 1)
                {
                    return 1f;
                }
                else if(density <= 1.5)
                {
                    return 1.5f;
                }
                else if(density <= 2)
                {
                    return 2f;
                }
                else if(density <= 3)
                {
                    return 3f;
                }
                else if(density <= 4)
                {
                    return 4f;
                }

                return 1f;
            }
        }
        public bool NfcEnabled
        {
            get
            {
                var manager = (NfcManager)Application.Context.GetSystemService("nfc");
                var adapter = manager.DefaultAdapter;
                return adapter != null && adapter.IsEnabled;
            }
        }
    }
}
