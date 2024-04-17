using System;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Android.App;
using Android.Content;
using Android.Content.PM;
using Android.Content.Res;
using Android.Nfc;
using Android.OS;
using Android.Runtime;
using Android.Views;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.Core.Resources.Localization;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Bit.Droid.Autofill;
using Bit.Droid.Receivers;
using Bit.App.Droid.Utilities;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using FileProvider = AndroidX.Core.Content.FileProvider;
using Bit.Core.Utilities.Fido2;

namespace Bit.Droid
{
    // Activity and IntentFilter declarations have been moved to Properties/AndroidManifest.xml
    // They have been hardcoded so we can use the default LaunchMode on Android 11+
    // LaunchMode defined in values/manifest.xml for Android 10- and values-v30/manifest.xml for Android 11+
    // See https://github.com/bitwarden/mobile/pull/1673 for details
    [Register("com.x8bit.bitwarden.MainActivity")]
    public class MainActivity : MauiAppCompatActivity
    {
        private IDeviceActionService _deviceActionService;
        private IFileService _fileService;        
        private IMessagingService _messagingService;
        private IBroadcasterService _broadcasterService;
        private IStateService _stateService;
        private IAppIdService _appIdService;
        private IEventService _eventService;
        private IPushNotificationListenerService _pushNotificationListenerService;
        private ILogger _logger;
        private PendingIntent _eventUploadPendingIntent;
        private AppOptions _appOptions;
        private string _activityKey = $"{nameof(MainActivity)}_{Java.Lang.JavaSystem.CurrentTimeMillis().ToString()}";
        private Java.Util.Regex.Pattern _otpPattern =
            Java.Util.Regex.Pattern.Compile("^.*?([cbdefghijklnrtuv]{32,64})$");

        protected override void OnCreate(Bundle savedInstanceState)
        {
            var eventUploadIntent = new Intent(this, typeof(EventUploadReceiver));
            _eventUploadPendingIntent = PendingIntent.GetBroadcast(this, 0, eventUploadIntent,
                AndroidHelpers.AddPendingIntentMutabilityFlag(PendingIntentFlags.UpdateCurrent, false));

            var policy = new StrictMode.ThreadPolicy.Builder().PermitAll().Build();
            StrictMode.SetThreadPolicy(policy);

            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _fileService = ServiceContainer.Resolve<IFileService>();
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _appIdService = ServiceContainer.Resolve<IAppIdService>("appIdService");
            _eventService = ServiceContainer.Resolve<IEventService>("eventService");
            _pushNotificationListenerService = ServiceContainer.Resolve<IPushNotificationListenerService>();
            _logger = ServiceContainer.Resolve<ILogger>("logger");

            //TabLayoutResource = Resource.Layout.Tabbar;
            //ToolbarResource = Resource.Layout.Toolbar;

            // this needs to be called here before base.OnCreate(...)
            Intent?.Validate();

            //We need to get and set the Options before calling OnCreate as that will "trigger" CreateWindow on App.xaml.cs
            _appOptions = GetOptions();
            //This does not replace existing Options in App.xaml.cs if it exists already. It only updates properties in Options related with Autofill/CreateSend/etc..
            ((Bit.App.App)Microsoft.Maui.Controls.Application.Current).SetAndroidOptions(_appOptions);

            base.OnCreate(savedInstanceState);

            _deviceActionService.SetScreenCaptureAllowedAsync().FireAndForget(_ =>
            {
                Window.AddFlags(Android.Views.WindowManagerFlags.Secure);
            });

            _logger.InitAsync();

            var toplayout = Window?.DecorView?.RootView;
            if (toplayout != null)
            {
                toplayout.FilterTouchesWhenObscured = true;
            }

            CreateNotificationChannel();
            DisableAndroidFontScale();

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
                    MainThread.BeginInvokeOnMainThread(() => Finish());
                }
                else if (message.Command == "listenYubiKeyOTP")
                {
                    ListenYubiKey((bool)message.Data);
                }
                else if (message.Command is ThemeManager.UPDATED_THEME_MESSAGE_KEY)
                {
                    MainThread.BeginInvokeOnMainThread(() => AppearanceAdjustments());
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
            //Xamarin.Essentials.Platform.OnResume();
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

            if (Intent?.GetStringExtra(Core.Constants.NotificationData) is string notificationDataJson)
            {
                var notificationType = JToken.Parse(notificationDataJson).SelectToken(Core.Constants.NotificationDataType);
                if (notificationType.ToString() == PasswordlessNotificationData.TYPE)
                {
                    _pushNotificationListenerService.OnNotificationTapped(JsonConvert.DeserializeObject<PasswordlessNotificationData>(notificationDataJson)).FireAndForget();
                }
            }
        }

        protected override void OnNewIntent(Intent intent)
        {
            base.OnNewIntent(intent);
            try
            {
                if (intent?.GetStringExtra(CredentialProviderConstants.Fido2CredentialAction) == CredentialProviderConstants.Fido2CredentialCreate
                    &&
                    _appOptions != null)
                {
                    _appOptions.HasUnlockedInThisTransaction = false;
                }

                if (intent?.GetStringExtra("uri") is string uri)
                {
                    _messagingService.Send(App.App.POP_ALL_AND_GO_TO_AUTOFILL_CIPHERS_MESSAGE);
                    if (_appOptions != null)
                    {
                       _appOptions.Uri = uri;
                    }
                }
                else if (intent.GetBooleanExtra("generatorTile", false))
                {
                    _messagingService.Send(App.App.POP_ALL_AND_GO_TO_TAB_GENERATOR_MESSAGE);
                    if (_appOptions != null)
                    {
                        _appOptions.GeneratorTile = true;
                    }
                }
                else if (intent.GetBooleanExtra("myVaultTile", false))
                {
                    _messagingService.Send(App.App.POP_ALL_AND_GO_TO_TAB_MYVAULT_MESSAGE);
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
                    _messagingService.Send(App.App.POP_ALL_AND_GO_TO_TAB_SEND_MESSAGE);
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
            if (requestCode == Core.Constants.SelectFilePermissionRequestCode)
            {
                if (grantResults.Any(r => r != Permission.Granted))
                {
                    _messagingService.Send("selectFileCameraPermissionDenied");
                }
                await _fileService.SelectFileAsync();
            }
            else
            {
                Platform.OnRequestPermissionsResult(requestCode, permissions, grantResults);
                //Xamarin.Essentials.Platform.OnRequestPermissionsResult(requestCode, permissions, grantResults);
                //PermissionsHandler.OnRequestPermissionsResult(requestCode, permissions, grantResults);
            }
            base.OnRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        protected override void OnActivityResult(int requestCode, Result resultCode, Intent data)
        {
            if (resultCode == Result.Ok &&
               (requestCode == Core.Constants.SelectFileRequestCode || requestCode == Core.Constants.SaveFileRequestCode))
            {
                Android.Net.Uri uri = null;
                string fileName = null;
                if (data != null && data.Data != null)
                {
                    if (data.Data.ToString()?.Contains(Constants.PACKAGE_NAME) != true)
                    {
                        uri = data.Data;
                        fileName = AndroidHelpers.GetFileName(ApplicationContext, uri);
                    }
                }
                else
                {
                    // camera
                    var tmpDir = new Java.IO.File(FilesDir, Constants.TEMP_CAMERA_IMAGE_DIR);
                    var file = new Java.IO.File(tmpDir, Constants.TEMP_CAMERA_IMAGE_NAME);
                    uri = FileProvider.GetUriForFile(this, "com.x8bit.bitwarden.fileprovider", file);
                    fileName = $"photo_{DateTime.UtcNow.ToString("yyyyMMddHHmmss")}.jpg";
                }

                if (uri == null || fileName == null)
                {
                    return;
                }

                if (requestCode == Core.Constants.SaveFileRequestCode)
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
                var pendingIntent = PendingIntent.GetActivity(this, 0, intent, AndroidHelpers.AddPendingIntentMutabilityFlag(0, true));
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
            var fido2CredentialAction = Intent.GetStringExtra(CredentialProviderConstants.Fido2CredentialAction);
            var options = new AppOptions
            {
                Uri = Intent.GetStringExtra("uri") ?? Intent.GetStringExtra(AutofillConstants.AutofillFrameworkUri),
                MyVaultTile = Intent.GetBooleanExtra("myVaultTile", false),
                GeneratorTile = Intent.GetBooleanExtra("generatorTile", false),
                FromAutofillFramework = Intent.GetBooleanExtra(AutofillConstants.AutofillFramework, false),
                Fido2CredentialAction = fido2CredentialAction,
                FromFido2Framework = !string.IsNullOrWhiteSpace(fido2CredentialAction),
                CreateSend = GetCreateSendRequest(Intent)
            };
            var fillType = Intent.GetIntExtra(AutofillConstants.AutofillFrameworkFillType, 0);
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

        private void CreateNotificationChannel()
        {
#if !FDROID
            if (Build.VERSION.SdkInt < BuildVersionCodes.O)
            {
                // Notification channels are new in API 26 (and not a part of the
                // support library). There is no need to create a notification
                // channel on older versions of Android.
                return;
            }

            var channel = new NotificationChannel(Core.Constants.AndroidNotificationChannelId, AppResources.AllNotifications, NotificationImportance.Default);
            if(GetSystemService(NotificationService) is NotificationManager notificationManager)
            {
                notificationManager.CreateNotificationChannel(channel);
            }
#endif
        }

        private void DisableAndroidFontScale()
        {
            try
            {
                //As we are using NamedSizes the xamarin will change the font size. So we are disabling the Android scaling.
                Resources.Configuration.FontScale = 1f;
                BaseContext.Resources.DisplayMetrics.ScaledDensity = Resources.Configuration.FontScale * (float)DeviceDisplay.MainDisplayInfo.Density;
            }
            catch (Exception e)
            {
                _logger.Exception(e);
            }
        }
    }
}
