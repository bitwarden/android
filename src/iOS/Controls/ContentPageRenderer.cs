using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using Bit.iOS.Controls;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ContentPage), typeof(ContentPageRenderer))]
namespace Bit.iOS.Controls
{
    public class ContentPageRenderer : PageRenderer
    {
        public override void ViewWillAppear(bool animated)
        {
            base.ViewWillAppear(animated);

            var contentPage = Element as ContentPage;
            if(contentPage == null || NavigationController == null)
            {
                return;
            }

            var itemsInfo = contentPage.ToolbarItems;

            var navigationItem = NavigationController.TopViewController.NavigationItem;
            var leftNativeButtons = (navigationItem.LeftBarButtonItems ?? new UIBarButtonItem[] { }).ToList();
            var rightNativeButtons = (navigationItem.RightBarButtonItems ?? new UIBarButtonItem[] { }).ToList();

            var newLeftButtons = new List<UIBarButtonItem>();
            var newRightButtons = new List<UIBarButtonItem>();

            rightNativeButtons.ForEach(nativeItem =>
            {
                // Use reflection to get Xamarin private field "_item"
                var field = nativeItem.GetType().GetField("_item", BindingFlags.NonPublic | BindingFlags.Instance);
                if(field == null)
                {
                    return;
                }

                var info = field.GetValue(nativeItem) as ToolbarItem;
                if(info == null)
                {
                    return;
                }

                if(info.Priority < 0)
                {
                    newLeftButtons.Add(nativeItem);
                }
                else
                {
                    newRightButtons.Add(nativeItem);
                }
            });

            leftNativeButtons.ForEach(nativeItem =>
            {
                newLeftButtons.Add(nativeItem);
            });

            navigationItem.RightBarButtonItems = newRightButtons.ToArray();
            navigationItem.LeftBarButtonItems = newLeftButtons.ToArray();
        }
    }
}
