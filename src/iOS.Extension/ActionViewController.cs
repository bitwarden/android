using System;

using MobileCoreServices;
using Foundation;
using UIKit;

namespace Bit.iOS.Extension
{
    public partial class ActionViewController : UIViewController
    {
        public ActionViewController(IntPtr handle) : base(handle)
        {
        }

        public string Content { get; set; }
        public Uri Uri { get; set; }

        public override void DidReceiveMemoryWarning()
        {
            base.DidReceiveMemoryWarning();
        }

        public override void ViewDidLoad()
        {
            base.ViewDidLoad();

            foreach(var item in ExtensionContext.InputItems)
            {
                foreach(var itemProvider in item.Attachments)
                {
                    if(itemProvider.HasItemConformingTo(UTType.PropertyList))
                    {
                        itemProvider.LoadItem(UTType.PropertyList, null, delegate (NSObject list, NSError error)
                        {
                            if(list != null)
                            {
                                var dict = list as NSDictionary;
                                var result = dict[NSJavaScriptExtension.PreprocessingResultsKey];
                                if(result != null)
                                {
                                    Content = result.ValueForKey(new NSString("content")) as NSString;
                                    Uri = new Uri(result.ValueForKey(new NSString("uri")) as NSString);
                                    Console.WriteLine("BITWARDEN LOG, Content: {0}", Content);
                                    Console.WriteLine("BITWARDEN LOG, Uri: {0}", Uri);
                                }
                            }
                        });
                        break;
                    }
                }
            }
        }

        partial void DoneClicked(NSObject sender)
        {

        }
    }
}
