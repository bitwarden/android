using System;
using CoreGraphics;
using Foundation;
using MobileCoreServices;
using UIKit;

namespace Bit.iOS.Extension
{
    public partial class ActionViewController : UIViewController
    {
        public ActionViewController() : base("ActionViewController", null)
        {
        }

        public string HtmlContent { get; set; }
        public Uri BaseUri { get; set; }
        public Uri Url { get; set; }

        public override void DidReceiveMemoryWarning()
        {
            base.DidReceiveMemoryWarning();
        }

        public override void LoadView()
        {
            View = new UIView(new CGRect(x: 0.0, y: 0, width: 320.0, height: 200.0));
            var button = new UIButton(new CGRect(x: 10.0, y: 50.0, width: 200.0, height: 30.0));
            button.SetTitle("Done", UIControlState.Normal);
            button.TouchUpInside += Button_TouchUpInside;
            View.AddSubview(button);
        }

        private void Button_TouchUpInside(object sender, EventArgs e)
        {
            var itemData = new NSDictionary(
                new NSString("username"),
                new NSString("myusername"),
                new NSString("password"),
                new NSString("mypassword"));

            var resultsProvider = new NSItemProvider(
                new NSDictionary(NSJavaScriptExtension.FinalizeArgumentKey, itemData),
                UTType.PropertyList);

            var resultsItem = new NSExtensionItem
            {
                Attachments = new NSItemProvider[] { resultsProvider }
            };

            var returningItems = new NSExtensionItem[] { resultsItem };
            ExtensionContext.CompleteRequest(returningItems, null);
        }

        public override void ViewDidLoad()
        {
            base.ViewDidLoad();

            foreach(var item in ExtensionContext.InputItems)
            {
                foreach(var itemProvider in item.Attachments)
                {
                    if(!itemProvider.HasItemConformingTo(UTType.PropertyList))
                    {
                        continue;
                    }

                    itemProvider.LoadItem(UTType.PropertyList, null, (NSObject list, NSError error) =>
                    {
                        if(list == null)
                        {
                            return;
                        }

                        var dict = list as NSDictionary;
                        var result = dict[NSJavaScriptExtension.PreprocessingResultsKey];
                        if(result == null)
                        {
                            return;
                        }

                        HtmlContent = result.ValueForKey(new NSString("htmlContent")) as NSString;
                        BaseUri = new Uri(result.ValueForKey(new NSString("baseUri")) as NSString);
                        Url = new Uri(result.ValueForKey(new NSString("url")) as NSString);
                    });

                    break;
                }
            }
        }

    }
}