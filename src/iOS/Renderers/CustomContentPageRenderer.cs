using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using Bit.iOS.Renderers;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ContentPage), typeof(CustomContentPageRenderer))]
namespace Bit.iOS.Renderers
{
    public class CustomContentPageRenderer : PageRenderer
    {
        public override void ViewWillAppear(bool animated)
        {
            base.ViewWillAppear(animated);
            if (!(Element is ContentPage contentPage) || NavigationController == null)
            {
                return;
            }

            // Hide bottom line under nav bar
            var navBar = NavigationController.NavigationBar;
            if (navBar != null)
            {
                navBar.SetValueForKey(FromObject(true), new Foundation.NSString("hidesShadow"));
            }

            var navigationItem = NavigationController.TopViewController.NavigationItem;
            var leftNativeButtons = (navigationItem.LeftBarButtonItems ?? new UIBarButtonItem[] { }).ToList();
            var rightNativeButtons = (navigationItem.RightBarButtonItems ?? new UIBarButtonItem[] { }).ToList();
            var newLeftButtons = new List<UIBarButtonItem>();
            var newRightButtons = new List<UIBarButtonItem>();
            foreach (var nativeItem in rightNativeButtons)
            {
                // Use reflection to get Xamarin private field "_item"
                var field = nativeItem.GetType().GetField("_item", BindingFlags.NonPublic | BindingFlags.Instance);
                if (field == null)
                {
                    return;
                }
                if (!(field.GetValue(nativeItem) is ToolbarItem info))
                {
                    return;
                }
                if (info.Priority < 0)
                {
                    newLeftButtons.Add(nativeItem);
                }
                else
                {
                    newRightButtons.Add(nativeItem);
                }
            }
            foreach (var nativeItem in leftNativeButtons)
            {
                newLeftButtons.Add(nativeItem);
            }
            navigationItem.RightBarButtonItems = newRightButtons.ToArray();
            navigationItem.LeftBarButtonItems = newLeftButtons.ToArray();
        }
    }
}
