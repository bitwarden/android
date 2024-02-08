using System.Reflection;
using Foundation;
using Microsoft.Maui.Handlers;
using UIKit;
using ContentView = Microsoft.Maui.Platform.ContentView;

namespace Bit.iOS.Core.Handlers
{
    public partial class CustomContentPageHandler : PageHandler
    {
        private Page? _page;

        protected override void ConnectHandler(ContentView platformView)
        {
            if (VirtualView is Page page)
            {
                _page = page;
                _page.Loaded += Page_Loaded;
            }

            base.ConnectHandler(platformView);
        }

        private void Page_Loaded(object? sender, EventArgs e)
        {
            //Workaround: We can't use DisconnectHandler to dispose as we would have to call it manually from "outside" this class. So we unregister the event and set the page to null here. (it's very unlikely it would be called anyway)
            if (_page != null)
            {
                _page.Loaded -= Page_Loaded;
                _page = null;

                var navController = ViewController?.NavigationController;
                if (navController?.NavigationBar != null)
                {
                    CustomizeNavBar(navController);
                }
            }
        }

        private void CustomizeNavBar(UINavigationController navigationController)
        {
            // Hide bottom line under nav bar
            var navBar = navigationController.NavigationBar;
            navBar.SetValueForKey(NSObject.FromObject(true), new Foundation.NSString("hidesShadow"));

            var navigationItem = navigationController.TopViewController.NavigationItem;
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