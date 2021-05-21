using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Android;
using Android.App;
using Android.App.Assist;
using Android.Content;
using Android.Content.PM;
using Android.Content.Res;
using Android.Nfc;
using Android.OS;
using Android.Provider;
using Android.Text;
using Android.Text.Method;
using Android.Views.Autofill;
using Android.Views.InputMethods;
using Android.Webkit;
using Android.Widget;
using AndroidX.Core.App;
using AndroidX.Core.Content;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Bit.Droid.Autofill;
using Plugin.CurrentActivity;

namespace Bit.Droid.Services
{
    public class DeviceActionService : IDeviceActionService
    {
        private readonly IStorageService _storageService;
        private readonly IMessagingService _messagingService;
        private readonly IBroadcasterService _broadcasterService;
        private readonly Func<IEventService> _eventServiceFunc;
        private ProgressDialog _progressDialog;
        private bool _cameraPermissionsDenied;
        private Toast _toast;
        private string _userAgent;

        public DeviceActionService(
            IStorageService storageService,
            IMessagingService messagingService,
            IBroadcasterService broadcasterService,
            Func<IEventService> eventServiceFunc)
        {
            _storageService = storageService;
            _messagingService = messagingService;
            _broadcasterService = broadcasterService;
            _eventServiceFunc = eventServiceFunc;

            _broadcasterService.Subscribe(nameof(DeviceActionService), (message) =>
            {
                if (message.Command == "selectFileCameraPermissionDenied")
                {
                    _cameraPermissionsDenied = true;
                }
            });
        }

        public string DeviceUserAgent
        {
            get
            {
                if (string.IsNullOrWhiteSpace(_userAgent))
                {
                    _userAgent = $"Bitwarden_Mobile/{Xamarin.Essentials.AppInfo.VersionString} " +
                        $"(Android {Build.VERSION.Release}; SDK {Build.VERSION.Sdk}; Model {Build.Model})";
                }
                return _userAgent;
            }
        }

        public DeviceType DeviceType => DeviceType.Android;

        public void Toast(string text, bool longDuration = false)
        {
            if (_toast != null)
            {
                _toast.Cancel();
                _toast.Dispose();
                _toast = null;
            }
            _toast = Android.Widget.Toast.MakeText(CrossCurrentActivity.Current.Activity, text,
                longDuration ? ToastLength.Long : ToastLength.Short);
            _toast.Show();
        }

        public bool LaunchApp(string appName)
        {
            var activity = CrossCurrentActivity.Current.Activity;
            appName = appName.Replace("androidapp://", string.Empty);
            var launchIntent = activity.PackageManager.GetLaunchIntentForPackage(appName);
            if (launchIntent != null)
            {
                activity.StartActivity(launchIntent);
            }
            return launchIntent != null;
        }

        public async Task ShowLoadingAsync(string text)
        {
            if (_progressDialog != null)
            {
                await HideLoadingAsync();
            }
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            _progressDialog = new ProgressDialog(activity);
            _progressDialog.SetMessage(text);
            _progressDialog.SetCancelable(false);
            _progressDialog.Show();
        }

        public Task HideLoadingAsync()
        {
            if (_progressDialog != null)
            {
                _progressDialog.Dismiss();
                _progressDialog.Dispose();
                _progressDialog = null;
            }
            return Task.FromResult(0);
        }

        public bool OpenFile(byte[] fileData, string id, string fileName)
        {
            try
            {
                var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
                var intent = BuildOpenFileIntent(fileData, fileName);
                if (intent == null)
                {
                    return false;
                }
                activity.StartActivity(intent);
                return true;
            }
            catch { }
            return false;
        }

        public bool CanOpenFile(string fileName)
        {
            try
            {
                var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
                var intent = BuildOpenFileIntent(new byte[0], string.Concat("opentest_", fileName));
                if (intent == null)
                {
                    return false;
                }
                var activities = activity.PackageManager.QueryIntentActivities(intent,
                    PackageInfoFlags.MatchDefaultOnly);
                return (activities?.Count ?? 0) > 0;
            }
            catch { }
            return false;
        }

        private Intent BuildOpenFileIntent(byte[] fileData, string fileName)
        {
            var extension = MimeTypeMap.GetFileExtensionFromUrl(fileName.Replace(' ', '_').ToLower());
            if (extension == null)
            {
                return null;
            }
            var mimeType = MimeTypeMap.Singleton.GetMimeTypeFromExtension(extension);
            if (mimeType == null)
            {
                return null;
            }

            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            var cachePath = activity.CacheDir;
            var filePath = Path.Combine(cachePath.Path, fileName);
            File.WriteAllBytes(filePath, fileData);
            var file = new Java.IO.File(cachePath, fileName);
            if (!file.IsFile)
            {
                return null;
            }

            try
            {
                var intent = new Intent(Intent.ActionView);
                var uri = FileProvider.GetUriForFile(activity.ApplicationContext,
                    "com.x8bit.bitwarden.fileprovider", file);
                intent.SetDataAndType(uri, mimeType);
                intent.SetFlags(ActivityFlags.GrantReadUriPermission);
                return intent;
            }
            catch { }
            return null;
        }

        public bool SaveFile(byte[] fileData, string id, string fileName, string contentUri)
        {
            try
            {
                var activity = (MainActivity)CrossCurrentActivity.Current.Activity;

                if (contentUri != null)
                {
                    var uri = Android.Net.Uri.Parse(contentUri);
                    var stream = activity.ContentResolver.OpenOutputStream(uri);
                    // Using java bufferedOutputStream due to this issue:
                    // https://github.com/xamarin/xamarin-android/issues/3498
                    var javaStream = new Java.IO.BufferedOutputStream(stream);
                    javaStream.Write(fileData);
                    javaStream.Flush();
                    javaStream.Close();
                    return true;
                }

                // Prompt for location to save file
                var extension = MimeTypeMap.GetFileExtensionFromUrl(fileName.Replace(' ', '_').ToLower());
                if (extension == null)
                {
                    return false;
                }

                string mimeType = MimeTypeMap.Singleton.GetMimeTypeFromExtension(extension);
                if (mimeType == null)
                {
                    // Unable to identify so fall back to generic "any" type
                    mimeType = "*/*";
                }

                var intent = new Intent(Intent.ActionCreateDocument);
                intent.SetType(mimeType);
                intent.AddCategory(Intent.CategoryOpenable);
                intent.PutExtra(Intent.ExtraTitle, fileName);

                activity.StartActivityForResult(intent, Constants.SaveFileRequestCode);
                return true;
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine(">>> {0}: {1}", ex.GetType(), ex.StackTrace);
            }
            return false;
        }

        public async Task ClearCacheAsync()
        {
            try
            {
                DeleteDir(CrossCurrentActivity.Current.Activity.CacheDir);
                await _storageService.SaveAsync(Constants.LastFileCacheClearKey, DateTime.UtcNow);
            }
            catch (Exception) { }
        }

        public Task SelectFileAsync()
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            var hasStorageWritePermission = !_cameraPermissionsDenied &&
                HasPermission(Manifest.Permission.WriteExternalStorage);
            var additionalIntents = new List<IParcelable>();
            if (activity.PackageManager.HasSystemFeature(PackageManager.FeatureCamera))
            {
                var hasCameraPermission = !_cameraPermissionsDenied && HasPermission(Manifest.Permission.Camera);
                if (!_cameraPermissionsDenied && !hasStorageWritePermission)
                {
                    AskPermission(Manifest.Permission.WriteExternalStorage);
                    return Task.FromResult(0);
                }
                if (!_cameraPermissionsDenied && !hasCameraPermission)
                {
                    AskPermission(Manifest.Permission.Camera);
                    return Task.FromResult(0);
                }
                if (!_cameraPermissionsDenied && hasCameraPermission && hasStorageWritePermission)
                {
                    try
                    {
                        var file = new Java.IO.File(activity.FilesDir, "temp_camera_photo.jpg");
                        if (!file.Exists())
                        {
                            file.ParentFile.Mkdirs();
                            file.CreateNewFile();
                        }
                        var outputFileUri = FileProvider.GetUriForFile(activity,
                            "com.x8bit.bitwarden.fileprovider", file);
                        additionalIntents.AddRange(GetCameraIntents(outputFileUri));
                    }
                    catch (Java.IO.IOException) { }
                }
            }

            var docIntent = new Intent(Intent.ActionOpenDocument);
            docIntent.AddCategory(Intent.CategoryOpenable);
            docIntent.SetType("*/*");
            var chooserIntent = Intent.CreateChooser(docIntent, AppResources.FileSource);
            if (additionalIntents.Count > 0)
            {
                chooserIntent.PutExtra(Intent.ExtraInitialIntents, additionalIntents.ToArray());
            }
            activity.StartActivityForResult(chooserIntent, Constants.SelectFileRequestCode);
            return Task.FromResult(0);
        }

        public Task<string> DisplayPromptAync(string title = null, string description = null,
            string text = null, string okButtonText = null, string cancelButtonText = null,
            bool numericKeyboard = false, bool autofocus = true, bool password = false)
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            if (activity == null)
            {
                return Task.FromResult<string>(null);
            }

            var alertBuilder = new AlertDialog.Builder(activity);
            alertBuilder.SetTitle(title);
            alertBuilder.SetMessage(description);
            var input = new EditText(activity)
            {
                InputType = InputTypes.ClassText
            };
            if (text == null)
            {
                text = string.Empty;
            }
            if (numericKeyboard)
            {
                input.InputType = InputTypes.ClassNumber | InputTypes.NumberFlagDecimal | InputTypes.NumberFlagSigned;
#pragma warning disable CS0618 // Type or member is obsolete
                input.KeyListener = DigitsKeyListener.GetInstance(false, false);
#pragma warning restore CS0618 // Type or member is obsolete
            }
            if (password)
            {
                input.InputType = InputTypes.TextVariationPassword | InputTypes.ClassText;
            }

            input.ImeOptions = input.ImeOptions | (ImeAction)ImeFlags.NoPersonalizedLearning |
                (ImeAction)ImeFlags.NoExtractUi;
            input.Text = text;
            input.SetSelection(text.Length);
            var container = new FrameLayout(activity);
            var lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MatchParent,
                LinearLayout.LayoutParams.MatchParent);
            lp.SetMargins(25, 0, 25, 0);
            input.LayoutParameters = lp;
            container.AddView(input);
            alertBuilder.SetView(container);

            okButtonText = okButtonText ?? AppResources.Ok;
            cancelButtonText = cancelButtonText ?? AppResources.Cancel;
            var result = new TaskCompletionSource<string>();
            alertBuilder.SetPositiveButton(okButtonText,
                (sender, args) => result.TrySetResult(input.Text ?? string.Empty));
            alertBuilder.SetNegativeButton(cancelButtonText, (sender, args) => result.TrySetResult(null));

            var alert = alertBuilder.Create();
            alert.Window.SetSoftInputMode(Android.Views.SoftInput.StateVisible);
            alert.Show();
            if (autofocus)
            {
                input.RequestFocus();
            }
            return result.Task;
        }

        public void RateApp()
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            try
            {
                var rateIntent = RateIntentForUrl("market://details", activity);
                activity.StartActivity(rateIntent);
            }
            catch (ActivityNotFoundException)
            {
                var rateIntent = RateIntentForUrl("https://play.google.com/store/apps/details", activity);
                activity.StartActivity(rateIntent);
            }
        }

        public void DisableAutofillService()
        {
            try
            {
                var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
                var type = Java.Lang.Class.FromType(typeof(AutofillManager));
                var manager = activity.GetSystemService(type) as AutofillManager;
                manager.DisableAutofillServices();
            }
            catch { }
        }

        public bool AutofillServicesEnabled()
        {
            if (Build.VERSION.SdkInt <= BuildVersionCodes.M)
            {
                // Android 5-6: Both accessibility & overlay are required or nothing happens
                return AutofillAccessibilityServiceRunning() && AutofillAccessibilityOverlayPermitted();
            }
            if (Build.VERSION.SdkInt == BuildVersionCodes.N)
            {
                // Android 7: Only accessibility is required (overlay is optional when using quick-action tile)
                return AutofillAccessibilityServiceRunning();
            }
            // Android 8+: Either autofill or accessibility is required
            return AutofillServiceEnabled() || AutofillAccessibilityServiceRunning();
        }

        public string GetBuildNumber()
        {
            return Application.Context.ApplicationContext.PackageManager.GetPackageInfo(
                Application.Context.PackageName, 0).VersionCode.ToString();
        }

        public bool SupportsFaceBiometric()
        {
            // only used by iOS
            return false;
        }

        public Task<bool> SupportsFaceBiometricAsync()
        {
            // only used by iOS
            return Task.FromResult(SupportsFaceBiometric());
        }

        public bool SupportsNfc()
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            var manager = activity.GetSystemService(Context.NfcService) as NfcManager;
            return manager.DefaultAdapter?.IsEnabled ?? false;
        }

        public bool SupportsCamera()
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            return activity.PackageManager.HasSystemFeature(PackageManager.FeatureCamera);
        }

        public bool SupportsAutofillService()
        {
            if (Build.VERSION.SdkInt < BuildVersionCodes.O)
            {
                return false;
            }
            try
            {
                var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
                var type = Java.Lang.Class.FromType(typeof(AutofillManager));
                var manager = activity.GetSystemService(type) as AutofillManager;
                return manager.IsAutofillSupported;
            }
            catch
            {
                return false;
            }
        }

        public int SystemMajorVersion()
        {
            return (int)Build.VERSION.SdkInt;
        }

        public string SystemModel()
        {
            return Build.Model;
        }

        public Task<string> DisplayAlertAsync(string title, string message, string cancel, params string[] buttons)
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            if (activity == null)
            {
                return Task.FromResult<string>(null);
            }

            var result = new TaskCompletionSource<string>();
            var alertBuilder = new AlertDialog.Builder(activity);
            alertBuilder.SetTitle(title);

            if (!string.IsNullOrWhiteSpace(message))
            {
                if (buttons != null && buttons.Length > 2)
                {
                    if (!string.IsNullOrWhiteSpace(title))
                    {
                        alertBuilder.SetTitle($"{title}: {message}");
                    }
                    else
                    {
                        alertBuilder.SetTitle(message);
                    }
                }
                else
                {
                    alertBuilder.SetMessage(message);
                }
            }

            if (buttons != null)
            {
                if (buttons.Length > 2)
                {
                    alertBuilder.SetItems(buttons, (sender, args) =>
                    {
                        result.TrySetResult(buttons[args.Which]);
                    });
                }
                else
                {
                    if (buttons.Length > 0)
                    {
                        alertBuilder.SetPositiveButton(buttons[0], (sender, args) =>
                        {
                            result.TrySetResult(buttons[0]);
                        });
                    }
                    if (buttons.Length > 1)
                    {
                        alertBuilder.SetNeutralButton(buttons[1], (sender, args) =>
                        {
                            result.TrySetResult(buttons[1]);
                        });
                    }
                }
            }

            if (!string.IsNullOrWhiteSpace(cancel))
            {
                alertBuilder.SetNegativeButton(cancel, (sender, args) =>
                {
                    result.TrySetResult(cancel);
                });
            }

            var alert = alertBuilder.Create();
            alert.CancelEvent += (o, args) => { result.TrySetResult(null); };
            alert.Show();
            return result.Task;
        }

        public async Task<string> DisplayActionSheetAsync(string title, string cancel, string destruction,
            params string[] buttons)
        {
            return await Xamarin.Forms.Application.Current.MainPage.DisplayActionSheet(
                title, cancel, destruction, buttons);
        }

        public void Autofill(CipherView cipher)
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            if (activity == null)
            {
                return;
            }
            if (activity.Intent?.GetBooleanExtra("autofillFramework", false) ?? false)
            {
                if (cipher == null)
                {
                    activity.SetResult(Result.Canceled);
                    activity.Finish();
                    return;
                }
                var structure = activity.Intent.GetParcelableExtra(
                    AutofillManager.ExtraAssistStructure) as AssistStructure;
                if (structure == null)
                {
                    activity.SetResult(Result.Canceled);
                    activity.Finish();
                    return;
                }
                var parser = new Parser(structure, activity.ApplicationContext);
                parser.Parse();
                if ((!parser.FieldCollection?.Fields?.Any() ?? true) || string.IsNullOrWhiteSpace(parser.Uri))
                {
                    activity.SetResult(Result.Canceled);
                    activity.Finish();
                    return;
                }
                var task = CopyTotpAsync(cipher);
                var dataset = AutofillHelpers.BuildDataset(activity, parser.FieldCollection, new FilledItem(cipher));
                var replyIntent = new Intent();
                replyIntent.PutExtra(AutofillManager.ExtraAuthenticationResult, dataset);
                activity.SetResult(Result.Ok, replyIntent);
                activity.Finish();
                var eventTask = _eventServiceFunc().CollectAsync(EventType.Cipher_ClientAutofilled, cipher.Id);
            }
            else
            {
                var data = new Intent();
                if (cipher == null)
                {
                    data.PutExtra("canceled", "true");
                }
                else
                {
                    var task = CopyTotpAsync(cipher);
                    data.PutExtra("uri", cipher.Login.Uri);
                    data.PutExtra("username", cipher.Login.Username);
                    data.PutExtra("password", cipher.Login.Password);
                }
                if (activity.Parent == null)
                {
                    activity.SetResult(Result.Ok, data);
                }
                else
                {
                    activity.Parent.SetResult(Result.Ok, data);
                }
                activity.Finish();
                _messagingService.Send("finishMainActivity");
                if (cipher != null)
                {
                    var eventTask = _eventServiceFunc().CollectAsync(EventType.Cipher_ClientAutofilled, cipher.Id);
                }
            }
        }

        public void CloseAutofill()
        {
            Autofill(null);
        }

        public void Background()
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            if (activity.Intent?.GetBooleanExtra("autofillFramework", false) ?? false)
            {
                activity.SetResult(Result.Canceled);
                activity.Finish();
            }
            else
            {
                activity.MoveTaskToBack(true);
            }
        }

        public bool AutofillAccessibilityServiceRunning()
        {
            try
            {
                var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
                var manager = activity.GetSystemService(Context.ActivityService) as ActivityManager;
                var services = manager.GetRunningServices(int.MaxValue);
                return services.Any(s => s.Process.ToLowerInvariant().Contains("bitwarden") &&
                    s.Service.ClassName.ToLowerInvariant().Contains("accessibilityservice"));
            }
            catch
            {
                return false;
            }
        }

        public bool AutofillAccessibilityOverlayPermitted()
        {
            return Accessibility.AccessibilityHelpers.OverlayPermitted();
        }

        public void OpenAccessibilityOverlayPermissionSettings()
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            try
            {
                var intent = new Intent(Settings.ActionManageOverlayPermission);
                intent.SetData(Android.Net.Uri.Parse("package:com.x8bit.bitwarden"));
                activity.StartActivity(intent);
            }
            catch (ActivityNotFoundException)
            {
                // can't open overlay permission management, fall back to app settings
                var intent = new Intent(Settings.ActionApplicationDetailsSettings);
                intent.SetData(Android.Net.Uri.Parse("package:com.x8bit.bitwarden"));
                activity.StartActivity(intent);
            }
            catch
            {
                var alertBuilder = new AlertDialog.Builder(activity);
                alertBuilder.SetMessage(AppResources.BitwardenAutofillGoToSettings);
                alertBuilder.SetCancelable(true);
                alertBuilder.SetPositiveButton(AppResources.Ok, (sender, args) =>
                {
                    (sender as AlertDialog)?.Cancel();
                });
                alertBuilder.Create().Show();
            }
        }

        public bool AutofillServiceEnabled()
        {
            if (Build.VERSION.SdkInt < BuildVersionCodes.O)
            {
                return false;
            }
            try
            {
                var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
                var afm = (AutofillManager)activity.GetSystemService(
                    Java.Lang.Class.FromType(typeof(AutofillManager)));
                return afm.IsEnabled && afm.HasEnabledAutofillServices;
            }
            catch
            {
                return false;
            }
        }

        public void OpenAccessibilitySettings()
        {
            try
            {
                var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
                var intent = new Intent(Settings.ActionAccessibilitySettings);
                activity.StartActivity(intent);
            }
            catch { }
        }

        public void OpenAutofillSettings()
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            try
            {
                var intent = new Intent(Settings.ActionRequestSetAutofillService);
                intent.SetData(Android.Net.Uri.Parse("package:com.x8bit.bitwarden"));
                activity.StartActivity(intent);
            }
            catch (ActivityNotFoundException)
            {
                var alertBuilder = new AlertDialog.Builder(activity);
                alertBuilder.SetMessage(AppResources.BitwardenAutofillGoToSettings);
                alertBuilder.SetCancelable(true);
                alertBuilder.SetPositiveButton(AppResources.Ok, (sender, args) =>
                {
                    (sender as AlertDialog)?.Cancel();
                });
                alertBuilder.Create().Show();
            }
        }

        public bool UsingDarkTheme()
        {
            try
            {
                if (Build.VERSION.SdkInt >= BuildVersionCodes.Q)
                {
                    var app = CrossCurrentActivity.Current.AppContext;
                    var uiModeFlags = app.Resources.Configuration.UiMode & UiMode.NightMask;
                    return uiModeFlags == UiMode.NightYes;
                }
            }
            catch { }
            return false;
        }

        public long GetActiveTime()
        {
            // Returns milliseconds since the system was booted, and includes deep sleep. This clock is guaranteed to
            // be monotonic, and continues to tick even when the CPU is in power saving modes, so is the recommend
            // basis for general purpose interval timing.
            // ref: https://developer.android.com/reference/android/os/SystemClock#elapsedRealtime()
            return SystemClock.ElapsedRealtime();
        }
        
        public void CloseMainApp()
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            if (activity == null)
            {
                return;
            }
            activity.Finish();
            _messagingService.Send("finishMainActivity");
        }

        private bool DeleteDir(Java.IO.File dir)
        {
            if (dir != null && dir.IsDirectory)
            {
                var children = dir.List();
                for (int i = 0; i < children.Length; i++)
                {
                    var success = DeleteDir(new Java.IO.File(dir, children[i]));
                    if (!success)
                    {
                        return false;
                    }
                }
                return dir.Delete();
            }
            else if (dir != null && dir.IsFile)
            {
                return dir.Delete();
            }
            else
            {
                return false;
            }
        }

        private bool HasPermission(string permission)
        {
            return ContextCompat.CheckSelfPermission(
                CrossCurrentActivity.Current.Activity, permission) == Permission.Granted;
        }

        private void AskPermission(string permission)
        {
            ActivityCompat.RequestPermissions(CrossCurrentActivity.Current.Activity, new string[] { permission },
                Constants.SelectFilePermissionRequestCode);
        }

        private List<IParcelable> GetCameraIntents(Android.Net.Uri outputUri)
        {
            var intents = new List<IParcelable>();
            var pm = CrossCurrentActivity.Current.Activity.PackageManager;
            var captureIntent = new Intent(MediaStore.ActionImageCapture);
            var listCam = pm.QueryIntentActivities(captureIntent, 0);
            foreach (var res in listCam)
            {
                var packageName = res.ActivityInfo.PackageName;
                var intent = new Intent(captureIntent);
                intent.SetComponent(new ComponentName(packageName, res.ActivityInfo.Name));
                intent.SetPackage(packageName);
                intent.PutExtra(MediaStore.ExtraOutput, outputUri);
                intents.Add(intent);
            }
            return intents;
        }

        private Intent RateIntentForUrl(string url, Activity activity)
        {
            var intent = new Intent(Intent.ActionView, Android.Net.Uri.Parse($"{url}?id={activity.PackageName}"));
            var flags = ActivityFlags.NoHistory | ActivityFlags.MultipleTask;
            if ((int)Build.VERSION.SdkInt >= 21)
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

        private async Task CopyTotpAsync(CipherView cipher)
        {
            if (!string.IsNullOrWhiteSpace(cipher?.Login?.Totp))
            {
                var userService = ServiceContainer.Resolve<IUserService>("userService");
                var autoCopyDisabled = await _storageService.GetAsync<bool?>(Constants.DisableAutoTotpCopyKey);
                var canAccessPremium = await userService.CanAccessPremiumAsync();
                if ((canAccessPremium || cipher.OrganizationUseTotp) && !autoCopyDisabled.GetValueOrDefault())
                {
                    var totpService = ServiceContainer.Resolve<ITotpService>("totpService");
                    var totp = await totpService.GetCodeAsync(cipher.Login.Totp);
                    if (totp != null)
                    {
                        CopyToClipboard(totp);
                    }
                }
            }
        }

        private void CopyToClipboard(string text)
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            var clipboardManager = activity.GetSystemService(
                Context.ClipboardService) as Android.Content.ClipboardManager;
            clipboardManager.PrimaryClip = ClipData.NewPlainText("bitwarden", text);
        }
    }
}
