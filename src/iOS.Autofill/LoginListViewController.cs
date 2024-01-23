using System;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.Core.Abstractions;
using Bit.Core.Resources.Localization;
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
        UIBarButtonItem _cancelButton;
        UIControl _accountSwitchButton;

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
            _cancelButton = new UIBarButtonItem(UIBarButtonSystemItem.Cancel, CancelButton_TouchUpInside);

            base.ViewDidLoad();

            SubscribeSyncCompleted();

            NavItem.Title = AppResources.Items;
            _cancelButton.Title = AppResources.Cancel;

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
                await ASHelpers.ReplaceAllIdentitiesAsync();
            }

            _accountSwitchingOverlayHelper = new AccountSwitchingOverlayHelper();

            _accountSwitchButton = await _accountSwitchingOverlayHelper.CreateAccountSwitchToolbarButtonItemCustomViewAsync();
            _accountSwitchButton.TouchUpInside += AccountSwitchedButton_TouchUpInside;

            NavItem.SetLeftBarButtonItems(new UIBarButtonItem[]
            {
                _cancelButton,
                new UIBarButtonItem(_accountSwitchButton)
            }, false);

            _accountSwitchingOverlayView = _accountSwitchingOverlayHelper.CreateAccountSwitchingOverlayView(OverlayView);
        }

        private void CancelButton_TouchUpInside(object sender, EventArgs e)
        {
            Cancel();
        }

        private void AccountSwitchedButton_TouchUpInside(object sender, EventArgs e)
        {
            _accountSwitchingOverlayHelper.OnToolbarItemActivated(_accountSwitchingOverlayView, OverlayView);
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

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                if (_accountSwitchButton != null)
                {
                    _accountSwitchingOverlayHelper.DisposeAccountSwitchToolbarButtonItemImage(_accountSwitchButton);

                    _accountSwitchButton.TouchUpInside -= AccountSwitchedButton_TouchUpInside;
                }
            }

            base.Dispose(disposing);
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
