using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.Core.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
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
        //internal const string HEADER_SECTION_IDENTIFIER = "headerSectionId";

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

        LazyResolve<IBroadcasterService> _broadcasterService = new LazyResolve<IBroadcasterService>();
        LazyResolve<ICipherService> _cipherService = new LazyResolve<ICipherService>();
        LazyResolve<IPlatformUtilsService> _platformUtilsService = new LazyResolve<IPlatformUtilsService>();
        LazyResolve<ILogger> _logger = new LazyResolve<ILogger>();

        bool _alreadyLoadItemsOnce = false;

        public async override void ViewDidLoad()
        {
            _cancelButton = new UIBarButtonItem(UIBarButtonSystemItem.Cancel, CancelButton_TouchUpInside);

            base.ViewDidLoad();

            SubscribeSyncCompleted();

            NavItem.Title = Context.IsCreatingPasskey ? AppResources.SavePasskey : AppResources.Items;
            _cancelButton.Title = AppResources.Cancel;

            TableView.RowHeight = UITableView.AutomaticDimension;
            TableView.EstimatedRowHeight = 44;
            TableView.BackgroundColor = ThemeHelpers.BackgroundColor;
            TableView.Source = new TableSource(this);
            //TableView.RegisterClassForHeaderFooterViewReuse(typeof(AccountViewCell), HEADER_SECTION_IDENTIFIER);

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
            ClipLogger.Log($"EmptyButton_Activated");
            SavePasskeyAsNewLoginAsync().FireAndForget(ex =>
            {
                _platformUtilsService.Value.ShowDialogAsync(AppResources.GenericErrorMessage, AppResources.AnErrorHasOccurred).FireAndForget();
            });
        }

        private async Task SavePasskeyAsNewLoginAsync()
        {
            if (!UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
            {
                Context?.ConfirmNewCredentialTcs?.TrySetException(new InvalidOperationException("Trying to save passkey as new login on iOS less than 17."));
                return;
            }

            ClipLogger.Log($"SavePasskeyAsNewLoginAsync ");

            var cipherId = await _cipherService.Value.CreateNewLoginForPasskeyAsync(Context.PasskeyCredentialIdentity.RelyingPartyIdentifier);

            ClipLogger.Log($"SavePasskeyAsNewLoginAsync -> setting result {cipherId}");
            Context.ConfirmNewCredentialTcs.TrySetResult(new Fido2ConfirmNewCredentialResult
            {
                CipherId = cipherId,
                UserVerified = true
            });
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
            ClipLogger.Log($"OnEmptyList");
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

        public class TableSource : ExtensionTableSource
        {
            private readonly LoginListViewController _controller;

            private readonly LazyResolve<IPlatformUtilsService> _platformUtilsService = new LazyResolve<IPlatformUtilsService>();
            private readonly LazyResolve<IPasswordRepromptService> _passwordRepromptService = new LazyResolve<IPasswordRepromptService>();

            public TableSource(LoginListViewController controller)
                : base(controller.Context, controller)
            {
                _controller = controller;
            }

            private Context Context => (Context)_context;

            //protected override async Task<IEnumerable<CipherViewModel>> LoadItemsAsync(bool urlFilter = true, string searchFilter = null)
            //{
            //    if (!Context.IsCreatingPasskey)
            //    {
            //        return await base.LoadItemsAsync(urlFilter, searchFilter);
            //    }


            //}

            public override async Task LoadAsync(bool urlFilter = true, string searchFilter = null)
            {
                await base.LoadAsync(urlFilter, searchFilter);

                if (Context.IsCreatingPasskey && !Items.Any())
                {
                    _controller?.OnEmptyList();
                }
            }

            //public override nint NumberOfSections(UITableView tableView)
            //{
            //    return Context.IsCreatingPasskey ? 1 : 0;
            //}

            //public override UIView GetViewForHeader(UITableView tableView, nint section)
            //{
            //    if (Context.IsCreatingPasskey)
            //    {
            //        var view = tableView.DequeueReusableHeaderFooterView(LoginListViewController.HEADER_SECTION_IDENTIFIER);

            //        return view;
            //    }

            //    return base.GetViewForHeader(tableView, section);
            //}

            public override nint RowsInSection(UITableView tableview, nint section)
            {
                if (Context.IsCreatingPasskey)
                {
                    return Items?.Count() ?? 0;
                }

                return base.RowsInSection(tableview, section);
            }

            public async override void RowSelected(UITableView tableView, NSIndexPath indexPath)
            {
                if (Context.IsCreatingPasskey)
                {
                    await SelectRowForPasskeyCreationAsync(tableView, indexPath);
                    return;
                }

                await AutofillHelpers.TableRowSelectedAsync(tableView, indexPath, this,
                    _controller.CPViewController, _controller, _controller.PasswordRepromptService, "loginAddSegue");
            }

            private async Task SelectRowForPasskeyCreationAsync(UITableView tableView, NSIndexPath indexPath)
            {
                ClipLogger.Log($"SelectRowForPasskeyCreationAsync");

                tableView.DeselectRow(indexPath, true);
                tableView.EndEditing(true);

                var item = Items.ElementAt(indexPath.Row);
                if (item is null)
                {
                    ClipLogger.Log($"SelectRowForPasskeyCreationAsync -> item is null");
                    await _platformUtilsService.Value.ShowDialogAsync(AppResources.GenericErrorMessage, AppResources.AnErrorHasOccurred);
                    return;
                }

                if (item.CipherView.Login.HasFido2Credentials
                    &&
                    !await _platformUtilsService.Value.ShowDialogAsync(
                        AppResources.ThisItemAlreadyContainsAPasskeyAreYouSureYouWantToOverwriteTheCurrentPasskey,
                        AppResources.OverwritePasskey,
                        AppResources.Yes,
                        AppResources.No))
                {
                    ClipLogger.Log($"SelectRowForPasskeyCreationAsync -> don't want to overwrite");
                    return;
                }

                if (!await _passwordRepromptService.Value.PromptAndCheckPasswordIfNeededAsync(item.Reprompt))
                {
                    ClipLogger.Log($"SelectRowForPasskeyCreationAsync -> PromptAndCheckPasswordIfNeededAsync -> false");
                    return;
                }

                // TODO: Check user verification

                ClipLogger.Log($"SelectRowForPasskeyCreationAsync -> Setting result {item.Id}");
                Context.ConfirmNewCredentialTcs.SetResult(new Fido2ConfirmNewCredentialResult
                {
                    CipherId = item.Id,
                    UserVerified = true
                });
            }
        }
    }
}
