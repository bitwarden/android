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
	[Register ("LockPinViewController")]
	partial class LockPinViewController
	{
		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UIBarButtonItem CancelButton { get; set; }

		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UILabel PinLabel { get; set; }

		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UITextField PinTextField { get; set; }

		[Action ("CancelButton_Activated:")]
		[GeneratedCode ("iOS Designer", "1.0")]
		partial void CancelButton_Activated (UIBarButtonItem sender);

		void ReleaseDesignerOutlets ()
		{
			if (CancelButton != null) {
				CancelButton.Dispose ();
				CancelButton = null;
			}
			if (PinLabel != null) {
				PinLabel.Dispose ();
				PinLabel = null;
			}
			if (PinTextField != null) {
				PinTextField.Dispose ();
				PinTextField = null;
			}
		}
	}
}
