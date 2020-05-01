// WARNING
//
// This file has been generated automatically by Rider IDE
//   to store outlets and actions made in Xcode.
// If it is removed, they will be lost.
// Manual changes to this file may not be handled correctly.
//
using Foundation;
using System.CodeDom.Compiler;

namespace Bit.iOS.Autofill
{
	[Register ("LoginPasswordViewController")]
	partial class LoginPasswordViewController
	{
		[Outlet]
		UIKit.UIBarButtonItem CancelButton { get; set; }

		[Outlet]
		UIKit.UITableView MainTableView { get; set; }

		[Outlet]
		UIKit.UINavigationItem NavItem { get; set; }

		[Outlet]
		UIKit.UIBarButtonItem SubmitButton { get; set; }

		[Action ("CancelButton_Activated:")]
		partial void CancelButton_Activated (UIKit.UIBarButtonItem sender);

		[Action ("SubmitButton_Activated:")]
		partial void SubmitButton_Activated (UIKit.UIBarButtonItem sender);

		void ReleaseDesignerOutlets ()
		{
			if (CancelButton != null) {
				CancelButton.Dispose ();
				CancelButton = null;
			}

			if (MainTableView != null) {
				MainTableView.Dispose ();
				MainTableView = null;
			}

			if (NavItem != null) {
				NavItem.Dispose ();
				NavItem = null;
			}

			if (SubmitButton != null) {
				SubmitButton.Dispose ();
				SubmitButton = null;
			}

		}
	}
}
