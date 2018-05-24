using Android.App;
using Android.Content;
using Android.Content.PM;
using Android.Nfc;
using Android.OS;
using Android.Views.Autofill;
using Bit.App.Abstractions;
using Plugin.CurrentActivity;

namespace Bit.Android.Services
{
    public class DeviceInfoService : IDeviceInfoService
    {
        public string Type => Xamarin.Forms.Device.Android;
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
        public bool NfcEnabled => NfcIsEnabled();
        public bool HasCamera => CrossCurrentActivity.Current.Activity.PackageManager.HasSystemFeature(
            PackageManager.FeatureCamera);
        public bool AutofillServiceSupported => AutofillSupported();
        public bool HasFaceIdSupport => false;
        private bool AutofillSupported()
        {
            if(Build.VERSION.SdkInt < BuildVersionCodes.O)
            {
                return false;
            }

            var afm = (AutofillManager)CrossCurrentActivity.Current.Activity.GetSystemService(
                Java.Lang.Class.FromType(typeof(AutofillManager)));
            return afm.IsAutofillSupported;
        }
        public bool NfcIsEnabled()
        {
            var activity = CrossCurrentActivity.Current.Activity;
            var manager = (NfcManager)activity.GetSystemService(Context.NfcService);
            var adapter = manager.DefaultAdapter;
            return adapter != null && adapter.IsEnabled;
        }
    }
}
