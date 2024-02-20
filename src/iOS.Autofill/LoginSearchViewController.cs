using System;
using Bit.iOS.Autofill.Models;
using Foundation;
using UIKit;
using Bit.iOS.Core.Controllers;
using Bit.Core.Resources.Localization;
using Bit.iOS.Core.Views;
using Bit.iOS.Autofill.Utilities;
using Bit.iOS.Core.Utilities;
using Bit.App.Abstractions;
using Bit.Core.Utilities;

namespace Bit.iOS.Autofill
{
    public partial class LoginSearchViewController : ExtendedUITableViewController, ILoginListViewController
    {
        public LoginSearchViewController(IntPtr handle)
            : base(handle)
        {
            DismissModalAction = Cancel;
        }

        public Context Context { get; set; }
        public CredentialProviderViewController CPViewController { get; set; }
        public bool FromList { get; set; }

        public async override void ViewDidLoad()
        {
            base.ViewDidLoad();
            NavItem.Title = AppResources.SearchVault;
            CancelBarButton.Title = AppResources.Cancel;
            SearchBar.Placeholder = AppResources.Search;
            SearchBar.BackgroundColor = SearchBar.BarTintColor = ThemeHelpers.ListHeaderBackgroundColor;
            SearchBar.UpdateThemeIfNeeded();

            TableView.RowHeight = UITableView.AutomaticDimension;
            TableView.EstimatedRowHeight = 44;
            TableView.Source = new TableSource(this);
            SearchBar.Delegate = new ExtensionSearchDelegate(TableView);
            await ((TableSource)TableView.Source).LoadAsync(false, SearchBar.Text);
        }

        public override void ViewDidAppear(bool animated)
        {
            base.ViewDidAppear(animated);
            SearchBar?.BecomeFirstResponder();
        }

        partial void CancelBarButton_Activated(UIBarButtonItem sender)
        {
            Cancel();
        }

        private void Cancel()
        {
            if (FromList)
            {
                DismissViewController(true, null);
            }
            else
            {
                CPViewController.CancelRequest(AuthenticationServices.ASExtensionErrorCode.UserCanceled);
            }
        }

        partial void AddBarButton_Activated(UIBarButtonItem sender)
        {
            PerformSegue(SegueConstants.ADD_LOGIN_FROM_SEARCH, this);
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            if (segue.DestinationViewController is UINavigationController navController)
            {
                if (navController.TopViewController is LoginAddViewController addLoginController)
                {
                    addLoginController.Context = Context;
                    addLoginController.LoginSearchController = this;
                    segue.DestinationViewController.PresentationController.Delegate =
                        new CustomPresentationControllerDelegate(addLoginController.DismissModalAction);
                }
            }
        }

        public void DismissModal()
        {
            DismissViewController(true, async () =>
            {
                await ((TableSource)TableView.Source).LoadAsync(false, SearchBar.Text);
                TableView.ReloadData();
            });
        }

        public class TableSource : BaseLoginListTableSource<LoginSearchViewController>
        {
            public TableSource(LoginSearchViewController controller)
                : base(controller)
            {
            }

            protected override string LoginAddSegue => SegueConstants.ADD_LOGIN_FROM_SEARCH;
        }
    }
}
