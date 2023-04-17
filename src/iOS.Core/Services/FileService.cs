using System;
using System.IO;
using System.Net;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.iOS.Core.Utilities;
using Foundation;
using MobileCoreServices;
using Photos;
using UIKit;

namespace Bit.iOS.Core.Services
{
    public class FileService : IFileService
    {
        private readonly IStateService _stateService;
        private readonly IMessagingService _messagingService;

        public FileService(IStateService stateService, IMessagingService messagingService)
        {
            _stateService = stateService;
            _messagingService = messagingService;
        }

        public bool OpenFile(byte[] fileData, string id, string fileName)
        {
            var filePath = Path.Combine(GetTempPath(), fileName);
            File.WriteAllBytes(filePath, fileData);
            var url = NSUrl.FromFilename(filePath);
            var controller = UIViewControllerExtensions.GetVisibleViewController();

            try
            {
                UIView presentingView = UIApplication.SharedApplication.KeyWindow.RootViewController.View;
                var documentController = new UIDocumentPickerViewController(url, UIDocumentPickerMode.ExportToService);
                controller.PresentViewController(documentController, true, null);

                return true;
            }
            catch
            {
                return false;
            }
        }

        public bool CanOpenFile(string fileName)
        {
            // Not sure of a way to check this ahead of time on iOS
            return true;
        }

        public bool SaveFile(byte[] fileData, string id, string fileName, string contentUri)
        {
            // OpenFile behavior is appropriate here as iOS prompts to save file
            return OpenFile(fileData, id, fileName);
        }

        public async Task ClearCacheAsync()
        {
            var url = new NSUrl(GetTempPath());
            var tmpFiles = NSFileManager.DefaultManager.GetDirectoryContent(url, null,
                NSDirectoryEnumerationOptions.SkipsHiddenFiles, out NSError error);
            if (error == null && tmpFiles.Length > 0)
            {
                foreach (var item in tmpFiles)
                {
                    NSFileManager.DefaultManager.Remove(item, out NSError itemError);
                }
            }
            await _stateService.SetLastFileCacheClearAsync(DateTime.UtcNow);
        }

        public Task SelectFileAsync()
        {
            var controller = UIViewControllerExtensions.GetVisibleViewController();
            var picker = new UIDocumentMenuViewController(new string[] { UTType.Data }, UIDocumentPickerMode.Import);
            picker.AddOption(AppResources.Camera, UIImage.FromBundle("camera"), UIDocumentMenuOrder.First, () =>
            {
                var imagePicker = new UIImagePickerController
                {
                    SourceType = UIImagePickerControllerSourceType.Camera
                };
                imagePicker.FinishedPickingMedia += ImagePicker_FinishedPickingMedia;
                imagePicker.Canceled += ImagePicker_Canceled;
                controller.PresentModalViewController(imagePicker, true);
            });
            picker.AddOption(AppResources.Photos, UIImage.FromBundle("photo"), UIDocumentMenuOrder.First, () =>
            {
                var imagePicker = new UIImagePickerController
                {
                    SourceType = UIImagePickerControllerSourceType.PhotoLibrary
                };
                imagePicker.FinishedPickingMedia += ImagePicker_FinishedPickingMedia;
                imagePicker.Canceled += ImagePicker_Canceled;
                controller.PresentModalViewController(imagePicker, true);
            });
            picker.DidPickDocumentPicker += (sender, e) =>
            {
                if (UIDevice.CurrentDevice.CheckSystemVersion(11, 0))
                {
                    e.DocumentPicker.Delegate = new PickerDelegate(this);
                }
                else
                {
                    e.DocumentPicker.DidPickDocument += DocumentPicker_DidPickDocument;
                }
                controller.PresentViewController(e.DocumentPicker, true, null);
            };
            var root = UIApplication.SharedApplication.KeyWindow.RootViewController;
            if (picker.PopoverPresentationController != null && root != null)
            {
                picker.PopoverPresentationController.SourceView = root.View;
                picker.PopoverPresentationController.SourceRect = root.View.Bounds;
            }
            controller.PresentViewController(picker, true, null);
            return Task.CompletedTask;
        }

        // ref:  //https://developer.xamarin.com/guides/ios/application_fundamentals/working_with_the_file_system/
        public string GetTempPath()
        {
            var documents = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);
            return Path.Combine(documents, "..", "tmp");
        }

        private void ImagePicker_FinishedPickingMedia(object sender, UIImagePickerMediaPickedEventArgs e)
        {
            if (sender is UIImagePickerController picker)
            {
                string fileName = null;
                if (e.Info.TryGetValue(UIImagePickerController.ReferenceUrl, out NSObject urlObj))
                {
                    var result = PHAsset.FetchAssets(new NSUrl[] { (urlObj as NSUrl) }, null);
                    fileName = result?.firstObject?.ValueForKey(new NSString("filename"))?.ToString();
                }
                fileName = fileName ?? $"photo_{DateTime.UtcNow.ToString("yyyyMMddHHmmss")}.jpg";
                var lowerFilename = fileName?.ToLowerInvariant();
                byte[] data;
                if (lowerFilename != null && (lowerFilename.EndsWith(".jpg") || lowerFilename.EndsWith(".jpeg")))
                {
                    using (var imageData = e.OriginalImage.AsJPEG())
                    {
                        data = new byte[imageData.Length];
                        System.Runtime.InteropServices.Marshal.Copy(imageData.Bytes, data, 0,
                            Convert.ToInt32(imageData.Length));
                    }
                }
                else
                {
                    using (var imageData = e.OriginalImage.AsPNG())
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
            if (sender is UIImagePickerController picker)
            {
                picker.DismissViewController(true, null);
            }
        }

        private void DocumentPicker_DidPickDocument(object sender, UIDocumentPickedEventArgs e)
        {
            PickedDocument(e.Url);
        }

        public void PickedDocument(NSUrl url)
        {
            url.StartAccessingSecurityScopedResource();
            var doc = new UIDocument(url);
            var fileName = doc.LocalizedName;
            if (string.IsNullOrWhiteSpace(fileName))
            {
                var path = doc.FileUrl?.ToString();
                if (path != null)
                {
                    path = WebUtility.UrlDecode(path);
                    var split = path.LastIndexOf('/');
                    fileName = path.Substring(split + 1);
                }
            }
            var fileCoordinator = new NSFileCoordinator();
            fileCoordinator.CoordinateRead(url, NSFileCoordinatorReadingOptions.WithoutChanges,
                out NSError error, (u) =>
                {
                    var data = NSData.FromUrl(u).ToArray();
                    SelectFileResult(data, fileName ?? "unknown_file_name");
                });
            url.StopAccessingSecurityScopedResource();
        }

        private void SelectFileResult(byte[] data, string fileName)
        {
            _messagingService.Send("selectFileResult", new Tuple<byte[], string>(data, fileName));
        }

        public class PickerDelegate : UIDocumentPickerDelegate
        {
            private readonly FileService _fileService;

            public PickerDelegate(FileService fileService)
            {
                _fileService = fileService;
            }

            public override void DidPickDocument(UIDocumentPickerViewController controller, NSUrl url)
            {
                _fileService.PickedDocument(url);
            }
        }
    }
}
