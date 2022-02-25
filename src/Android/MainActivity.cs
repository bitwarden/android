using System;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Android.App;
using Android.Content;
using Android.Content.PM;
using Android.Nfc;
using Android.OS;
using Android.Runtime;
using AndroidX.Core.Content;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Bit.Droid.Receivers;
using Bit.Droid.Utilities;
using ZXing.Net.Mobile.Android;

namespace Bit.Droid
{
    // Activity and IntentFilter declarations have been moved to Properties/AndroidManifest.xml
    // They have been hardcoded so we can use the default LaunchMode on Android 11+
    // LaunchMode defined in values/manifest.xml for Android 10- and values-v30/manifest.xml for Android 11+
    // See https://github.com/bitwarden/mobile/pull/1673 for details
    [Register("com.x8bit.bitwarden.MainActivity")]
    public class MainActivity : Xamarin.Forms.Platform.Android.FormsAppCompatActivity
    {
        private IDeviceActionService _deviceActionService;
        private IMessagingService _messagingService;
        private IBroadcasterService _broadcasterService;
        private IStateService _stateService;
        private IAppIdService _appIdService;
        private IEventService _eventService;
        private PendingIntent _eventUploadPendingIntent;
        private AppOptions _appOptions;
        private string _activityKey = $"{nameof(MainActivity)}_{Java.Lang.JavaSystem.CurrentTimeMillis().ToString()}";
        private Java.Util.Regex.Pattern _otpPattern =
            Java.Util.Regex.Pattern.Compile("^.*?([cbdefghijklnrtuv]{32,64})$");

        protected override void OnCreate(Bundle savedInstanceState)
        {
            var eventUploadIntent = new Intent(this, typeof(EventUploadReceiver));
            _eventUploadPendingIntent = PendingIntent.GetBroadcast(this, 0, eventUploadIntent,
                PendingIntentFlags.UpdateCurrent);

            var policy = new StrictMode.ThreadPolicy.Builder().PermitAll().Build();
            StrictMode.SetThreadPolicy(policy);

            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _appIdService = ServiceContainer.Resolve<IAppIdService>("appIdService");
            _eventService = ServiceContainer.Resolve<IEventService>("eventService");

            TabLayoutResource = Resource.Layout.Tabbar;
            ToolbarResource = Resource.Layout.Toolbar;

            // this needs to be called here before base.OnCreate(...)
            Intent?.Validate();

            base.OnCreate(savedInstanceState);
            if (!CoreHelpers.InDebugMode())
            {
                Window.AddFlags(Android.Views.WindowManagerFlags.Secure);
            }

#if !FDROID
            var appCenterHelper = new AppCenterHelper(_appIdService, _stateService);
            var appCenterTask = appCenterHelper.InitAsync();
#endif

            Xamarin.Essentials.Platform.Init(this, savedInstanceState);
            Xamarin.Forms.Forms.Init(this, savedInstanceState);
            _appOptions = GetOptions();
            LoadApplication(new App.App(_appOptions));

            _broadcasterService.Subscribe(_activityKey, (message) =>
            {
                if (message.Command == "startEventTimer")
                {
                    StartEventAlarm();
                }
                else if (message.Command == "stopEventTimer")
                {
                    var task = StopEventAlarmAsync();
                }
                else if (message.Command == "finishMainActivity")
                {
                    Xamarin.Forms.Device.BeginInvokeOnMainThread(() => Finish());
                }
                else if (message.Command == "listenYubiKeyOTP")
                {
                    ListenYubiKey((bool)message.Data);
                }
                else if (message.Command == "updatedTheme")
                {
                    Xamarin.Forms.Device.BeginInvokeOnMainThread(() => AppearanceAdjustments());
                }
                else if (message.Command == "exit")
                {
                    ExitApp();
                }
            });
        }

        protected override void OnPause()
        {
            base.OnPause();
            ListenYubiKey(false);
        }

        protected override void OnResume()
        {
            base.OnResume();
            Xamarin.Essentials.Platform.OnResume();
            AppearanceAdjustments();

            ThemeManager.UpdateThemeOnPagesAsync();

            if (_deviceActionService.SupportsNfc())
            {
                try
                {
                    _messagingService.Send("resumeYubiKey");
                }
                catch { }
            }
            AndroidHelpers.SetPreconfiguredRestrictionSettingsAsync(this)
                .GetAwaiter()
                .GetResult();
        }

        protected override void OnNewIntent(Intent intent)
        {
            base.OnNewIntent(intent);
            try
            {
                if (intent?.GetStringExtra("uri") is string uri)
                {
                    _messagingService.Send("popAllAndGoToAutofillCiphers");
                    if (_appOptions != null)
                    {
                       _appOptions.Uri = uri;
                    }
                }
                else if (intent.GetBooleanExtra("generatorTile", false))
                {
                    _messagingService.Send("popAllAndGoToTabGenerator");
                    if (_appOptions != null)
                    {
                        _appOptions.GeneratorTile = true;
                    }
                }
                else if (intent.GetBooleanExtra("myVaultTile", false))
                {
                    _messagingService.Send("popAllAndGoToTabMyVault");
                    if (_appOptions != null)
                    {
                        _appOptions.MyVaultTile = true;
                    }
                }
                else if (intent.Action == Intent.ActionSend && intent.Type != null)
                {
                    if (_appOptions != null)
                    {
                        _appOptions.CreateSend = GetCreateSendRequest(intent);
                    }
                    _messagingService.Send("popAllAndGoToTabSend");
                }
                else
                {
                    ParseYubiKey(intent.DataString);
                }
            }
            catch (Exception e)
            {
                System.Diagnostics.Debug.WriteLine(">>> {0}: {1}", e.GetType(), e.StackTrace);
            }
        }

        public async override void OnRequestPermissionsResult(int requestCode, string[] permissions,
            [GeneratedEnum] Permission[] grantResults)
        {
            if (requestCode == Constants.SelectFilePermissionRequestCode)
            {
                if (grantResults.Any(r => r != Permission.Granted))
                {
                    _messagingService.Send("selectFileCameraPermissionDenied");
                }
                await _deviceActionService.SelectFileAsync();
            }
            else
            {
                Xamarin.Essentials.Platform.OnRequestPermissionsResult(requestCode, permissions, grantResults);
                PermissionsHandler.OnRequestPermissionsResult(requestCode, permissions, grantResults);
            }
            base.OnRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        protected override void OnActivityResult(int requestCode, Result resultCode, Intent data)
        {
            if (resultCode == Result.Ok &&
               (requestCode == Constants.SelectFileRequestCode || requestCode == Constants.SaveFileRequestCode))
            {
                Android.Net.Uri uri = null;
                string fileName = null;
                if (data != null && data.Data != null)
                {
                    uri = data.Data;
                    fileName = AndroidHelpers.GetFileName(ApplicationContext, uri);
                }
                else
                {
                    // camera
                    var file = new Java.IO.File(FilesDir, "temp_camera_photo.jpg");
                    uri = FileProvider.GetUriForFile(this, "com.x8bit.bitwarden.fileprovider", file);
                    fileName = $"photo_{DateTime.UtcNow.ToString("yyyyMMddHHmmss")}.jpg";
                }

                if (uri == null)
                {
                    return;
                }

                if (requestCode == Constants.SaveFileRequestCode)
                {
                    _messagingService.Send("selectSaveFileResult",
                        new Tuple<string, string>(uri.ToString(), fileName));
                    return;
                }
                
                try
                {
                    using (var stream = ContentResolver.OpenInputStream(uri))
                    using (var memoryStream = new MemoryStream())
                    {
                        stream.CopyTo(memoryStream);
                        _messagingService.Send("selectFileResult",
                            new Tuple<byte[], string>(memoryStream.ToArray(), fileName ?? "unknown_file_name"));
                    }
                }
                catch (Java.IO.FileNotFoundException)
                {
                    return;
                }
            }
        }

        protected override void OnDestroy()
        {
            base.OnDestroy();
            _broadcasterService.Unsubscribe(_activityKey);
        }

        private void ListenYubiKey(bool listen)
        {
            if (!_deviceActionService.SupportsNfc())
            {
                return;
            }
            var adapter = NfcAdapter.GetDefaultAdapter(this);
            if (listen)
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
                try
                {
                    adapter.DisableForegroundDispatch(this);
                }
                catch { }
            }
        }

        private AppOptions GetOptions()
        {
            var options = new AppOptions
            {
                Uri = Intent.GetStringExtra("uri") ?? Intent.GetStringExtra("autofillFrameworkUri"),
                MyVaultTile = Intent.GetBooleanExtra("myVaultTile", false),
                GeneratorTile = Intent.GetBooleanExtra("generatorTile", false),
                FromAutofillFramework = Intent.GetBooleanExtra("autofillFramework", false),
                CreateSend = GetCreateSendRequest(Intent)
            };
            var fillType = Intent.GetIntExtra("autofillFrameworkFillType", 0);
            if (fillType > 0)
            {
                options.FillType = (CipherType)fillType;
            }
            if (Intent.GetBooleanExtra("autofillFrameworkSave", false))
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

        private Tuple<SendType, string, byte[], string> GetCreateSendRequest(Intent intent)
        {
            if (intent.Action == Intent.ActionSend && intent.Type != null)
            {
                if ((intent.Flags & ActivityFlags.LaunchedFromHistory) == ActivityFlags.LaunchedFromHistory)
                {
                    // don't re-deliver intent if resuming from app switcher
                    return null;
                }
                var type = intent.Type;
                if (type.Contains("text/"))
                {
                    var subject = intent.GetStringExtra(Intent.ExtraSubject);
                    var text = intent.GetStringExtra(Intent.ExtraText);
                    return new Tuple<SendType, string, byte[], string>(SendType.Text, subject, null, text);
                }
                else
                {
                    var data = intent.ClipData?.GetItemAt(0);
                    var uri = data?.Uri;
                    var filename = AndroidHelpers.GetFileName(ApplicationContext, uri);
                    try
                    {
                        using (var stream = ContentResolver.OpenInputStream(uri))
                        using (var memoryStream = new MemoryStream())
                        {
                            stream.CopyTo(memoryStream);
                            return new Tuple<SendType, string, byte[], string>(SendType.File, filename, memoryStream.ToArray(), null);
                        }
                    }
                    catch (Java.IO.FileNotFoundException) { }
                }
            }
            return null;
        }

        private void ParseYubiKey(string data)
        {
            if (data == null)
            {
                return;
            }
            var otpMatch = _otpPattern.Matcher(data);
            if (otpMatch.Matches())
            {
                var otp = otpMatch.Group(1);
                _messagingService.Send("gotYubiKeyOTP", otp);
            }
        }

        private void AppearanceAdjustments()
        {
            Window?.SetStatusBarColor(ThemeHelpers.NavBarBackgroundColor);
            Window?.DecorView.SetBackgroundColor(ThemeHelpers.BackgroundColor);
            ThemeHelpers.SetAppearance(ThemeManager.GetTheme(), ThemeManager.OsDarkModeEnabled());
        }

        private void ExitApp()
        {
            FinishAffinity();
            Java.Lang.JavaSystem.Exit(0);
        }

        private void StartEventAlarm()
        {
            var alarmManager = GetSystemService(AlarmService) as AlarmManager;
            alarmManager.SetInexactRepeating(AlarmType.ElapsedRealtime, 120000, 300000, _eventUploadPendingIntent);
        }

        private async Task StopEventAlarmAsync()
        {
            var alarmManager = GetSystemService(AlarmService) as AlarmManager;
            alarmManager.Cancel(_eventUploadPendingIntent);
            await _eventService.UploadEventsAsync();
        }
    }
}
