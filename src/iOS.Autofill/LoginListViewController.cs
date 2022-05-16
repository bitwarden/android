using System;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Bit.iOS.Autofill.Models;
using Bit.iOS.Autofill.Utilities;
using Bit.iOS.Core.Controllers;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Views;
using Foundation;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class LoginListViewController : ExtendedUIViewController
    {
        public LoginListViewController(IntPtr handle)
            : base(handle)
        {
            DismissModalAction = Cancel;
            PasswordRepromptService = ServiceContainer.Resolve<IPasswordRepromptService>("passwordRepromptService");
        }

        public Context Context { get; set; }
        public CredentialProviderViewController CPViewController { get; set; }
        public IPasswordRepromptService PasswordRepromptService { get; private set; }

        AccountSwitchingOverlayView _accountSwitchingOverlayView;
        AccountSwitchingOverlayHelper _accountSwitchingOverlayHelper;

        public async override void ViewDidLoad()
        {
            base.ViewDidLoad();
            NavItem.Title = AppResources.Items;
            CancelBarButton.Title = AppResources.Cancel;

            TableView.RowHeight = UITableView.AutomaticDimension;
            TableView.EstimatedRowHeight = 44;
            TableView.Source = new TableSource(this);
            await ((TableSource)TableView.Source).LoadItemsAsync();

            var storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            var needsAutofillReplacement = await storageService.GetAsync<bool?>(
                Core.Constants.AutofillNeedsIdentityReplacementKey);
            if (needsAutofillReplacement.GetValueOrDefault())
            {
                await ASHelpers.ReplaceAllIdentities();
            }

            _accountSwitchingOverlayHelper = new AccountSwitchingOverlayHelper();
            AccountSwitchingBarButton.Image = await _accountSwitchingOverlayHelper.CreateAvatarImageAsync();

            _accountSwitchingOverlayView = _accountSwitchingOverlayHelper.CreateAccountSwitchingOverlayView(OverlayView);
        }

        partial void AccountSwitchingBarButton_Activated(UIBarButtonItem sender)
        {
            var overlayVisible = _accountSwitchingOverlayView.IsVisible;
            _accountSwitchingOverlayView.ToggleVisibililtyCommand.Execute(null);
            OverlayView.Hidden = false;
            OverlayView.UserInteractionEnabled = !overlayVisible;
            OverlayView.Subviews[0].UserInteractionEnabled = !overlayVisible;
        }

        partial void CancelBarButton_Activated(UIBarButtonItem sender)
        {
            Cancel();
        }

        private void Cancel()
        {
            CPViewController.CompleteRequest();
        }

        partial void AddBarButton_Activated(UIBarButtonItem sender)
        {
            PerformSegue("loginAddSegue", this);
        }

        partial void SearchBarButton_Activated(UIBarButtonItem sender)
        {
            PerformSegue("loginSearchFromListSegue", this);
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            if (segue.DestinationViewController is UINavigationController navController)
            {
                if (navController.TopViewController is LoginAddViewController addLoginController)
                {
                    addLoginController.Context = Context;
                    addLoginController.LoginListController = this;
                    segue.DestinationViewController.PresentationController.Delegate =
                        new CustomPresentationControllerDelegate(addLoginController.DismissModalAction);
                }
                if (navController.TopViewController is LoginSearchViewController searchLoginController)
                {
                    searchLoginController.Context = Context;
                    searchLoginController.CPViewController = CPViewController;
                    searchLoginController.FromList = true;
                    segue.DestinationViewController.PresentationController.Delegate =
                        new CustomPresentationControllerDelegate(searchLoginController.DismissModalAction);
                }
            }
        }

        public void DismissModal()
        {
            DismissViewController(true, async () =>
            {
                await ((TableSource)TableView.Source).LoadItemsAsync();
                TableView.ReloadData();
            });
        }

        public class TableSource : ExtensionTableSource
        {
            private LoginListViewController _controller;

            public TableSource(LoginListViewController controller)
                : base(controller.Context, controller)
            {
                _controller = controller;
            }

            public async override void RowSelected(UITableView tableView, NSIndexPath indexPath)
            {
                await AutofillHelpers.TableRowSelectedAsync(tableView, indexPath, this,
                    _controller.CPViewController, _controller, _controller.PasswordRepromptService, "loginAddSegue");
            }
        }
    }
}
