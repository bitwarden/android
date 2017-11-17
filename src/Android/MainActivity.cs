using System;
using Android.App;
using Android.Content.PM;
using Android.Views;
using Android.OS;
using Bit.App.Abstractions;
using XLabs.Ioc;
using Plugin.Settings.Abstractions;
using Plugin.Connectivity.Abstractions;
using Acr.UserDialogs;
using Android.Content;
using System.Reflection;
using Xamarin.Forms.Platform.Android;
using Xamarin.Forms;
using System.Threading.Tasks;
using Bit.App.Models.Page;
using Bit.App;
using Android.Nfc;
using Android.Views.InputMethods;
using System.IO;
using System.Linq;
using Android.Views.Autofill;
using Android.App.Assist;
using Bit.Android.Autofill;
using System.Collections.Generic;
using Bit.App.Models;
using Bit.App.Enums;

namespace Bit.Android
{
    [Activity(Label = "bitwarden",
        Icon = "@drawable/icon",
        ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation,
        Exported = false)]
    public class MainActivity : FormsAppCompatActivity
    {
        private const string HockeyAppId = "d3834185b4a643479047b86c65293d42";
        private DateTime? _lastAction;
        private Java.Util.Regex.Pattern _otpPattern = Java.Util.Regex.Pattern.Compile("^.*?([cbdefghijklnrtuv]{32,64})$");
        private IDeviceActionService _deviceActionService;
        private ISettings _settings;

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

            HockeyApp.Android.CrashManager.Register(this, HockeyAppId,
                new HockeyAppCrashManagerListener(appIdService, authService));

            Forms.Init(this, bundle);

            typeof(Color).GetProperty("Accent", BindingFlags.Public | BindingFlags.Static)
                .SetValue(null, Color.FromHex("d2d6de"));

            _deviceActionService = Resolver.Resolve<IDeviceActionService>();
            _settings = Resolver.Resolve<ISettings>();
            LoadApplication(new App.App(
                GetOptions(),
                Resolver.Resolve<IAuthService>(),
                Resolver.Resolve<IConnectivity>(),
                Resolver.Resolve<IUserDialogs>(),
                Resolver.Resolve<IDatabaseService>(),
                Resolver.Resolve<ISyncService>(),
                _settings,
                Resolver.Resolve<ILockService>(),
                Resolver.Resolve<IGoogleAnalyticsService>(),
                Resolver.Resolve<ILocalizeService>(),
                Resolver.Resolve<IAppInfoService>(),
                Resolver.Resolve<IAppSettingsService>(),
                _deviceActionService));

            MessagingCenter.Subscribe<Xamarin.Forms.Application>(
                Xamarin.Forms.Application.Current, "DismissKeyboard", (sender) =>
            {
                DismissKeyboard();
            });

            MessagingCenter.Subscribe<Xamarin.Forms.Application>(Xamarin.Forms.Application.Current, "RateApp", (sender) =>
            {
                RateApp();
            });

            MessagingCenter.Subscribe<Xamarin.Forms.Application>(Xamarin.Forms.Application.Current, "Accessibility", (sender) =>
            {
                OpenAccessibilitySettings();
            });

            MessagingCenter.Subscribe<Xamarin.Forms.Application, VaultListPageModel.Cipher>(
                Xamarin.Forms.Application.Current, "Autofill", (sender, args) =>
            {
                ReturnCredentials(args);
            });

            MessagingCenter.Subscribe<Xamarin.Forms.Application>(Xamarin.Forms.Application.Current, "BackgroundApp", (sender) =>
            {
                if(Intent.GetBooleanExtra("autofillFramework", false))
                {
                    SetResult(Result.Canceled);
                    Finish();
                }
                else
                {
                    MoveTaskToBack(true);
                }
            });

            MessagingCenter.Subscribe<Xamarin.Forms.Application, string>(
                Xamarin.Forms.Application.Current, "LaunchApp", (sender, args) =>
            {
                LaunchApp(args);
            });

            MessagingCenter.Subscribe<Xamarin.Forms.Application, bool>(
                Xamarin.Forms.Application.Current, "ListenYubiKeyOTP", (sender, listen) =>
            {
                ListenYubiKey(listen);
            });
        }

        private void ReturnCredentials(VaultListPageModel.Cipher cipher)
        {
            if(Intent.GetBooleanExtra("autofillFramework", false))
            {
                if(cipher == null)
                {
                    SetResult(Result.Canceled);
                    Finish();
                    return;
                }

                var structure = Intent.GetParcelableExtra(AutofillManager.ExtraAssistStructure) as AssistStructure;
                if(structure == null)
                {
                    SetResult(Result.Canceled);
                    Finish();
                    return;
                }

                var parser = new Parser(structure);
                parser.ParseForFill();
                if(!parser.FieldCollection.Fields.Any() || string.IsNullOrWhiteSpace(parser.Uri))
                {
                    SetResult(Result.Canceled);
                    Finish();
                    return;
                }

                var items = new List<IFilledItem> { new CipherFilledItem(cipher) };
                var response = AutofillHelpers.BuildFillResponse(this, parser.FieldCollection, items);
                var replyIntent = new Intent();
                replyIntent.PutExtra(AutofillManager.ExtraAuthenticationResult, response);
                SetResult(Result.Ok, replyIntent);
                Finish();
            }
            else
            {
                var data = new Intent();
                if(cipher == null)
                {
                    data.PutExtra("canceled", "true");
                }
                else
                {
                    var isPremium = Resolver.Resolve<ITokenService>()?.TokenPremium ?? false;
                    var autoCopyEnabled = !_settings.GetValueOrDefault(Constants.SettingDisableTotpCopy, false);
                    if(isPremium && autoCopyEnabled && _deviceActionService != null && cipher.LoginTotp?.Value != null)
                    {
                        _deviceActionService.CopyToClipboard(App.Utilities.Crypto.Totp(cipher.LoginTotp.Value));
                    }

                    data.PutExtra("uri", cipher.LoginUri);
                    data.PutExtra("username", cipher.LoginUsername);
                    data.PutExtra("password", cipher.LoginPassword?.Value ?? null);
                }

                if(Parent == null)
                {
                    SetResult(Result.Ok, data);
                }
                else
                {
                    Parent.SetResult(Result.Ok, data);
                }

                Finish();
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

            if(Utilities.NfcEnabled())
            {
                MessagingCenter.Send(Xamarin.Forms.Application.Current, "ResumeYubiKey");
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

        public void RateApp()
        {
            try
            {
                var rateIntent = RateIntentForUrl("market://details");
                StartActivity(rateIntent);
            }
            catch(ActivityNotFoundException)
            {
                var rateIntent = RateIntentForUrl("https://play.google.com/store/apps/details");
                StartActivity(rateIntent);
            }
        }

        private Intent RateIntentForUrl(string url)
        {
            var intent = new Intent(Intent.ActionView, global::Android.Net.Uri.Parse($"{url}?id={PackageName}"));
            var flags = ActivityFlags.NoHistory | ActivityFlags.MultipleTask;
            if((int)Build.VERSION.SdkInt >= 21)
            {
                flags |= ActivityFlags.NewDocument;
            }
            else
            {
                // noinspection deprecation
                flags |= ActivityFlags.ClearWhenTaskReset;
            }

            intent.AddFlags(flags);
            return intent;
        }

        private void OpenAccessibilitySettings()
        {
            var intent = new Intent(global::Android.Provider.Settings.ActionAccessibilitySettings);
            StartActivity(intent);
        }

        private void LaunchApp(string packageName)
        {
            if(_lastAction.LastActionWasRecent())
            {
                return;
            }
            _lastAction = DateTime.UtcNow;

            packageName = packageName.Replace("androidapp://", string.Empty);
            var launchIntent = PackageManager.GetLaunchIntentForPackage(packageName);
            if(launchIntent == null)
            {
                var dialog = Resolver.Resolve<IUserDialogs>();
                dialog.Alert(string.Format(App.Resources.AppResources.CannotOpenApp, packageName));
            }
            else
            {
                StartActivity(launchIntent);
            }
        }

        private void ListenYubiKey(bool listen)
        {
            if(!Utilities.NfcEnabled())
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

                // register for foreground dispatch so we'll receive tags according to our intent filters
                adapter.EnableForegroundDispatch(this, pendingIntent, filters, null);
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

        private void DismissKeyboard()
        {
            try
            {
                var imm = (InputMethodManager)GetSystemService(InputMethodService);
                imm.HideSoftInputFromWindow(CurrentFocus.WindowToken, 0);
            }
            catch { }
        }

        private AppOptions GetOptions()
        {
            var options = new AppOptions
            {
                Uri = Intent.GetStringExtra("uri") ?? Intent.GetStringExtra("autofillFrameworkUri"),
                MyVault = Intent.GetBooleanExtra("myVaultTile", false),
                FromAutofillFramework = Intent.GetBooleanExtra("autofillFramework", false)
            };

            if(Intent.GetBooleanExtra("autofillFrameworkSave", false))
            {
                options.SaveType = (CipherType)Intent.GetIntExtra("autofillFrameworkType", 0);
                options.SaveUsername = Intent.GetStringExtra("autofillFrameworkUsername");
                options.SavePassword = Intent.GetStringExtra("autofillFrameworkPassword");
            }

            return options;
        }
    }
}
