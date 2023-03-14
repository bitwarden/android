using System;
using System.Threading.Tasks;
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
using CoreFoundation;
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

        LazyResolve<IBroadcasterService> _broadcasterService = new LazyResolve<IBroadcasterService>("broadcasterService");
        LazyResolve<ILogger> _logger = new LazyResolve<ILogger>("logger");
        bool _alreadyLoadItemsOnce = false;

        public async override void ViewDidLoad()
        {
            base.ViewDidLoad();

            SubscribeSyncCompleted();

            NavItem.Title = AppResources.Items;
            CancelBarButton.Title = AppResources.Cancel;

            TableView.RowHeight = UITableView.AutomaticDimension;
            TableView.EstimatedRowHeight = 44;
            TableView.BackgroundColor = ThemeHelpers.BackgroundColor;
            TableView.Source = new TableSource(this);
            await ((TableSource)TableView.Source).LoadItemsAsync();

            _alreadyLoadItemsOnce = true;

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

        async partial void RefreshControl_Activated(UIRefreshControl sender)
        {
            try
            {
                await Task.Delay(500);
                await((TableSource)TableView.Source).RefreshAsync();
            }
            finally
            {
                sender.EndRefreshing();
            }
        }

        partial void AccountSwitchingBarButton_Activated(UIBarButtonItem sender)
        {
            _accountSwitchingOverlayHelper.OnToolbarItemActivated(_accountSwitchingOverlayView, OverlayView);
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

        private void SubscribeSyncCompleted()
        {
            _broadcasterService.Value.Subscribe(nameof(LoginListViewController), message =>
            {
                if (message.Command == "syncCompleted" && _alreadyLoadItemsOnce)
                {
                    DispatchQueue.MainQueue.DispatchAsync(async () =>
                    {
                        try
                        {
                            await ((TableSource)TableView.Source).LoadItemsAsync();
                            TableView.ReloadData();
                        }
                        catch (Exception ex)
                        {
                            _logger.Value.Exception(ex);
                        }
                    });
                }
            });
        }

        public override void ViewDidUnload()
        {
            base.ViewDidUnload();

            _broadcasterService.Value.Unsubscribe(nameof(LoginListViewController));
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
