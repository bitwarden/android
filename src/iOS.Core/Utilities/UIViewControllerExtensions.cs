using System;
using UIKit;

namespace Bit.iOS.Core.Utilities
{
    public static class UIViewControllerExtensions
    {
        public static UIViewController GetVisibleViewController()
        {
            return GetVisibleViewController(UIApplication.SharedApplication.KeyWindow.RootViewController);
        }

        public static UIViewController GetVisibleViewController(this UIViewController controller)
        {
            if (controller?.PresentedViewController == null)
            {
                return controller;
            }
            if (controller.PresentedViewController is UINavigationController)
            {
                return ((UINavigationController)controller.PresentedViewController).VisibleViewController;
            }
            if (controller.PresentedViewController is UITabBarController)
            {
                return ((UITabBarController)controller.PresentedViewController).SelectedViewController;
            }
            return GetVisibleViewController(controller.PresentedViewController);
        }
    }
}

