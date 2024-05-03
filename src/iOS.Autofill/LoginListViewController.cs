using System;
using System.Linq;
using System.Threading.Tasks;
using AuthenticationServices;
using Bit.App.Controls;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.Core.Utilities.Fido2;
using Bit.iOS.Autofill.ListItems;
using Bit.iOS.Autofill.Models;
using Bit.iOS.Autofill.Utilities;
using Bit.iOS.Core.Controllers;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Views;
using CoreFoundation;
using Foundation;
using Microsoft.Maui.ApplicationModel;
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
        LazyResolve<IUserVerificationMediatorService> _userVerificationMediatorService = new LazyResolve<IUserVerificationMediatorService>();
        LazyResolve<IFido2MediatorService> _fido2MediatorService = new LazyResolve<IFido2MediatorService>();

        bool _alreadyLoadItemsOnce = false;
        bool _isLoading;

        private string NavTitle
        {
            get
            {
                if (Context.IsCreatingPasskey)
                {
                    return AppResources.SavePasskey;
                }

                if (Context.IsCreatingOrPreparingListForPasskey)
                {
                    return AppResources.Autofill;
                }

                return AppResources.Items;
            }
        }

        private TableSource Source => (TableSource)TableView.Source;

        public async override void ViewDidLoad()
        {
            try
            {
                _cancelButton = new UIBarButtonItem(UIBarButtonSystemItem.Cancel, CancelButton_TouchUpInside);

                base.ViewDidLoad();

                SubscribeSyncCompleted();

                NavItem.Title = NavTitle;

                _cancelButton.Title = AppResources.Cancel;

                _searchBar.Placeholder = AppResources.Search;
                _searchBar.BackgroundColor = _searchBar.BarTintColor = ThemeHelpers.ListHeaderBackgroundColor;
                _searchBar.UpdateThemeIfNeeded();
                _searchBar.Delegate = new ExtensionSearchDelegate(TableView);

                TableView.BackgroundColor = ThemeHelpers.BackgroundColor;
                
                var tableSource = new TableSource(this);
                TableView.Source = tableSource;
                tableSource.RegisterTableViewCells(TableView);

                if (Context.IsCreatingOrPreparingListForPasskey)
                {
                    TableView.SectionHeaderHeight = 55;
                    TableView.RegisterClassForHeaderFooterViewReuse(typeof(HeaderItemView), HEADER_SECTION_IDENTIFIER);
                }

                if (UIDevice.CurrentDevice.CheckSystemVersion(15, 0))
                {
                    TableView.SectionHeaderTopPadding = 0;
                }

                if (Context.IsCreatingPasskey)
                {
                    _emptyViewLabel.Text = string.Format(AppResources.NoItemsForUri, Context.UrlString);

                    _emptyViewButton.SetTitle(AppResources.SavePasskeyAsNewLogin, UIControlState.Normal);
                    _emptyViewButton.Layer.BorderWidth = 2;
                    _emptyViewButton.Layer.BorderColor = UIColor.FromName(ColorConstants.LIGHT_TEXT_MUTED).CGColor;
                    _emptyViewButton.Layer.CornerRadius = 10;
                    _emptyViewButton.ClipsToBounds = true;
                }

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

                if (Context.IsPreparingListForPasskey)
                {
                    var fido2UserInterface = new Fido2GetAssertionFromListUserInterface(Context,
                        () => Task.CompletedTask,
                        () => Context?.VaultUnlockedDuringThisSession ?? false,
                        CPViewController.VerifyUserAsync,
                        Source.ReloadWithAllowedFido2Credentials);

                    DoFido2GetAssertionAsync(fido2UserInterface).FireAndForget(ex => ex is not Fido2AuthenticatorException);
                }
                else
                {
                    await ReloadItemsAsync();
                    _alreadyLoadItemsOnce = true;
                }
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        public async Task DoFido2GetAssertionAsync(IFido2GetAssertionUserInterface fido2GetAssertionUserInterface)
        {
            if (!UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
            {
                CPViewController.OnProvidingCredentialException(new InvalidOperationException("Trying to get assertion request before iOS 17"));
                return;
            }

            if (Context.PasskeyCredentialRequestParameters is null)
            {
                CPViewController.OnProvidingCredentialException(new InvalidOperationException("Trying to get assertion request without a PasskeyCredentialRequestParameters"));
                return;
            }

            try
            {
                var fido2AssertionResult = await _fido2MediatorService.Value.GetAssertionAsync(new Fido2AuthenticatorGetAssertionParams
                {
                    RpId = Context.PasskeyCredentialRequestParameters.RelyingPartyIdentifier,
                    Hash = Context.PasskeyCredentialRequestParameters.ClientDataHash.ToArray(),
                    UserVerificationPreference = Fido2UserVerificationPreferenceExtensions.ToFido2UserVerificationPreference(Context.PasskeyCredentialRequestParameters.UserVerificationPreference),
                    AllowCredentialDescriptorList = Context.PasskeyCredentialRequestParameters.AllowedCredentials?
                        .Select(c => new PublicKeyCredentialDescriptor { Id = c.ToArray() })
                        .ToArray()
                }, fido2GetAssertionUserInterface);

                if (fido2AssertionResult.SelectedCredential is null)
                {
                    throw new NullReferenceException("SelectedCredential must have a value");
                }

                await CPViewController.CompleteAssertionRequest(new ASPasskeyAssertionCredential(
                    NSData.FromArray(fido2AssertionResult.SelectedCredential.UserHandle),
                    Context.PasskeyCredentialRequestParameters.RelyingPartyIdentifier,
                    NSData.FromArray(fido2AssertionResult.Signature),
                    Context.PasskeyCredentialRequestParameters.ClientDataHash,
                    NSData.FromArray(fido2AssertionResult.AuthenticatorData),
                    NSData.FromArray(fido2AssertionResult.SelectedCredential.Id)
                ));
            }
            catch (InvalidOperationNeedsUIException)
            {
                return;
            }
            catch (TaskCanceledException)
            {
                return;
            }
            catch
            {
                try
                {
                    if (Context?.IsExecutingWithoutUserInteraction == false)
                    {
                        // Ideally we should inform the user an error has occurred but for the specific scenario where we have user interaction and we can fallback to password list we'll try to do that.
                        //_ = _platformUtilsService.Value.ShowDialogAsync(
                        //    string.Format(AppResources.ThereWasAProblemReadingAPasskeyForXTryAgainLater, Context.PasskeyCredentialRequestParameters.RelyingPartyIdentifier),
                        //    AppResources.ErrorReadingPasskey);

                        //Reset TableView formatting to Password and reload passwords
                        TableView.SectionHeaderHeight = 0; 
                        Context.IsPasswordFallback = true;
                        await ReloadItemsAsync();
                        _alreadyLoadItemsOnce = true;
                    }
                }
                catch (Exception ex)
                {
                    LoggerHelper.LogEvenIfCantBeResolved(ex);
                }

                throw;
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
            try
            {
                if (!Context.IsCreatingOrPreparingListForPasskey)
                {
                    PerformSegue(SegueConstants.LOGIN_SEARCH_FROM_LIST, this);
                    return;
                }

                if (_isLoading)
                {
                    // if it's loading we simplify this logic to just avoid toggling the search bar visibility
                    // and reloading items while this is taking place.
                    return;
                }

                UIView.Animate(0.3f,
                    () =>
                    {
                        _tableViewTopToSearchBarConstraint.Active = !_tableViewTopToSearchBarConstraint.Active;
                        _searchBar.Hidden = !_searchBar.Hidden;
                    },
                    () =>
                    {
                        if (_tableViewTopToSearchBarConstraint.Active)
                        {
                            _searchBar?.BecomeFirstResponder();

                            if (Context.IsCreatingPasskey)
                            {
                                _emptyView.Hidden = true;
                                TableView.Hidden = false;
                            }
                        }
                        else
                        {
                            _searchBar.Text = string.Empty;
                            _searchBar.Text = null;

                            _searchBar.ResignFirstResponder();

                            ReloadItemsAsync().FireAndForget();
                        }
                    });
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
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
                Context?.PickCredentialForFido2CreationTcs?.TrySetException(new InvalidOperationException("Trying to save passkey as new login on iOS less than 17."));
                return;
            }

            if (Context.PasskeyCreationParams is null)
            {
                Context?.PickCredentialForFido2CreationTcs?.TrySetException(new InvalidOperationException("Trying to save passkey as new login wihout creation params."));
                return;
            }

            bool? isUserVerified = null;
            if (Context?.PasskeyCreationParams?.UserVerificationPreference != Fido2UserVerificationPreference.Discouraged)
            {
                var verification = await VerifyUserAsync();
                if (verification.IsCancelled)
                {
                    return;
                }
                isUserVerified = verification.Result;

                if (!isUserVerified.Value && await _userVerificationMediatorService.Value.ShouldEnforceFido2RequiredUserVerificationAsync(Fido2UserVerificationOptions))
                {
                    await _platformUtilsService.Value.ShowDialogAsync(AppResources.ErrorCreatingPasskey, AppResources.SavePasskey);
                    return;
                }
            }

            var loadingAlert = Dialogs.CreateLoadingAlert(AppResources.Saving);
            try
            {
                PresentViewController(loadingAlert, true, null);

                var cipherId = await _cipherService.Value.CreateNewLoginForPasskeyAsync(Context.PasskeyCreationParams.Value);
                Context.PickCredentialForFido2CreationTcs.TrySetResult((cipherId, isUserVerified));
            }
            catch
            {
                await loadingAlert.DismissViewControllerAsync(false);
                throw;
            }
        }

        private async Task<CancellableResult<bool>> VerifyUserAsync()
        {
            try
            {
                if (Context?.PasskeyCreationParams is null)
                {
                    return new CancellableResult<bool>(false);
                }

                return await _userVerificationMediatorService.Value.VerifyUserForFido2Async(Fido2UserVerificationOptions);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                return new CancellableResult<bool>(false);
            }
        }

        private Fido2UserVerificationOptions Fido2UserVerificationOptions
        {
            get
            {
                ArgumentNullException.ThrowIfNull(Context);
                ArgumentNullException.ThrowIfNull(Context.PasskeyCreationParams);

                return new Fido2UserVerificationOptions
                (
                    false,
                    Context.PasskeyCreationParams.Value.UserVerificationPreference,
                    Context.VaultUnlockedDuringThisSession,
                    Context.PasskeyCredentialIdentity?.RelyingPartyIdentifier
                );
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
                            await ReloadItemsAsync();
                        }
                        catch (Exception ex)
                        {
                            _logger.Value.Exception(ex);
                        }
                    });
                }
            });
        }

        public void OnItemsLoaded(string searchFilter)
        {
            if (Context.IsCreatingPasskey)
            {
                _emptyView.Hidden = !Source.IsEmpty;
                TableView.Hidden = Source.IsEmpty;

                if (Source.IsEmpty)
                {
                    _emptyViewLabel.Text = string.Format(AppResources.NoItemsForUri, string.IsNullOrEmpty(searchFilter) ? Context.UrlString : searchFilter);
                }
            }
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
                    await ReloadItemsAsync();
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

        private async Task LoadSourceAsync()
        {
            _isLoading = true;

            try
            {
                await MainThread.InvokeOnMainThreadAsync(() =>
                {
                    TableView.Hidden = true;
                    _searchBar.Hidden = true;
                    _loadingView.Hidden = false;
                });

                await Source.LoadAsync(string.IsNullOrEmpty(_searchBar?.Text), _searchBar?.Text);

                await MainThread.InvokeOnMainThreadAsync(() =>
                {
                    _loadingView.Hidden = true;
                    TableView.Hidden = Context.IsCreatingPasskey && Source.IsEmpty;
                    _searchBar.Hidden = string.IsNullOrEmpty(_searchBar?.Text);
                });
            }
            finally
            {
                _isLoading = false;
            }
        }

        public async Task ReloadItemsAsync()
        {
            try
            {
                await LoadSourceAsync();

                _alreadyLoadItemsOnce = true;

                await MainThread.InvokeOnMainThreadAsync(TableView.ReloadData);
            }
            catch
            {
                _platformUtilsService.Value.ShowDialogAsync(AppResources.GenericErrorMessage, AppResources.AnErrorHasOccurred).FireAndForget();
                throw;
            }
        }

        public void ReloadTableViewData() => TableView.ReloadData();

        public class TableSource : BaseLoginListTableSource<LoginListViewController>
        {
            public TableSource(LoginListViewController controller)
                : base(controller)
            {
            }

            protected override string LoginAddSegue => SegueConstants.ADD_LOGIN;
        }
    }
}
