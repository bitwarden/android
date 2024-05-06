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
		UIKit.UIView _emptyView { get; set; }

		[Outlet]
		UIKit.UIButton _emptyViewButton { get; set; }

		[Outlet]
		UIKit.UIImageView _emptyViewImage { get; set; }

		[Outlet]
		UIKit.UILabel _emptyViewLabel { get; set; }

		[Outlet]
		UIKit.UIActivityIndicatorView _loadingView { get; set; }

		[Outlet]
		UIKit.UISearchBar _searchBar { get; set; }

		[Outlet]
		UIKit.NSLayoutConstraint _tableViewTopToSearchBarConstraint { get; set; }

		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UIKit.UIBarButtonItem AddBarButton { get; set; }

		[Outlet]
		UIKit.UIView MainView { get; set; }

		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UIKit.UINavigationItem NavItem { get; set; }

		[Outlet]
		UIKit.UIView OverlayView { get; set; }

		[Outlet]
		UIKit.UITableView TableView { get; set; }

		[Action ("AddBarButton_Activated:")]
		partial void AddBarButton_Activated (UIKit.UIBarButtonItem sender);

		[Action ("EmptyButton_Activated:")]
		partial void EmptyButton_Activated (UIKit.UIButton sender);

		[Action ("SearchBarButton_Activated:")]
		partial void SearchBarButton_Activated (UIKit.UIBarButtonItem sender);
		
		void ReleaseDesignerOutlets ()
		{
			if (_emptyView != null) {
				_emptyView.Dispose ();
				_emptyView = null;
			}

			if (_emptyViewButton != null) {
				_emptyViewButton.Dispose ();
				_emptyViewButton = null;
			}

			if (_emptyViewImage != null) {
				_emptyViewImage.Dispose ();
				_emptyViewImage = null;
			}

			if (_emptyViewLabel != null) {
				_emptyViewLabel.Dispose ();
				_emptyViewLabel = null;
			}

			if (_searchBar != null) {
				_searchBar.Dispose ();
				_searchBar = null;
			}

			if (_tableViewTopToSearchBarConstraint != null) {
				_tableViewTopToSearchBarConstraint.Dispose ();
				_tableViewTopToSearchBarConstraint = null;
			}

			if (AddBarButton != null) {
				AddBarButton.Dispose ();
				AddBarButton = null;
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

			if (_loadingView != null) {
				_loadingView.Dispose ();
				_loadingView = null;
			}
		}
	}
}
