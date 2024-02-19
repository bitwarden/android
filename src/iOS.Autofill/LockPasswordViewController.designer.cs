// WARNING
//
// This file has been generated automatically by Visual Studio to store outlets and
// actions made in the UI designer. If it is removed, they will be lost.
// Manual changes to this file may not be handled correctly.
//
using Foundation;
using System.CodeDom.Compiler;

namespace Bit.iOS.Autofill
{
	[Register ("LockPasswordViewController")]
	partial class LockPasswordViewController
	{
		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UIKit.UITableView MainTableView { get; set; }

		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UIKit.UINavigationItem NavItem { get; set; }

		[Outlet]
		UIKit.UIView OverlayView { get; set; }

		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UIKit.UIBarButtonItem SubmitButton { get; set; }

		[Action ("SubmitButton_Activated:")]
		partial void SubmitButton_Activated (UIKit.UIBarButtonItem sender);
		
		void ReleaseDesignerOutlets ()
		{
			if (MainTableView != null) {
				MainTableView.Dispose ();
				MainTableView = null;
			}

			if (NavItem != null) {
				NavItem.Dispose ();
				NavItem = null;
			}

			if (OverlayView != null) {
				OverlayView.Dispose ();
				OverlayView = null;
			}

			if (SubmitButton != null) {
				SubmitButton.Dispose ();
				SubmitButton = null;
			}
		}
	}
}
