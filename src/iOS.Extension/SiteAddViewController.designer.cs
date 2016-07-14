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
	[Register ("SiteAddViewController")]
	partial class SiteAddViewController
	{
		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UIBarButtonItem CancelBarButton { get; set; }

		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UIBarButtonItem SaveBarButton { get; set; }

		[Action ("CancelBarButton_Activated:")]
		[GeneratedCode ("iOS Designer", "1.0")]
		partial void CancelBarButton_Activated (UIBarButtonItem sender);

		[Action ("SaveBarButton_Activated:")]
		[GeneratedCode ("iOS Designer", "1.0")]
		partial void SaveBarButton_Activated (UIBarButtonItem sender);

		void ReleaseDesignerOutlets ()
		{
			if (CancelBarButton != null) {
				CancelBarButton.Dispose ();
				CancelBarButton = null;
			}
			if (SaveBarButton != null) {
				SaveBarButton.Dispose ();
				SaveBarButton = null;
			}
			if (TableView != null) {
				TableView.Dispose ();
				TableView = null;
			}
		}
	}
}
