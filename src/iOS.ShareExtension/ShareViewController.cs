using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Bit.iOS.Core;
using Bit.iOS.Core.Controllers;
using Bit.iOS.ShareExtension.Models;
using CoreFoundation;
using CoreNFC;
using MobileCoreServices;
using UIKit;

namespace Bit.iOS.ShareExtension
{
    public partial class ShareViewController : ExtendedUIViewController
    {
        private bool _initedAppCenter;
        private NFCNdefReaderSession _nfcSession = null;
        private NFCReaderDelegate _nfcDelegate = null;
        readonly LazyResolve<ISendService> _sendService = new LazyResolve<ISendService>("sendService");

        protected ShareViewController(IntPtr handle) : base(handle)
        {
            // Note: this .ctor should not contain any initialization logic.
        }

        public Context Context { get; set; }
        public LoadingViewController LoadingController { get; set; }

        public override void ViewDidLoad()
        {
            //InitApp();

            base.ViewDidLoad();

            if (Context.ProviderType == UTType.Image)
            {
                LoadImageFromExtensionItem();
            }

            //sendsAddEditViewController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            //// Present new view controller as a regular view controller
            //this.PresentModalViewController(sendsAddEditViewController, false);
        }

        async Task LoadImageFromExtensionItem()
        {
            var extensionItem = Context.ExtensionContext?.InputItems.FirstOrDefault();
            if (extensionItem?.Attachments is null)
                return;

            foreach (var item in extensionItem.Attachments)
            {
                if (!item.HasItemConformingTo(UTType.Image))
                    continue;

                var image = await item.LoadObjectAsync<UIImage>();
                DispatchQueue.MainQueue.DispatchAsync(() => _imageView.Image = image);
            }
        }

        partial void Send_Activated(UIBarButtonItem sender)
        {
            var sendView = new SendView()
            {
                Name = _nameField.Text,
                AccessCount = 1000,
                MaxAccessCount = 10000,
                Type = Bit.Core.Enums.SendType.File,
                DeletionDate = new DateTime(2022, 03, 03)
            };
            sendView.File.FileName = Guid.NewGuid().ToString() + ".png";

            byte[] data;
            using (var imageData = _imageView.Image.AsPNG())
            {
                data = new byte[imageData.Length];
                System.Runtime.InteropServices.Marshal.Copy(imageData.Bytes, data, 0,
                    Convert.ToInt32(imageData.Length));
            }

            Task.Run(async () =>
            {
                try
                {
                    var (send, encryptedFileData) = await _sendService.Value.EncryptAsync(sendView, data, null);// "Testing.123");
                    if (send == null)
                    {
                        return;
                    }
                    var sendId = await _sendService.Value.SaveWithServerAsync(send, encryptedFileData);
                }
                catch (Exception ex)
                {
                    Console.WriteLine(ex.Message);
                }
            });

            LoadingController.CompleteRequest(null, null);
        }

        partial void Cancel_Activated(UIBarButtonItem sender)
        {
            LoadingController.CompleteRequest(null, null);
        }


        //private void InitApp()
        //{
        //    if (ServiceContainer.RegisteredServices.Count > 0)
        //    {
        //        ServiceContainer.Reset();
        //    }
        //    iOSCoreHelpers.RegisterLocalServices();
        //    var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
        //    var messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
        //    ServiceContainer.Init(deviceActionService.DeviceUserAgent,
        //        Bit.Core.Constants.iOSExtensionClearCiphersCacheKey, Bit.Core.Constants.iOSAllClearCipherCacheKeys);
        //    if (!_initedAppCenter)
        //    {
        //        iOSCoreHelpers.RegisterAppCenter();
        //        _initedAppCenter = true;
        //    }
        //    iOSCoreHelpers.Bootstrap();
        //    //iOSCoreHelpers.AppearanceAdjustments();
        //    _nfcDelegate = new NFCReaderDelegate((success, message) =>
        //        messagingService.Send("gotYubiKeyOTP", message));
        //    iOSCoreHelpers.SubscribeBroadcastReceiver(this, _nfcSession, _nfcDelegate);
        //}
    }

    //private func imageFromExtensionItem(extensionItem: NSExtensionItem, callback: (image: UIImage?) -> Void) {
    
    //    for attachment in extensionItem.attachments as! [NSItemProvider] {
    //      if(attachment.hasItemConformingToTypeIdentifier(kUTTypeImage as String)) {
    //        // Marshal on to a background thread
    //        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, UInt(0))) {
    //          attachment.loadItemForTypeIdentifier(kUTTypeImage as String, options: nil) {
    //            (imageProvider, error) in
    //            var image: UIImage? = nil
    //            if let error = error {
    //              println("Item loading error: \(error.localizedDescription)")
    //            }
    //                image = imageProvider as? UIImage

    //                dispatch_async(dispatch_get_main_queue()) {
    //                    callback(image: image)
    //            }
    //          }
    //        }
    //      }
    //    }
    //}

    //public partial class ShareViewController : SLComposeServiceViewController
    //{
    //    protected ShareViewController(IntPtr handle) : base(handle)
    //    {
    //        // Note: this .ctor should not contain any initialization logic.
    //    }

    //    public override void DidReceiveMemoryWarning()
    //    {
    //        // Releases the view if it doesn't have a superview.
    //        base.DidReceiveMemoryWarning();

    //        // Release any cached data, images, etc that aren't in use.
    //    }

    //    public override void ViewDidLoad()
    //    {
    //        base.ViewDidLoad();

    //        // Do any additional setup after loading the view.
    //    }

    //    public override bool IsContentValid()
    //    {
    //        // Do validation of contentText and/or NSExtensionContext attachments here
    //        return true;
    //    }

    //    public override void DidSelectPost()
    //    {
    //        // This is called after the user selects Post. Do the upload of contentText and/or NSExtensionContext attachments.

    //        // Inform the host that we're done, so it un-blocks its UI. Note: Alternatively you could call super's -didSelectPost, which will similarly complete the extension context.
    //        ExtensionContext.CompleteRequest(new NSExtensionItem[0], null);
    //    }

    //    public override SLComposeSheetConfigurationItem[] GetConfigurationItems()
    //    {
    //        // To add configuration options via table cells at the bottom of the sheet, return an array of SLComposeSheetConfigurationItem here.
    //        return new SLComposeSheetConfigurationItem[0];
    //    }
    //}
}
