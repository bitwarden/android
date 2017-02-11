// WARNING
//
// This file has been generated automatically by Xamarin Studio from the outlets and
// actions declared in your storyboard file.
// Manual changes to this file will not be maintained.
//
using Foundation;
using System;
using System.CodeDom.Compiler;
using UIKit;

namespace Bit.iOS.Extension
{
    [Register ("LoginAddViewController")]
    partial class LoginAddViewController
    {
        [Outlet]
        [GeneratedCode ("iOS Designer", "1.0")]
        UIKit.UIBarButtonItem CancelBarButton { get; set; }

        [Outlet]
        [GeneratedCode ("iOS Designer", "1.0")]
        UIKit.UINavigationItem NavItem { get; set; }

        [Outlet]
        [GeneratedCode ("iOS Designer", "1.0")]
        UIKit.UIBarButtonItem SaveBarButton { get; set; }

        [Action ("CancelBarButton_Activated:")]
        [GeneratedCode ("iOS Designer", "1.0")]
        partial void CancelBarButton_Activated (UIKit.UIBarButtonItem sender);

        [Action ("SaveBarButton_Activated:")]
        [GeneratedCode ("iOS Designer", "1.0")]
        partial void SaveBarButton_Activated (UIKit.UIBarButtonItem sender);

        void ReleaseDesignerOutlets ()
        {
            if (CancelBarButton != null) {
                CancelBarButton.Dispose ();
                CancelBarButton = null;
            }

            if (NavItem != null) {
                NavItem.Dispose ();
                NavItem = null;
            }

            if (SaveBarButton != null) {
                SaveBarButton.Dispose ();
                SaveBarButton = null;
            }
        }
    }
}