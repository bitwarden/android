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
	[Register ("LoginListViewController")]
	partial class LoginListViewController
	{
		[Outlet]
		UIKit.UIBarButtonItem AccountSwitchingBarButton { get; set; }

		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UIKit.UIBarButtonItem AddBarButton { get; set; }

		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UIKit.UIBarButtonItem CancelBarButton { get; set; }

		[Outlet]
		UIKit.UIView MainView { get; set; }

		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UIKit.UINavigationItem NavItem { get; set; }

		[Outlet]
		UIKit.UIView OverlayView { get; set; }

		[Outlet]
		UIKit.UITableView TableView { get; set; }

		[Action ("AccountSwitchingBarButton_Activated:")]
		partial void AccountSwitchingBarButton_Activated (UIKit.UIBarButtonItem sender);

		[Action ("AddBarButton_Activated:")]
		partial void AddBarButton_Activated (UIKit.UIBarButtonItem sender);

		[Action ("CancelBarButton_Activated:")]
		partial void CancelBarButton_Activated (UIKit.UIBarButtonItem sender);

		[Action ("RefreshControl_Activated:")]
		partial void RefreshControl_Activated (UIKit.UIRefreshControl sender);

		[Action ("SearchBarButton_Activated:")]
		partial void SearchBarButton_Activated (UIKit.UIBarButtonItem sender);
		
		void ReleaseDesignerOutlets ()
		{
			if (AddBarButton != null) {
				AddBarButton.Dispose ();
				AddBarButton = null;
			}

			if (CancelBarButton != null) {
				CancelBarButton.Dispose ();
				CancelBarButton = null;
			}

			if (MainView != null) {
				MainView.Dispose ();
				MainView = null;
			}

			if (NavItem != null) {
				NavItem.Dispose ();
				NavItem = null;
			}

			if (OverlayView != null) {
				OverlayView.Dispose ();
				OverlayView = null;
			}

			if (TableView != null) {
				TableView.Dispose ();
				TableView = null;
			}

			if (AccountSwitchingBarButton != null) {
				AccountSwitchingBarButton.Dispose ();
				AccountSwitchingBarButton = null;
			}
		}
	}
}
