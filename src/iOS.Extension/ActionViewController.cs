using System;
using Bit.iOS.Core;
using Bit.iOS.Extension.Models;
using Foundation;
using MobileCoreServices;
using Newtonsoft.Json;
using UIKit;

namespace Bit.iOS.Extension
{
    public partial class ActionViewController : UIViewController
    {
        public ActionViewController(IntPtr handle) : base(handle)
        {
        }

        public Context Context { get; set; }

        public override void ViewDidLoad()
        {
            base.ViewDidLoad();
            View.BackgroundColor = UIColor.FromPatternImage(new UIImage("boxed-bg.png"));
        }

        partial void CancelClicked(UIBarButtonItem sender)
        {
            CompleteRequest(null);
        }

        partial void DoneClicked(NSObject sender)
        {
            NSDictionary itemData = null;
            if(Context.ProviderType == UTType.PropertyList)
            {
                var fillScript = new FillScript(Context.Details);
                var scriptJson = JsonConvert.SerializeObject(fillScript, new JsonSerializerSettings { NullValueHandling = NullValueHandling.Ignore });
                var scriptDict = new NSDictionary(Constants.AppExtensionWebViewPageFillScript, scriptJson);
                itemData = new NSDictionary(NSJavaScriptExtension.FinalizeArgumentKey, scriptDict);
            }
            if(Context.ProviderType == Constants.UTTypeAppExtensionFindLoginAction)
            {
                itemData = new NSDictionary(
                    Constants.AppExtensionUsernameKey, "me@example.com",
                    Constants.AppExtensionPasswordKey, "mypassword");
            }
            else if(Context.ProviderType == Constants.UTTypeAppExtensionFillBrowserAction
                || Context.ProviderType == Constants.UTTypeAppExtensionFillWebViewAction)
            {
                var fillScript = new FillScript(Context.Details);
                var scriptJson = JsonConvert.SerializeObject(fillScript, new JsonSerializerSettings { NullValueHandling = NullValueHandling.Ignore });
                itemData = new NSDictionary(Constants.AppExtensionWebViewPageFillScript, scriptJson);
            }
            else if(Context.ProviderType == Constants.UTTypeAppExtensionSaveLoginAction)
            {
                itemData = new NSDictionary(
                    Constants.AppExtensionUsernameKey, "me@example.com",
                    Constants.AppExtensionPasswordKey, "mypassword");
            }
            else if(Context.ProviderType == Constants.UTTypeAppExtensionChangePasswordAction)
            {
                itemData = new NSDictionary(
                    Constants.AppExtensionPasswordKey, "mynewpassword",
                    Constants.AppExtensionOldPasswordKey, "myoldpassword");
            }

            CompleteRequest(itemData);
        }

        private void CompleteRequest(NSDictionary itemData)
        {
            var resultsProvider = new NSItemProvider(itemData, UTType.PropertyList);
            var resultsItem = new NSExtensionItem { Attachments = new NSItemProvider[] { resultsProvider } };
            var returningItems = new NSExtensionItem[] { resultsItem };

            Context.ExtContext.CompleteRequest(returningItems, null);
        }
    }
}
