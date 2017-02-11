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
    [Register ("PasswordGeneratorViewController")]
    partial class PasswordGeneratorViewController
    {
        [Outlet]
        [GeneratedCode ("iOS Designer", "1.0")]
        UIKit.UIView BaseView { get; set; }

        [Outlet]
        [GeneratedCode ("iOS Designer", "1.0")]
        UIKit.UIBarButtonItem CancelBarButton { get; set; }

        [Outlet]
        [GeneratedCode ("iOS Designer", "1.0")]
        UIKit.UINavigationItem NavItem { get; set; }

        [Outlet]
        [GeneratedCode ("iOS Designer", "1.0")]
        UIKit.UIView OptionsContainer { get; set; }

        [Outlet]
        [GeneratedCode ("iOS Designer", "1.0")]
        UIKit.UILabel PasswordLabel { get; set; }

        [Outlet]
        [GeneratedCode ("iOS Designer", "1.0")]
        UIKit.UIBarButtonItem SelectBarButton { get; set; }

        [Action ("CancelBarButton_Activated:")]
        [GeneratedCode ("iOS Designer", "1.0")]
        partial void CancelBarButton_Activated (UIKit.UIBarButtonItem sender);

        [Action ("SelectBarButton_Activated:")]
        [GeneratedCode ("iOS Designer", "1.0")]
        partial void SelectBarButton_Activated (UIKit.UIBarButtonItem sender);

        void ReleaseDesignerOutlets ()
        {
            if (BaseView != null) {
                BaseView.Dispose ();
                BaseView = null;
            }

            if (CancelBarButton != null) {
                CancelBarButton.Dispose ();
                CancelBarButton = null;
            }

            if (NavItem != null) {
                NavItem.Dispose ();
                NavItem = null;
            }

            if (OptionsContainer != null) {
                OptionsContainer.Dispose ();
                OptionsContainer = null;
            }

            if (PasswordLabel != null) {
                PasswordLabel.Dispose ();
                PasswordLabel = null;
            }

            if (SelectBarButton != null) {
                SelectBarButton.Dispose ();
                SelectBarButton = null;
            }
        }
    }
}