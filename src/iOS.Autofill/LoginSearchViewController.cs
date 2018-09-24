using System;
using Bit.iOS.Autofill.Models;
using Foundation;
using UIKit;
using Bit.iOS.Core.Controllers;
using Bit.App.Resources;
using Bit.iOS.Core.Views;
using Bit.iOS.Autofill.Utilities;

namespace Bit.iOS.Autofill
{
    public partial class LoginSearchViewController : ExtendedUITableViewController
    {
        public LoginSearchViewController(IntPtr handle) : base(handle)
        { }

        public Context Context { get; set; }
        public CredentialProviderViewController CPViewController { get; set; }

        public override void ViewWillAppear(bool animated)
        {
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);
            base.ViewWillAppear(animated);
        }

        public async override void ViewDidLoad()
        {
            base.ViewDidLoad();
            NavItem.Title = AppResources.SearchVault;
            CancelBarButton.Title = AppResources.Cancel;
            SearchBar.Placeholder = AppResources.Search;

            TableView.RowHeight = UITableView.AutomaticDimension;
            TableView.EstimatedRowHeight = 44;
            TableView.Source = new TableSource(this);
            SearchBar.Delegate = new ExtensionSearchDelegate(TableView);
            await ((TableSource)TableView.Source).LoadItemsAsync(false, SearchBar.Text);
        }

        partial void CancelBarButton_Activated(UIBarButtonItem sender)
        {
            CPViewController.CompleteRequest();
        }

        partial void AddBarButton_Activated(UIBarButtonItem sender)
        {
            PerformSegue("loginAddFromSearchSegue", this);
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            var navController = segue.DestinationViewController as UINavigationController;
            if(navController != null)
            {
                var addLoginController = navController.TopViewController as LoginAddViewController;
                if(addLoginController != null)
                {
                    addLoginController.Context = Context;
                    addLoginController.LoginSearchController = this;
                }
            }
        }

        public void DismissModal()
        {
            DismissViewController(true, async () =>
            {
                await ((TableSource)TableView.Source).LoadItemsAsync(false, SearchBar.Text);
                TableView.ReloadData();
            });
        }

        public class TableSource : ExtensionTableSource
        {
            private Context _context;
            private LoginSearchViewController _controller;

            public TableSource(LoginSearchViewController controller)
                : base(controller.Context, controller)
            {
                _context = controller.Context;
                _controller = controller;
            }

            public override void RowSelected(UITableView tableView, NSIndexPath indexPath)
            {
                AutofillHelpers.TableRowSelected(tableView, indexPath, this,
                    _controller.CPViewController, _controller, _settings, "loginAddFromSearchSegue");
            }
        }
    }
}
