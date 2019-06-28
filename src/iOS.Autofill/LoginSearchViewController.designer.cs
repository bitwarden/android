// WARNING
//
// This file has been generated automatically by Visual Studio from the outlets and
// actions declared in your storyboard file.
// Manual changes to this file will not be maintained.
//
using Foundation;
using System;
using System.CodeDom.Compiler;
using UIKit;

namespace Bit.iOS.Autofill
{
    [Register ("LoginSearchViewController")]
    partial class LoginSearchViewController
    {
        [Outlet]
        [GeneratedCode ("iOS Designer", "1.0")]
        UIKit.UIBarButtonItem CancelBarButton { get; set; }

        [Outlet]
        [GeneratedCode ("iOS Designer", "1.0")]
        UIKit.UINavigationItem NavItem { get; set; }

        [Outlet]
        [GeneratedCode ("iOS Designer", "1.0")]
        UIKit.UISearchBar SearchBar { get; set; }

        [Action ("AddBarButton_Activated:")]
        [GeneratedCode ("iOS Designer", "1.0")]
        partial void AddBarButton_Activated (UIKit.UIBarButtonItem sender);

        [Action ("CancelBarButton_Activated:")]
        [GeneratedCode ("iOS Designer", "1.0")]
        partial void CancelBarButton_Activated (UIKit.UIBarButtonItem sender);

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

            if (SearchBar != null) {
                SearchBar.Dispose ();
                SearchBar = null;
            }
        }
    }
}