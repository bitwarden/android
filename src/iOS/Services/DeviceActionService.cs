using System;
using Bit.App.Abstractions;
using UIKit;
using Foundation;
using System.IO;
using MobileCoreServices;

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

        public byte[] SelectFile()
        {
            var controller = GetVisibleViewController();

            var allowedUTIs = new string[]
            {
                UTType.AliasFile,
                UTType.AliasRecord,
                UTType.AppleICNS,
                UTType.Image,
                UTType.Movie,
                UTType.GIF,
                UTType.Video,
                UTType.Folder,
                UTType.ApplicationFile,
                UTType.JPEG,
                UTType.PNG,
                UTType.BMP,
                UTType.Spreadsheet
            };

            var picker = new UIDocumentMenuViewController(allowedUTIs, UIDocumentPickerMode.Open);
            picker.AddOption("Camera", null, UIDocumentMenuOrder.First, () =>
            {
                var imagePicker = new UIImagePickerController { SourceType = UIImagePickerControllerSourceType.Camera };

                imagePicker.FinishedPickingMedia += (sender, ev) =>
                {
                    //var filepath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments), "tmp.png");
                    //var image = (UIImage)ev.Info.ObjectForKey(new NSString("UIImagePickerControllerOriginalImage"));
                    //DismissViewController(true, null)
                };

                imagePicker.Canceled += (sender, ev2) =>
                {
                    //DismissViewController(true, null)
                };

                controller.PresentModalViewController(imagePicker, true);
            });
            picker.AddOption("Photo Library", null, UIDocumentMenuOrder.First, () =>
            {
                var imagePicker = new UIImagePickerController { SourceType = UIImagePickerControllerSourceType.PhotoLibrary };

                imagePicker.FinishedPickingMedia += (sender, ev) =>
                {
                    //var filepath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments), "tmp.png");
                    //var image = (UIImage)ev.Info.ObjectForKey(new NSString("UIImagePickerControllerOriginalImage"));
                    //DismissViewController(true, null)
                };

                imagePicker.Canceled += (sender, ev2) =>
                {
                    //DismissViewController(true, null)
                };

                controller.PresentModalViewController(imagePicker, true);
            });

            controller.PresentViewController(picker, true, null);

            return null;
        }
    }
}
