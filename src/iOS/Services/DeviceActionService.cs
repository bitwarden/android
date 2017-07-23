using System;
using Bit.App.Abstractions;
using UIKit;
using Foundation;
using System.IO;
using MobileCoreServices;
using Bit.App.Resources;
using Xamarin.Forms;
using Photos;
using System.Net;
using System.Threading.Tasks;

namespace Bit.iOS.Services
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
            UIPasteboard clipboard = UIPasteboard.General;
            clipboard.String = text;
        }

        public bool OpenFile(byte[] fileData, string id, string fileName)
        {
            var filePath = Path.Combine(GetTempPath(), fileName);
            File.WriteAllBytes(filePath, fileData);
            var url = NSUrl.FromFilename(filePath);
            var viewer = UIDocumentInteractionController.FromUrl(url);
            var controller = GetVisibleViewController();
            return viewer.PresentOpenInMenu(controller.View.Frame, controller.View, true);
        }

        public bool CanOpenFile(string fileName)
        {
            // Not sure of a way to check this ahead of time on iOS
            return true;
        }

        public void ClearCache()
        {
            var url = new NSUrl(GetTempPath());
            NSError error;
            var tmpFiles = NSFileManager.DefaultManager.GetDirectoryContent(url, null,
                NSDirectoryEnumerationOptions.SkipsHiddenFiles, out error);
            if(error == null && tmpFiles.Length > 0)
            {
                foreach(var item in tmpFiles)
                {
                    NSError itemError;
                    NSFileManager.DefaultManager.Remove(item, out itemError);
                }
            }

            _appSettingsService.LastCacheClear = DateTime.UtcNow;
        }

        private UIViewController GetVisibleViewController(UIViewController controller = null)
        {
            controller = controller ?? UIApplication.SharedApplication.KeyWindow.RootViewController;
            if(controller.PresentedViewController == null)
            {
                return controller;
            }

            if(controller.PresentedViewController is UINavigationController)
            {
                return ((UINavigationController)controller.PresentedViewController).VisibleViewController;
            }

            if(controller.PresentedViewController is UITabBarController)
            {
                return ((UITabBarController)controller.PresentedViewController).SelectedViewController;
            }

            return GetVisibleViewController(controller.PresentedViewController);
        }

        // ref:  //https://developer.xamarin.com/guides/ios/application_fundamentals/working_with_the_file_system/
        public string GetTempPath()
        {
            var documents = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);
            var tmp = Path.Combine(documents, "..", "tmp");
            return tmp;
        }

        public Task SelectFileAsync()
        {
            var controller = GetVisibleViewController();
            var picker = new UIDocumentMenuViewController(new string[] { UTType.Data }, UIDocumentPickerMode.Import);

            picker.AddOption(AppResources.Camera, UIImage.FromBundle("camera"), UIDocumentMenuOrder.First, () =>
            {
                var imagePicker = new UIImagePickerController { SourceType = UIImagePickerControllerSourceType.Camera };
                imagePicker.FinishedPickingMedia += ImagePicker_FinishedPickingMedia;
                imagePicker.Canceled += ImagePicker_Canceled;
                controller.PresentModalViewController(imagePicker, true);
            });

            picker.AddOption(AppResources.Photos, UIImage.FromBundle("photo"), UIDocumentMenuOrder.First, () =>
            {
                var imagePicker = new UIImagePickerController { SourceType = UIImagePickerControllerSourceType.PhotoLibrary };
                imagePicker.FinishedPickingMedia += ImagePicker_FinishedPickingMedia;
                imagePicker.Canceled += ImagePicker_Canceled;
                controller.PresentModalViewController(imagePicker, true);
            });

            picker.DidPickDocumentPicker += (sender, e) =>
            {
                controller.PresentViewController(e.DocumentPicker, true, null);
                e.DocumentPicker.DidPickDocument += DocumentPicker_DidPickDocument;
            };

            controller.PresentViewController(picker, true, null);
            return Task.FromResult(0);
        }

        private void ImagePicker_FinishedPickingMedia(object sender, UIImagePickerMediaPickedEventArgs e)
        {
            if(sender is UIImagePickerController picker)
            {
                string fileName = null;
                NSObject urlObj;
                if(e.Info.TryGetValue(UIImagePickerController.ReferenceUrl, out urlObj))
                {
                    var result = PHAsset.FetchAssets(new NSUrl[] { (urlObj as NSUrl) }, null);
                    fileName = result?.firstObject?.ValueForKey(new NSString("filename"))?.ToString();
                }

                fileName = fileName ?? $"photo_{DateTime.UtcNow.ToString("yyyyMMddHHmmss")}.jpg";

                var lowerFilename = fileName?.ToLowerInvariant();
                byte[] data;
                if(lowerFilename != null && (lowerFilename.EndsWith(".jpg") || lowerFilename.EndsWith(".jpeg")))
                {
                    using(var imageData = e.OriginalImage.AsJPEG())
                    {
                        data = new byte[imageData.Length];
                        System.Runtime.InteropServices.Marshal.Copy(imageData.Bytes, data, 0,
                            Convert.ToInt32(imageData.Length));
                    }
                }
                else
                {
                    using(var imageData = e.OriginalImage.AsPNG())
                    {
                        data = new byte[imageData.Length];
                        System.Runtime.InteropServices.Marshal.Copy(imageData.Bytes, data, 0,
                            Convert.ToInt32(imageData.Length));
                    }
                }

                SelectFileResult(data, fileName);
                picker.DismissViewController(true, null);
            }
        }

        private void ImagePicker_Canceled(object sender, EventArgs e)
        {
            if(sender is UIImagePickerController picker)
            {
                picker.DismissViewController(true, null);
            }
        }

        private void DocumentPicker_DidPickDocument(object sender, UIDocumentPickedEventArgs e)
        {
            e.Url.StartAccessingSecurityScopedResource();

            var doc = new UIDocument(e.Url);
            var fileName = doc.LocalizedName;
            if(string.IsNullOrWhiteSpace(fileName))
            {
                var path = doc.FileUrl?.ToString();
                if(path != null)
                {
                    path = WebUtility.UrlDecode(path);
                    var split = path.LastIndexOf('/');
                    fileName = path.Substring(split + 1);
                }
            }

            var fileCoordinator = new NSFileCoordinator();
            NSError error;
            fileCoordinator.CoordinateRead(e.Url, NSFileCoordinatorReadingOptions.WithoutChanges, out error, (url) =>
            {
                var data = NSData.FromUrl(url).ToArray();
                SelectFileResult(data, fileName ?? "unknown_file_name");
            });

            e.Url.StopAccessingSecurityScopedResource();
        }

        private void SelectFileResult(byte[] data, string fileName)
        {
            MessagingCenter.Send(Xamarin.Forms.Application.Current, "SelectFileResult",
                new Tuple<byte[], string>(data, fileName));
        }
    }
}
