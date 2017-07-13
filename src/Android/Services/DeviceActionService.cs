using System;
using Android.Content;
using Bit.App.Abstractions;
using Xamarin.Forms;
using Java.IO;
using Android.Webkit;
using Plugin.CurrentActivity;
using System.IO;
using System.Diagnostics;
using Android.Support.V4.Content;

namespace Bit.Android.Services
{
    public class DeviceActionService : IDeviceActionService
    {
        public void CopyToClipboard(string text)
        {
            var clipboardManager = (ClipboardManager)Forms.Context.GetSystemService(Context.ClipboardService);
            clipboardManager.Text = text;
        }

        public bool OpenFile(byte[] fileData, string id, string fileName)
        {
            var extension = MimeTypeMap.GetFileExtensionFromUrl(fileName);
            if(extension == null)
            {
                return false;
            }

            var mimeType = MimeTypeMap.Singleton.GetMimeTypeFromExtension(extension.ToLower());
            if(mimeType == null)
            {
                return false;
            }

            var cachePath = CrossCurrentActivity.Current.Activity.CacheDir;
            var filePath = Path.Combine(cachePath.Path, fileName);
            System.IO.File.WriteAllBytes(filePath, fileData);
            var file = new Java.IO.File(cachePath, fileName);
            try
            {
                var packageManager = CrossCurrentActivity.Current.Activity.PackageManager;
                var testIntent = new Intent(Intent.ActionView);
                testIntent.SetType(mimeType);
                var list = packageManager.QueryIntentActivities(testIntent,
                    global::Android.Content.PM.PackageInfoFlags.MatchDefaultOnly);
                if(list.Count > 0 && file.IsFile)
                {
                    var intent = new Intent(Intent.ActionView);
                    var uri = FileProvider.GetUriForFile(CrossCurrentActivity.Current.Activity.ApplicationContext,
                        "com.x8bit.bitwarden.fileprovider", file);
                    intent.SetDataAndType(uri, mimeType);
                    intent.SetFlags(ActivityFlags.GrantReadUriPermission);
                    CrossCurrentActivity.Current.Activity.StartActivity(intent);
                    return true;
                }
            }
            catch { }

            return false;
        }
    }
}
