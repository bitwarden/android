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

namespace Bit.Android.Services
{
    public class DeviceActionService : IDeviceActionService
    {
        private readonly IAppSettingsService _appSettingsService;
        private bool _cameraPermissionsDenied;

        public DeviceActionService(
            IAppSettingsService appSettingsService)
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

        public Task SelectFileAsync()
        {
            MessagingCenter.Unsubscribe<Application>(Application.Current, "SelectFileCameraPermissionDenied");

            var hasStorageWritePermission = !_cameraPermissionsDenied && HasPermission(Manifest.Permission.WriteExternalStorage);

            var additionalIntents = new List<IParcelable>();
            if(Forms.Context.PackageManager.HasSystemFeature(PackageManager.FeatureCamera))
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
            MessagingCenter.Subscribe<Application>(Application.Current, "SelectFileCameraPermissionDenied", (sender) =>
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
    }
}
