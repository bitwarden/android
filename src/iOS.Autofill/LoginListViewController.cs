using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Controls;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.iOS.Autofill.ListItems;
using Bit.iOS.Autofill.Models;
using Bit.iOS.Autofill.Utilities;
using Bit.iOS.Core.Controllers;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Views;
using CoreFoundation;
using CoreGraphics;
using Foundation;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class LoginListViewController : ExtendedUIViewController, ILoginListViewController
    {
        internal const string HEADER_SECTION_IDENTIFIER = "headerSectionId";

        UIBarButtonItem _cancelButton;
        UIControl _accountSwitchButton;

        public LoginListViewController(IntPtr handle)
            : base(handle)
        {
            DismissModalAction = Cancel;
        }

        public Context Context { get; set; }
        public CredentialProviderViewController CPViewController { get; set; }

        AccountSwitchingOverlayView _accountSwitchingOverlayView;
        AccountSwitchingOverlayHelper _accountSwitchingOverlayHelper;

        LazyResolve<IBroadcasterService> _broadcasterService = new LazyResolve<IBroadcasterService>();
        LazyResolve<ICipherService> _cipherService = new LazyResolve<ICipherService>();
        LazyResolve<IPlatformUtilsService> _platformUtilsService = new LazyResolve<IPlatformUtilsService>();
        LazyResolve<ILogger> _logger = new LazyResolve<ILogger>();

        bool _alreadyLoadItemsOnce = false;

        public async override void ViewDidLoad()
        {
            try
            {
                _cancelButton = new UIBarButtonItem(UIBarButtonSystemItem.Cancel, CancelButton_TouchUpInside);

                base.ViewDidLoad();

                SubscribeSyncCompleted();

                NavItem.Title = Context.IsCreatingPasskey ? AppResources.SavePasskey : AppResources.Items;
                _cancelButton.Title = AppResources.Cancel;

                TableView.BackgroundColor = ThemeHelpers.BackgroundColor;
                
                var tableSource = new TableSource(this);
                TableView.Source = tableSource;
                tableSource.RegisterTableViewCells(TableView);

                if (Context.IsCreatingPasskey)
                {
                    TableView.SectionHeaderHeight = 55;
                    TableView.RegisterClassForHeaderFooterViewReuse(typeof(HeaderItemView), HEADER_SECTION_IDENTIFIER);
                }

                if (UIDevice.CurrentDevice.CheckSystemVersion(15, 0))
                {
                    TableView.SectionHeaderTopPadding = 0;
                }

                await ((TableSource)TableView.Source).LoadAsync();

                if (Context.IsCreatingPasskey)
                {
                    _headerLabel.Text = AppResources.ChooseALoginToSaveThisPasskeyTo;
                    _emptyViewLabel.Text = string.Format(AppResources.NoItemsForUri, Context.UrlString);

                    _emptyViewButton.SetTitle(AppResources.SavePasskeyAsNewLogin, UIControlState.Normal);
                    _emptyViewButton.Layer.BorderWidth = 2;
                    _emptyViewButton.Layer.BorderColor = UIColor.FromName(ColorConstants.LIGHT_TEXT_MUTED).CGColor;
                    _emptyViewButton.Layer.CornerRadius = 10;
                    _emptyViewButton.ClipsToBounds = true;

                    _headerView.Hidden = false;
                }

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
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
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
            CPViewController.CancelRequest(AuthenticationServices.ASExtensionErrorCode.UserCanceled);
        }

        partial void AddBarButton_Activated(UIBarButtonItem sender)
        {
            PerformSegue(SegueConstants.ADD_LOGIN, this);
        }

        partial void SearchBarButton_Activated(UIBarButtonItem sender)
        {
            PerformSegue(SegueConstants.LOGIN_SEARCH_FROM_LIST, this);
        }

        partial void EmptyButton_Activated(UIButton sender)
        {
            SavePasskeyAsNewLoginAsync().FireAndForget(ex =>
            {
                var message = AppResources.AnErrorHasOccurred;
                if (ex is ApiException apiEx && apiEx.Error != null)
                {
                    message = apiEx.Error.GetSingleMessage();
                }

                _platformUtilsService.Value.ShowDialogAsync(AppResources.GenericErrorMessage, message).FireAndForget();
            });
        }

        private async Task SavePasskeyAsNewLoginAsync()
        {
            if (!UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
            {
                Context?.ConfirmNewCredentialTcs?.TrySetException(new InvalidOperationException("Trying to save passkey as new login on iOS less than 17."));
                return;
            }

            if (Context.PasskeyCreationParams is null)
            {
                Context?.ConfirmNewCredentialTcs?.TrySetException(new InvalidOperationException("Trying to save passkey as new login wihout creation params."));
                return;
            }

            var loadingAlert = Dialogs.CreateLoadingAlert(AppResources.Saving);

            try
            {
                PresentViewController(loadingAlert, true, null);

                var cipherId = await _cipherService.Value.CreateNewLoginForPasskeyAsync(Context.PasskeyCreationParams.Value);
                Context.ConfirmNewCredentialTcs.TrySetResult((cipherId, true));
            }
            catch
            {
                await loadingAlert.DismissViewControllerAsync(false);
                throw;
            }
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
                            await ((TableSource)TableView.Source).LoadAsync();
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

        public void OnEmptyList()
        {
            _emptyView.Hidden = false;
            _headerView.Hidden = false;
            TableView.Hidden = true;
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
                try
                {
                    await ((TableSource)TableView.Source).LoadAsync();
                    TableView.ReloadData();
                }
                catch (Exception ex)
                {
                    _logger.Value.Exception(ex);
                }
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

        public class TableSource : BaseLoginListTableSource<LoginListViewController>
        {
            public TableSource(LoginListViewController controller)
                : base(controller)
            {
            }

            protected override string LoginAddSegue => SegueConstants.ADD_LOGIN;

            public override async Task LoadAsync(bool urlFilter = true, string searchFilter = null)
            {
                try
                {
                    await base.LoadAsync(urlFilter, searchFilter);

                    if (Context.IsCreatingPasskey && !Items.Any())
                    {
                        Controller?.OnEmptyList();
                    }
                }
                catch (Exception ex)
                {
                    LoggerHelper.LogEvenIfCantBeResolved(ex);
                }
            }

            public override UIView GetViewForHeader(UITableView tableView, nint section)
            {
                try
                {
                    if (Context.IsCreatingPasskey
                        &&
                        tableView.DequeueReusableHeaderFooterView(LoginListViewController.HEADER_SECTION_IDENTIFIER) is HeaderItemView headerItemView)
                    {
                        headerItemView.SetHeaderText(AppResources.ChooseALoginToSaveThisPasskeyTo);
                        return headerItemView;
                    }

                    return new UIView(CGRect.Empty);
                }
                catch (Exception ex)
                {
                    LoggerHelper.LogEvenIfCantBeResolved(ex);
                    return new UIView();
                }
            }

            public override nint RowsInSection(UITableView tableview, nint section)
            {
                if (Context.IsCreatingPasskey)
                {
                    return Items?.Count() ?? 0;
                }

                return base.RowsInSection(tableview, section);
            }
        }
    }
}
