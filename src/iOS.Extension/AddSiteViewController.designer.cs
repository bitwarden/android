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
	[Register ("AddSiteViewController")]
	partial class AddSiteViewController
	{
		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UITableView tableView { get; set; }

		[Action ("UIBarButtonItem2289_Activated:")]
		[GeneratedCode ("iOS Designer", "1.0")]
		partial void UIBarButtonItem2289_Activated (UIBarButtonItem sender);

		[Action ("UIBarButtonItem2290_Activated:")]
		[GeneratedCode ("iOS Designer", "1.0")]
		partial void UIBarButtonItem2290_Activated (UIBarButtonItem sender);

		void ReleaseDesignerOutlets ()
		{
			if (tableView != null) {
				tableView.Dispose ();
				tableView = null;
			}
		}
	}
}
