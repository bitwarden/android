using System;
using System.IO;
using System.Threading.Tasks;
using Android.App;
using Android.Content;
using Android.Content.PM;
using Android.Support.V4.Content;
using Android.Webkit;
using Android.Widget;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Plugin.CurrentActivity;

namespace Bit.Droid.Services
{
    public class DeviceActionService : IDeviceActionService
    {
        private readonly IStorageService _storageService;

        private ProgressDialog _progressDialog;
        private Android.Widget.Toast _toast;

        public DeviceActionService(IStorageService storageService)
        {
            _storageService = storageService;
        }

        public DeviceType DeviceType => DeviceType.Android;

        public void Toast(string text, bool longDuration = false)
        {
            if(_toast != null)
            {
                _toast.Cancel();
                _toast.Dispose();
                _toast = null;
            }
            _toast = Android.Widget.Toast.MakeText(CrossCurrentActivity.Current.Activity, text,
                longDuration ? Android.Widget.ToastLength.Long : Android.Widget.ToastLength.Short);
            _toast.Show();
        }

        public bool LaunchApp(string appName)
        {
            var activity = CrossCurrentActivity.Current.Activity;
            appName = appName.Replace("androidapp://", string.Empty);
            var launchIntent = activity.PackageManager.GetLaunchIntentForPackage(appName);
            if(launchIntent != null)
            {
                activity.StartActivity(launchIntent);
            }
            return launchIntent != null;
        }

        public async Task ShowLoadingAsync(string text)
        {
            if(_progressDialog != null)
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
            if(_progressDialog != null)
            {
                _progressDialog.Dismiss();
                _progressDialog.Dispose();
                _progressDialog = null;
            }
            return Task.FromResult(0);
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

            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            var cachePath = activity.CacheDir;
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
                var uri = FileProvider.GetUriForFile(activity.ApplicationContext,
                    "com.x8bit.bitwarden.fileprovider", file);
                intent.SetDataAndType(uri, mimeType);
                intent.SetFlags(ActivityFlags.GrantReadUriPermission);
                activity.StartActivity(intent);
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
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            var intent = new Intent(Intent.ActionView);
            intent.SetType(mimeType);
            var activities = activity.PackageManager.QueryIntentActivities(intent, PackageInfoFlags.MatchDefaultOnly);
            return (activities?.Count ?? 0) > 0;
        }

        public async Task ClearCacheAsync()
        {
            try
            {
                DeleteDir(CrossCurrentActivity.Current.Activity.CacheDir);
                await _storageService.SaveAsync(Constants.LastFileCacheClearKey, DateTime.UtcNow);
            }
            catch(Exception) { }
        }

        public Task<string> DisplayPromptAync(string title = null, string description = null,
            string text = null, string okButtonText = null, string cancelButtonText = null)
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            if(activity == null)
            {
                return Task.FromResult<string>(null);
            }

            var alertBuilder = new AlertDialog.Builder(activity);
            alertBuilder.SetTitle(title);
            alertBuilder.SetMessage(description);
            var input = new EditText(activity)
            {
                InputType = Android.Text.InputTypes.ClassText
            };
            if(text == null)
            {
                text = string.Empty;
            }

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
            return result.Task;
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
    }
}