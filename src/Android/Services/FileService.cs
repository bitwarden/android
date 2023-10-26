using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using Android;
using Android.Content;
using Android.Content.PM;
using Android.OS;
using Android.Provider;
using Android.Webkit;
using AndroidX.Core.App;
using AndroidX.Core.Content;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Plugin.CurrentActivity;

namespace Bit.Droid.Services
{
    public class FileService : IFileService
    {
        private readonly IStateService _stateService;
        private readonly IBroadcasterService _broadcasterService;

        private bool _cameraPermissionsDenied;

        public FileService(IStateService stateService, IBroadcasterService broadcasterService)
        {
            _stateService = stateService;
            _broadcasterService = broadcasterService;

            _broadcasterService.Subscribe(nameof(FileService), (message) =>
            {
                if (message.Command == "selectFileCameraPermissionDenied")
                {
                    _cameraPermissionsDenied = true;
                }
            });
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

                activity.StartActivityForResult(intent, Core.Constants.SaveFileRequestCode);
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
                await _stateService.SetLastFileCacheClearAsync(DateTime.UtcNow);
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
                        var tmpDir = new Java.IO.File(activity.FilesDir, Constants.TEMP_CAMERA_IMAGE_DIR);
                        var file = new Java.IO.File(tmpDir, Constants.TEMP_CAMERA_IMAGE_NAME);
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
            activity.StartActivityForResult(chooserIntent, Core.Constants.SelectFileRequestCode);
            return Task.FromResult(0);
        }

        private bool DeleteDir(Java.IO.File dir)
        {
            if (dir is null)
            {
                return false;
            }

            if (dir.IsDirectory)
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

            if (dir.IsFile)
            {
                return dir.Delete();
            }

            return false;
        }

        private bool HasPermission(string permission)
        {
            return ContextCompat.CheckSelfPermission(
                CrossCurrentActivity.Current.Activity, permission) == Permission.Granted;
        }

        private void AskPermission(string permission)
        {
            ActivityCompat.RequestPermissions(CrossCurrentActivity.Current.Activity, new string[] { permission },
                Core.Constants.SelectFilePermissionRequestCode);
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
    }
}
