// WARNING
//
// This file has been generated automatically by Visual Studio to store outlets and
// actions made in the UI designer. If it is removed, they will be lost.
// Manual changes to this file may not be handled correctly.
//
using Foundation;
using System.CodeDom.Compiler;

namespace Bit.iOS.ShareExtension
{
	[Register ("LockPasswordViewController")]
	partial class LockPasswordViewController
	{
		[Outlet]
		UIKit.UIBarButtonItem _cancelButton { get; set; }

		[Outlet]
		UIKit.UITableView _mainTableView { get; set; }

		[Outlet]
		UIKit.UINavigationItem _navItem { get; set; }

		[Outlet]
		UIKit.UIBarButtonItem _submitButton { get; set; }

		[Action ("CancelButton_Activated:")]
		partial void CancelButton_Activated (UIKit.UIBarButtonItem sender);

		[Action ("SubmitButton_Activated:")]
		partial void SubmitButton_Activated (UIKit.UIBarButtonItem sender);
		
		void ReleaseDesignerOutlets ()
		{
			if (_cancelButton != null) {
				_cancelButton.Dispose ();
				_cancelButton = null;
			}

			if (_mainTableView != null) {
				_mainTableView.Dispose ();
				_mainTableView = null;
			}

			if (_navItem != null) {
				_navItem.Dispose ();
				_navItem = null;
			}

			if (_submitButton != null) {
				_submitButton.Dispose ();
				_submitButton = null;
			}
		}
	}
}
