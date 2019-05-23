using Android.App;
using Android.Content.PM;
using Android.Runtime;
using Android.OS;
using Bit.Core;
using System.Linq;
using Bit.App.Abstractions;
using Bit.Core.Utilities;
using Bit.Core.Abstractions;
using System.IO;
using System;
using Android.Content;
using Bit.Droid.Utilities;
using Bit.Droid.Receivers;
using Bit.App.Models;
using Bit.Core.Enums;
using Android.Nfc;

namespace Bit.Droid
{
    [Activity(
        Label = "Bitwarden",
        Icon = "@mipmap/ic_launcher",
        Theme = "@style/MainTheme",
        Exported = false,
        ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation)]
    [Register("com.x8bit.bitwarden.MainActivity")]
    public class MainActivity : Xamarin.Forms.Platform.Android.FormsAppCompatActivity
    {
        private IDeviceActionService _deviceActionService;
        private IMessagingService _messagingService;
        private IBroadcasterService _broadcasterService;
        private PendingIntent _lockAlarmPendingIntent;
        private AppOptions _appOptions;

        protected override void OnCreate(Bundle savedInstanceState)
        {
            var alarmIntent = new Intent(this, typeof(LockAlarmReceiver));
            _lockAlarmPendingIntent = PendingIntent.GetBroadcast(this, 0, alarmIntent,
                PendingIntentFlags.UpdateCurrent);

            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");

            TabLayoutResource = Resource.Layout.Tabbar;
            ToolbarResource = Resource.Layout.Toolbar;

            base.OnCreate(savedInstanceState);
            Xamarin.Essentials.Platform.Init(this, savedInstanceState);
            Xamarin.Forms.Forms.Init(this, savedInstanceState);
            _appOptions = GetOptions();
            LoadApplication(new App.App(_appOptions));

            _broadcasterService.Subscribe(nameof(MainActivity), (message) =>
            {
                if(message.Command == "scheduleLockTimer")
                {
                    var lockOptionMs = (int)message.Data * 1000;
                    var triggerMs = Java.Lang.JavaSystem.CurrentTimeMillis() + lockOptionMs + 10;
                    var alarmManager = GetSystemService(AlarmService) as AlarmManager;
                    alarmManager.Set(AlarmType.RtcWakeup, triggerMs, _lockAlarmPendingIntent);
                }
                else if(message.Command == "cancelLockTimer")
                {
                    var alarmManager = GetSystemService(AlarmService) as AlarmManager;
                    alarmManager.Cancel(_lockAlarmPendingIntent);
                }
                else if(message.Command == "finishMainActivity")
                {
                    Finish();
                }
            });
        }

        public async override void OnRequestPermissionsResult(int requestCode, string[] permissions,
            [GeneratedEnum] Permission[] grantResults)
        {
            if(requestCode == Constants.SelectFilePermissionRequestCode)
            {
                if(grantResults.Any(r => r != Permission.Granted))
                {
                    _messagingService.Send("selectFileCameraPermissionDenied");
                }
                await _deviceActionService.SelectFileAsync();
            }
            else
            {
                Xamarin.Essentials.Platform.OnRequestPermissionsResult(requestCode, permissions, grantResults);
                ZXing.Net.Mobile.Forms.Android.PermissionsHandler.OnRequestPermissionsResult(
                    requestCode, permissions, grantResults);
            }
            base.OnRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        protected override void OnActivityResult(int requestCode, Result resultCode, Intent data)
        {
            if(requestCode == Constants.SelectFileRequestCode && resultCode == Result.Ok)
            {
                Android.Net.Uri uri = null;
                string fileName = null;
                if(data != null && data.Data != null)
                {
                    uri = data.Data;
                    fileName = AndroidHelpers.GetFileName(ApplicationContext, uri);
                }
                else
                {
                    // camera
                    var root = new Java.IO.File(Android.OS.Environment.ExternalStorageDirectory, "bitwarden");
                    var file = new Java.IO.File(root, "temp_camera_photo.jpg");
                    uri = Android.Net.Uri.FromFile(file);
                    fileName = $"photo_{DateTime.UtcNow.ToString("yyyyMMddHHmmss")}.jpg";
                }

                if(uri == null)
                {
                    return;
                }
                try
                {
                    using(var stream = ContentResolver.OpenInputStream(uri))
                    using(var memoryStream = new MemoryStream())
                    {
                        stream.CopyTo(memoryStream);
                        _messagingService.Send("selectFileResult",
                            new Tuple<byte[], string>(memoryStream.ToArray(), fileName ?? "unknown_file_name"));
                    }
                }
                catch(Java.IO.FileNotFoundException)
                {
                    return;
                }
            }
        }

        private void ListenYubiKey(bool listen)
        {
            if(!_deviceActionService.SupportsNfc())
            {
                return;
            }
            var adapter = NfcAdapter.GetDefaultAdapter(this);
            if(listen)
            {
                var intent = new Intent(this, Class);
                intent.AddFlags(ActivityFlags.SingleTop);
                var pendingIntent = PendingIntent.GetActivity(this, 0, intent, 0);
                // register for all NDEF tags starting with http och https
                var ndef = new IntentFilter(NfcAdapter.ActionNdefDiscovered);
                ndef.AddDataScheme("http");
                ndef.AddDataScheme("https");
                var filters = new IntentFilter[] { ndef };
                try
                {
                    // register for foreground dispatch so we'll receive tags according to our intent filters
                    adapter.EnableForegroundDispatch(this, pendingIntent, filters, null);
                }
                catch { }
            }
            else
            {
                adapter.DisableForegroundDispatch(this);
            }
        }

        private AppOptions GetOptions()
        {
            var options = new AppOptions
            {
                Uri = Intent.GetStringExtra("uri") ?? Intent.GetStringExtra("autofillFrameworkUri"),
                MyVaultTile = Intent.GetBooleanExtra("myVaultTile", false),
                FromAutofillFramework = Intent.GetBooleanExtra("autofillFramework", false)
            };
            var fillType = Intent.GetIntExtra("autofillFrameworkFillType", 0);
            if(fillType > 0)
            {
                options.FillType = (CipherType)fillType;
            }
            if(Intent.GetBooleanExtra("autofillFrameworkSave", false))
            {
                options.SaveType = (CipherType)Intent.GetIntExtra("autofillFrameworkType", 0);
                options.SaveName = Intent.GetStringExtra("autofillFrameworkName");
                options.SaveUsername = Intent.GetStringExtra("autofillFrameworkUsername");
                options.SavePassword = Intent.GetStringExtra("autofillFrameworkPassword");
                options.SaveCardName = Intent.GetStringExtra("autofillFrameworkCardName");
                options.SaveCardNumber = Intent.GetStringExtra("autofillFrameworkCardNumber");
                options.SaveCardExpMonth = Intent.GetStringExtra("autofillFrameworkCardExpMonth");
                options.SaveCardExpYear = Intent.GetStringExtra("autofillFrameworkCardExpYear");
                options.SaveCardCode = Intent.GetStringExtra("autofillFrameworkCardCode");
            }
            return options;
        }
    }
}
