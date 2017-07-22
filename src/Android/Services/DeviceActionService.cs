using System;
using Android.Content;
using Bit.App.Abstractions;
using Xamarin.Forms;
using Android.Webkit;
using Plugin.CurrentActivity;
using System.IO;
using Android.Support.V4.Content;
using Bit.App;

namespace Bit.Android.Services
{
    public class DeviceActionService : IDeviceActionService
    {
        private readonly IAppSettingsService _appSettingsService;

        public DeviceActionService(IAppSettingsService appSettingsService)
        {
            _appSettingsService = appSettingsService;
        }

        public void CopyToClipboard(string text)
        {
            var clipboardManager = (ClipboardManager)Forms.Context.GetSystemService(Context.ClipboardService);
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
            var activities = pm.QueryIntentActivities(intent, global::Android.Content.PM.PackageInfoFlags.MatchDefaultOnly);
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

        public void SelectFile()
        {
            var intent = new Intent(Intent.ActionOpenDocument);
            intent.AddCategory(Intent.CategoryOpenable);
            intent.SetType("*/*");
            CrossCurrentActivity.Current.Activity.StartActivityForResult(intent, Constants.SelectFileRequestCode);
        }
    }
}
