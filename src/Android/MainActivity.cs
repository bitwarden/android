using System;
using Android.App;
using Android.Content.PM;
using Android.Views;
using Android.OS;
using Bit.App.Abstractions;
using XLabs.Ioc;
using Plugin.Settings.Abstractions;
using Plugin.Connectivity.Abstractions;
using Android.Content;
using System.Reflection;
using Xamarin.Forms.Platform.Android;
using Xamarin.Forms;
using System.Threading.Tasks;
using Bit.App;
using Android.Nfc;
using System.IO;
using System.Linq;
using Bit.App.Models;
using Bit.App.Enums;

namespace Bit.Android
{
    [Activity(ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation, Exported = false)]
    public class MainActivity : FormsAppCompatActivity
    {
        private const string HockeyAppId = "d3834185b4a643479047b86c65293d42";
        private Java.Util.Regex.Pattern _otpPattern = Java.Util.Regex.Pattern.Compile("^.*?([cbdefghijklnrtuv]{32,64})$");
        private IDeviceActionService _deviceActionService;
        private IDeviceInfoService _deviceInfoService;
        private IAppSettingsService _appSettingsService;
        private ISettings _settings;
        private AppOptions _appOptions;

        protected override void OnCreate(Bundle bundle)
        {
            if(!Resolver.IsSet)
            {
                MainApplication.SetIoc(Application);
            }

            var policy = new StrictMode.ThreadPolicy.Builder().PermitAll().Build();
            StrictMode.SetThreadPolicy(policy);

            ToolbarResource = Resource.Layout.toolbar;
            TabLayoutResource = Resource.Layout.tabs;

            base.OnCreate(bundle);

            // workaround for app compat bug
            // ref https://forums.xamarin.com/discussion/62414/app-resuming-results-in-crash-with-formsappcompatactivity
            Task.Delay(10).Wait();

            Console.WriteLine("A OnCreate");
            if(!App.Utilities.Helpers.InDebugMode())
            {
                Window.AddFlags(WindowManagerFlags.Secure);
            }

            var appIdService = Resolver.Resolve<IAppIdService>();
            var authService = Resolver.Resolve<IAuthService>();

#if !FDROID
            HockeyApp.Android.CrashManager.Register(this, HockeyAppId,
                new HockeyAppCrashManagerListener(appIdService, authService));
#endif

            Forms.Init(this, bundle);

            typeof(Color).GetProperty("Accent", BindingFlags.Public | BindingFlags.Static)
                .SetValue(null, Color.FromHex("d2d6de"));

            _deviceActionService = Resolver.Resolve<IDeviceActionService>();
            _deviceInfoService = Resolver.Resolve<IDeviceInfoService>();
            _appSettingsService = Resolver.Resolve<IAppSettingsService>();
            _settings = Resolver.Resolve<ISettings>();
            _appOptions = GetOptions();
            LoadApplication(new App.App(
                _appOptions,
                Resolver.Resolve<IAuthService>(),
                Resolver.Resolve<IConnectivity>(),
                Resolver.Resolve<IDatabaseService>(),
                Resolver.Resolve<ISyncService>(),
                _settings,
                Resolver.Resolve<ILockService>(),
                Resolver.Resolve<ILocalizeService>(),
                Resolver.Resolve<IAppInfoService>(),
                _appSettingsService,
                _deviceActionService));

            if(_appOptions?.Uri == null)
            {
                MessagingCenter.Subscribe<Xamarin.Forms.Application, bool>(Xamarin.Forms.Application.Current,
                    "ListenYubiKeyOTP", (sender, listen) => ListenYubiKey(listen));

                MessagingCenter.Subscribe<Xamarin.Forms.Application>(Xamarin.Forms.Application.Current,
                    "FinishMainActivity", (sender) => Finish());
            }
        }

        protected override void OnPause()
        {
            Console.WriteLine("A OnPause");
            base.OnPause();
            ListenYubiKey(false);
        }

        protected override void OnDestroy()
        {
            Console.WriteLine("A OnDestroy");
            base.OnDestroy();
        }

        protected override void OnRestart()
        {
            Console.WriteLine("A OnRestart");
            base.OnRestart();
        }

        protected override void OnStart()
        {
            Console.WriteLine("A OnStart");
            base.OnStart();
        }

        protected override void OnStop()
        {
            Console.WriteLine("A OnStop");
            base.OnStop();
        }

        protected override void OnResume()
        {
            base.OnResume();
            Console.WriteLine("A OnResume");

            // workaround for app compat bug
            // ref https://bugzilla.xamarin.com/show_bug.cgi?id=36907
            Task.Delay(10).Wait();

            if(_deviceInfoService.NfcEnabled)
            {
                try
                {
                    MessagingCenter.Send(Xamarin.Forms.Application.Current, "ResumeYubiKey");
                }
                catch(Exception e)
                {
                    System.Diagnostics.Debug.WriteLine(e);
                }
            }

            if(_appSettingsService.Locked)
            {
                MessagingCenter.Send(Xamarin.Forms.Application.Current, "Resumed", false);
            }
        }

        protected override void OnNewIntent(Intent intent)
        {
            base.OnNewIntent(intent);
            Console.WriteLine("A OnNewIntent");
            ParseYubiKey(intent.DataString);
        }

        public async override void OnRequestPermissionsResult(int requestCode, string[] permissions, Permission[] grantResults)
        {
            if(requestCode == Constants.SelectFilePermissionRequestCode)
            {
                if(grantResults.Any(r => r != Permission.Granted))
                {
                    MessagingCenter.Send(Xamarin.Forms.Application.Current, "SelectFileCameraPermissionDenied");
                }
                await _deviceActionService.SelectFileAsync();
                return;
            }

            ZXing.Net.Mobile.Forms.Android.PermissionsHandler.OnRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        protected override void OnActivityResult(int requestCode, Result resultCode, Intent data)
        {
            if(requestCode == Constants.SelectFileRequestCode && resultCode == Result.Ok)
            {
                global::Android.Net.Uri uri = null;
                string fileName = null;
                if(data != null && data.Data != null)
                {
                    uri = data.Data;
                    fileName = Utilities.GetFileName(ApplicationContext, uri);
                }
                else
                {
                    // camera
                    var root = new Java.IO.File(global::Android.OS.Environment.ExternalStorageDirectory, "bitwarden");
                    var file = new Java.IO.File(root, "temp_camera_photo.jpg");
                    uri = global::Android.Net.Uri.FromFile(file);
                    fileName = $"photo_{DateTime.UtcNow.ToString("yyyyMMddHHmmss")}.jpg";
                }

                if(uri == null)
                {
                    return;
                }

                using(var stream = ContentResolver.OpenInputStream(uri))
                using(var memoryStream = new MemoryStream())
                {
                    stream.CopyTo(memoryStream);
                    MessagingCenter.Send(Xamarin.Forms.Application.Current, "SelectFileResult",
                        new Tuple<byte[], string>(memoryStream.ToArray(), fileName ?? "unknown_file_name"));
                }
            }
        }

        private void ListenYubiKey(bool listen)
        {
            if(!_deviceInfoService.NfcEnabled)
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

        private void ParseYubiKey(string data)
        {
            if(data == null)
            {
                return;
            }

            var otpMatch = _otpPattern.Matcher(data);
            if(otpMatch.Matches())
            {
                var otp = otpMatch.Group(1);
                MessagingCenter.Send(Xamarin.Forms.Application.Current, "GotYubiKeyOTP", otp);
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
