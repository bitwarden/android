using System;
using Bit.App.Controls;
using Bit.iOS.Controls;
using Foundation;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ExtendedTabbedPage), typeof(ExtendedTabbedPageRenderer))]
namespace Bit.iOS.Controls
{
    public class ExtendedTabbedPageRenderer : TabbedRenderer
    {
        protected override void OnElementChanged(VisualElementChangedEventArgs e)
        {
            base.OnElementChanged(e);

            var page = (ExtendedTabbedPage)Element;

            TabBar.TintColor = page.TintColor.ToUIColor();
            TabBar.BackgroundColor = page.BackgroundColor.ToUIColor();

            if(page.NoBorder)
            {
                // remove top border
                // ref: http://stackoverflow.com/questions/14371343/ios-uitabbar-remove-top-shadow-gradient-line
                TabBar.SetValueForKeyPath(FromObject(true), new NSString("_hidesShadow"));
            }
        }

        public override void ViewWillAppear(bool animated)
        {
            if(TabBar?.Items == null)
            {
                return;
            }

            var tabs = Element as TabbedPage;
            if(tabs != null)
            {
                for(int i = 0; i < TabBar.Items.Length; i++)
                {
                    UpdateItem(TabBar.Items[i], tabs.Children[i].Icon);
                }
            }

            base.ViewWillAppear(animated);
        }

        private void UpdateItem(UITabBarItem item, string icon)
        {
            if(item == null)
            {
                return;
            }

            try
            {
                icon = string.Concat(icon, "_selected");
                if(item?.SelectedImage?.AccessibilityIdentifier == icon)
                {
                    return;
                }

                item.SelectedImage = UIImage.FromBundle(icon);
                item.SelectedImage.AccessibilityIdentifier = icon;
            }
            catch { }
        }
    }
}
