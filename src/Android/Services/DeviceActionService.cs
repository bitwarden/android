using System;
using Android.Content;
using Bit.App.Abstractions;
using Xamarin.Forms;
using Android.Webkit;
using Plugin.CurrentActivity;
using System.IO;
using Android.Support.V4.Content;
using Bit.App;
using Bit.App.Resources;
using Android.Provider;
using System.Threading.Tasks;
using Android.OS;
using System.Collections.Generic;
using Android;
using Android.Content.PM;
using Android.Support.V4.App;
using Bit.App.Models.Page;
using XLabs.Ioc;
using Android.App;
using Android.Views.Autofill;
using Android.App.Assist;
using Bit.Android.Autofill;
using System.Linq;
using Plugin.Settings.Abstractions;
using Android.Views.InputMethods;
using Android.Widget;

namespace Bit.Android.Services
{
    public class DeviceActionService : IDeviceActionService
    {
        private readonly IAppSettingsService _appSettingsService;
        private bool _cameraPermissionsDenied;
        private DateTime? _lastAction;
        private ProgressDialog _progressDialog;
        private global::Android.Widget.Toast _toast;

        public DeviceActionService(
            IAppSettingsService appSettingsService)
        {
            _appSettingsService = appSettingsService;
        }

        private Context CurrentContext => CrossCurrentActivity.Current.Activity;

        public void Toast(string text, bool longDuration = false)
        {
            if(_toast != null)
            {
                _toast.Cancel();
                _toast.Dispose();
                _toast = null;
            }

            _toast = global::Android.Widget.Toast.MakeText(CurrentContext, text,
                longDuration ? global::Android.Widget.ToastLength.Long : global::Android.Widget.ToastLength.Short);
            _toast.Show();
        }

        public void CopyToClipboard(string text)
        {
            var clipboardManager = (ClipboardManager)CurrentContext.GetSystemService(Context.ClipboardService);
            clipboardManager.Text = text;
        }

        public bool OpenFile(byte[] fileData, string id, string fileName)
        {
            if(!CanOpenFile(fileName))
            {
                return false;
            }

            var extension = MimeTypeMap.GetFileExtensionFromUrl(fileName.Replace(' ', '_').ToLower());
            if(extension == null)
            {
                return false;
            }

            var mimeType = MimeTypeMap.Singleton.GetMimeTypeFromExtension(extension);
            if(mimeType == null)
            {
                return false;
            }

            var cachePath = CrossCurrentActivity.Current.Activity.CacheDir;
            var filePath = Path.Combine(cachePath.Path, fileName);
            File.WriteAllBytes(filePath, fileData);
            var file = new Java.IO.File(cachePath, fileName);
            if(!file.IsFile)
            {
                return false;
            }

            try
            {
                var intent = new Intent(Intent.ActionView);
                var uri = FileProvider.GetUriForFile(CrossCurrentActivity.Current.Activity.ApplicationContext,
                    "com.x8bit.bitwarden.fileprovider", file);
                intent.SetDataAndType(uri, mimeType);
                intent.SetFlags(ActivityFlags.GrantReadUriPermission);
                CrossCurrentActivity.Current.Activity.StartActivity(intent);
                return true;
            }
            catch { }

            return false;
        }

        public bool CanOpenFile(string fileName)
        {
            var extension = MimeTypeMap.GetFileExtensionFromUrl(fileName.Replace(' ', '_').ToLower());
            if(extension == null)
            {
                return false;
            }

            var mimeType = MimeTypeMap.Singleton.GetMimeTypeFromExtension(extension);
            if(mimeType == null)
            {
                return false;
            }

            var pm = CrossCurrentActivity.Current.Activity.PackageManager;
            var intent = new Intent(Intent.ActionView);
            intent.SetType(mimeType);
            var activities = pm.QueryIntentActivities(intent, PackageInfoFlags.MatchDefaultOnly);
            return (activities?.Count ?? 0) > 0;
        }

        public void ClearCache()
        {
            try
            {
                DeleteDir(CrossCurrentActivity.Current.Activity.CacheDir);
                _appSettingsService.LastCacheClear = DateTime.UtcNow;
            }
            catch(Exception) { }
        }

        public Task SelectFileAsync()
        {
            MessagingCenter.Unsubscribe<Xamarin.Forms.Application>(Xamarin.Forms.Application.Current,
                "SelectFileCameraPermissionDenied");

            var hasStorageWritePermission = !_cameraPermissionsDenied && HasPermission(Manifest.Permission.WriteExternalStorage);

            var additionalIntents = new List<IParcelable>();
            if(CurrentContext.PackageManager.HasSystemFeature(PackageManager.FeatureCamera))
            {
                var hasCameraPermission = !_cameraPermissionsDenied && HasPermission(Manifest.Permission.Camera);

                if(!_cameraPermissionsDenied && !hasStorageWritePermission)
                {
                    AskCameraPermission(Manifest.Permission.WriteExternalStorage);
                    return Task.FromResult(0);
                }

                if(!_cameraPermissionsDenied && !hasCameraPermission)
                {
                    AskCameraPermission(Manifest.Permission.Camera);
                    return Task.FromResult(0);
                }

                if(!_cameraPermissionsDenied && hasCameraPermission && hasStorageWritePermission)
                {
                    try
                    {
                        var root = new Java.IO.File(global::Android.OS.Environment.ExternalStorageDirectory, "bitwarden");
                        var file = new Java.IO.File(root, "temp_camera_photo.jpg");
                        if(!file.Exists())
                        {
                            file.ParentFile.Mkdirs();
                            file.CreateNewFile();
                        }
                        var outputFileUri = global::Android.Net.Uri.FromFile(file);
                        additionalIntents.AddRange(GetCameraIntents(outputFileUri));
                    }
                    catch(Java.IO.IOException) { }
                }
            }

            var docIntent = new Intent(Intent.ActionOpenDocument);
            docIntent.AddCategory(Intent.CategoryOpenable);
            docIntent.SetType("*/*");

            var chooserIntent = Intent.CreateChooser(docIntent, AppResources.FileSource);
            if(additionalIntents.Count > 0)
            {
                chooserIntent.PutExtra(Intent.ExtraInitialIntents, additionalIntents.ToArray());
            }

            CrossCurrentActivity.Current.Activity.StartActivityForResult(chooserIntent, Constants.SelectFileRequestCode);
            return Task.FromResult(0);
        }

        public void Autofill(VaultListPageModel.Cipher cipher)
        {
            var activity = (MainActivity)CurrentContext;
            if(activity.Intent.GetBooleanExtra("autofillFramework", false))
            {
                if(cipher == null)
                {
                    activity.SetResult(Result.Canceled);
                    activity.Finish();
                    return;
                }

                var structure = activity.Intent.GetParcelableExtra(
                    AutofillManager.ExtraAssistStructure) as AssistStructure;
                if(structure == null)
                {
                    activity.SetResult(Result.Canceled);
                    activity.Finish();
                    return;
                }

                var parser = new Parser(structure);
                parser.Parse();
                if(!parser.FieldCollection.Fields.Any() || string.IsNullOrWhiteSpace(parser.Uri))
                {
                    activity.SetResult(Result.Canceled);
                    activity.Finish();
                    return;
                }

                var dataset = AutofillHelpers.BuildDataset(activity, parser.FieldCollection,
                    new FilledItem(cipher.CipherModel));
                var replyIntent = new Intent();
                replyIntent.PutExtra(AutofillManager.ExtraAuthenticationResult, dataset);
                activity.SetResult(Result.Ok, replyIntent);
                activity.Finish();
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
                    var settings = Resolver.Resolve<ISettings>();
                    var autoCopyEnabled = !settings.GetValueOrDefault(Constants.SettingDisableTotpCopy, false);
                    if(isPremium && autoCopyEnabled && cipher.LoginTotp?.Value != null)
                    {
                        CopyToClipboard(App.Utilities.Crypto.Totp(cipher.LoginTotp.Value));
                    }

                    data.PutExtra("uri", cipher.LoginUri);
                    data.PutExtra("username", cipher.LoginUsername);
                    data.PutExtra("password", cipher.LoginPassword?.Value ?? null);
                }

                if(activity.Parent == null)
                {
                    activity.SetResult(Result.Ok, data);
                }
                else
                {
                    activity.Parent.SetResult(Result.Ok, data);
                }

                activity.Finish();
                MessagingCenter.Send(Xamarin.Forms.Application.Current, "FinishMainActivity");
            }
        }

        public void CloseAutofill()
        {
            Autofill(null);
        }

        public void Background()
        {
            var activity = (MainActivity)CurrentContext;
            if(activity.Intent.GetBooleanExtra("autofillFramework", false))
            {
                activity.SetResult(Result.Canceled);
                activity.Finish();
            }
            else
            {
                activity.MoveTaskToBack(true);
            }
        }

        public void RateApp()
        {
            var activity = (MainActivity)CurrentContext;
            try
            {
                var rateIntent = RateIntentForUrl("market://details", activity);
                activity.StartActivity(rateIntent);
            }
            catch(ActivityNotFoundException)
            {
                var rateIntent = RateIntentForUrl("https://play.google.com/store/apps/details", activity);
                activity.StartActivity(rateIntent);
            }
        }

        public void DismissKeyboard()
        {
            try
            {
                var activity = (MainActivity)CurrentContext;
                var imm = (InputMethodManager)activity.GetSystemService(Context.InputMethodService);
                imm.HideSoftInputFromWindow(activity.CurrentFocus.WindowToken, 0);
            }
            catch { }
        }

        public void OpenAccessibilitySettings()
        {
            var activity = (MainActivity)CurrentContext;
            var intent = new Intent(Settings.ActionAccessibilitySettings);
            activity.StartActivity(intent);
        }

        public async Task LaunchAppAsync(string appName, Page page)
        {
            var activity = (MainActivity)CurrentContext;
            if(_lastAction.LastActionWasRecent())
            {
                return;
            }
            _lastAction = DateTime.UtcNow;

            appName = appName.Replace("androidapp://", string.Empty);
            var launchIntent = activity.PackageManager.GetLaunchIntentForPackage(appName);
            if(launchIntent == null)
            {
                await page.DisplayAlert(null, string.Format(AppResources.CannotOpenApp, appName), AppResources.Ok);
            }
            else
            {
                activity.StartActivity(launchIntent);
            }
        }

        private Intent RateIntentForUrl(string url, Activity activity)
        {
            var intent = new Intent(Intent.ActionView, global::Android.Net.Uri.Parse($"{url}?id={activity.PackageName}"));
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

        private bool DeleteDir(Java.IO.File dir)
        {
            if(dir != null && dir.IsDirectory)
            {
                var children = dir.List();
                for(int i = 0; i < children.Length; i++)
                {
                    var success = DeleteDir(new Java.IO.File(dir, children[i]));
                    if(!success)
                    {
                        return false;
                    }
                }
                return dir.Delete();
            }
            else if(dir != null && dir.IsFile)
            {
                return dir.Delete();
            }
            else
            {
                return false;
            }
        }

        private List<IParcelable> GetCameraIntents(global::Android.Net.Uri outputUri)
        {
            var intents = new List<IParcelable>();
            var pm = CrossCurrentActivity.Current.Activity.PackageManager;
            var captureIntent = new Intent(MediaStore.ActionImageCapture);
            var listCam = pm.QueryIntentActivities(captureIntent, 0);
            foreach(var res in listCam)
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

        private bool HasPermission(string permission)
        {
            return ContextCompat.CheckSelfPermission(CrossCurrentActivity.Current.Activity, permission) == Permission.Granted;
        }

        private void AskCameraPermission(string permission)
        {
            MessagingCenter.Subscribe<Xamarin.Forms.Application>(Xamarin.Forms.Application.Current,
                "SelectFileCameraPermissionDenied", (sender) =>
                {
                    _cameraPermissionsDenied = true;
                });

            AskPermission(permission);
        }

        private void AskPermission(string permission)
        {
            ActivityCompat.RequestPermissions(CrossCurrentActivity.Current.Activity, new string[] { permission },
                Constants.SelectFilePermissionRequestCode);
        }

        public void OpenAutofillSettings()
        {
            try
            {
                var activity = (MainActivity)CurrentContext;
                var intent = new Intent(Settings.ActionRequestSetAutofillService);
                intent.SetData(global::Android.Net.Uri.Parse("package:com.x8bit.bitwarden"));
                activity.StartActivity(intent);
            }
            catch(ActivityNotFoundException)
            {
                var alertBuilder = new AlertDialog.Builder((MainActivity)CurrentContext);
                alertBuilder.SetMessage(AppResources.BitwardenAutofillGoToSettings);
                alertBuilder.SetCancelable(true);
                alertBuilder.SetPositiveButton(AppResources.Ok, (sender, args) =>
                {
                    (sender as AlertDialog)?.Cancel();
                });
                alertBuilder.Create().Show();
            }
        }

        public async Task ShowLoadingAsync(string text)
        {
            if(_progressDialog != null)
            {
                await HideLoadingAsync();
            }

            var activity = (MainActivity)CurrentContext;
            _progressDialog = new ProgressDialog(activity);
            _progressDialog.SetMessage(text);
            _progressDialog.SetCancelable(false);
            _progressDialog.Show();
        }

        public Task HideLoadingAsync()
        {
            if(_progressDialog != null)
            {
                _progressDialog.Dismiss();
                _progressDialog.Dispose();
                _progressDialog = null;
            }

            return Task.FromResult(0);
        }

        public Task<string> DisplayPromptAync(string title = null, string description = null, string text = null)
        {
            var activity = (MainActivity)CurrentContext;
            if(activity == null)
            {
                return Task.FromResult<string>(null);
            }

            var alertBuilder = new AlertDialog.Builder(activity);
            alertBuilder.SetTitle(title);
            alertBuilder.SetMessage(description);

            var input = new EditText(activity)
            {
                InputType = global::Android.Text.InputTypes.ClassText
            };

            if(text == null)
            {
                text = string.Empty;
            }

            input.Text = text;
            input.SetSelection(text.Length);

            alertBuilder.SetView(input);

            var result = new TaskCompletionSource<string>();
            alertBuilder.SetPositiveButton(AppResources.Ok, (sender, args) =>
            {
                result.TrySetResult(input.Text ?? string.Empty);
            });

            alertBuilder.SetNegativeButton(AppResources.Cancel, (sender, args) =>
            {
                result.TrySetResult(null);
            });

            var alert = alertBuilder.Create();
            alert.Window.SetSoftInputMode(global::Android.Views.SoftInput.StateVisible);
            alert.Show();
            return result.Task;
        }

        public Task<string> DisplayAlertAsync(string title, string message, string cancel, params string[] buttons)
        {
            var activity = (MainActivity)CurrentContext;
            if(activity == null)
            {
                return Task.FromResult<string>(null);
            }

            var result = new TaskCompletionSource<string>();
            var alertBuilder = new AlertDialog.Builder(activity);
            alertBuilder.SetTitle(title);

            if(!string.IsNullOrWhiteSpace(message))
            {
                if(buttons != null && buttons.Length > 2)
                {
                    if(!string.IsNullOrWhiteSpace(title))
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

            if(buttons != null)
            {
                if(buttons.Length > 2)
                {
                    alertBuilder.SetItems(buttons, (sender, args) =>
                    {
                        result.TrySetResult(buttons[args.Which]);
                    });
                }
                else
                {
                    if(buttons.Length > 0)
                    {
                        alertBuilder.SetPositiveButton(buttons[0], (sender, args) =>
                        {
                            result.TrySetResult(buttons[0]);
                        });
                    }
                    if(buttons.Length > 1)
                    {
                        alertBuilder.SetNeutralButton(buttons[1], (sender, args) =>
                        {
                            result.TrySetResult(buttons[1]);
                        });
                    }
                }
            }

            if(!string.IsNullOrWhiteSpace(cancel))
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
    }
}
