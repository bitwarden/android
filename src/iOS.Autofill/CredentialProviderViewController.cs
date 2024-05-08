using System;
using System.Threading.Tasks;
using AuthenticationServices;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Pages;
using Bit.App.Utilities;
using Bit.App.Utilities.AccountManagement;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.Core.Utilities.Fido2;
using Bit.iOS.Autofill.Models;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Views;
using CoreFoundation;
using CoreNFC;
using Foundation;
using Microsoft.Maui.ApplicationModel;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Platform;
using ObjCRuntime;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class CredentialProviderViewController : ASCredentialProviderViewController, IAccountsManagerHost
    {
        private Context _context;
        private NFCNdefReaderSession _nfcSession = null;
        private Core.NFCReaderDelegate _nfcDelegate = null;
        private IAccountsManager _accountsManager;

        private readonly LazyResolve<IStateService> _stateService = new LazyResolve<IStateService>();
        private readonly LazyResolve<IConditionedAwaiterManager> _conditionedAwaiterManager = new LazyResolve<IConditionedAwaiterManager>();
        private readonly LazyResolve<IBroadcasterService> _broadcasterService = new LazyResolve<IBroadcasterService>();
        private readonly LazyResolve<IVaultTimeoutService> _vaultTimeoutService = new LazyResolve<IVaultTimeoutService>();

        public CredentialProviderViewController(IntPtr handle)
            : base(handle)
        {
            ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
        }

        private ASCredentialProviderExtensionContext ASExtensionContext => _context?.ExtContext as ASCredentialProviderExtensionContext;

        public override void ViewDidLoad()
        {
            try
            {
                InitAppIfNeededAsync().FireAndForget(ex => OnProvidingCredentialException(ex));

                base.ViewDidLoad();

                Logo.Image = new UIImage(ThemeHelpers.LightTheme ? "logo.png" : "logo_white.png");
                View.BackgroundColor = ThemeHelpers.SplashBackgroundColor;

                _context = new Context
                {
                    ExtContext = ExtensionContext
                };

                _conditionedAwaiterManager.Value.Recreate(AwaiterPrecondition.AutofillIOSExtensionViewDidAppear);
            }
            catch (Exception ex)
            {
                OnProvidingCredentialException(ex);
            }
        }

        public override void ViewDidAppear(bool animated)
        {
            base.ViewDidAppear(animated);

            _conditionedAwaiterManager.Value.SetAsCompleted(AwaiterPrecondition.AutofillIOSExtensionViewDidAppear);
        }

        public override async void PrepareCredentialList(ASCredentialServiceIdentifier[] serviceIdentifiers)
        {
            try
            {
                await InitAppIfNeededAsync();
                _context.ServiceIdentifiers = serviceIdentifiers;
                if (serviceIdentifiers.Length > 0)
                {
                    var uri = serviceIdentifiers[0].Identifier;
                    if (serviceIdentifiers[0].Type == ASCredentialServiceIdentifierType.Domain)
                    {
                        uri = string.Concat("https://", uri);
                    }
                    _context.UrlString = uri;
                }
                if (!await IsAuthed())
                {
                    await _accountsManager.NavigateOnAccountChangeAsync(false);
                }
                else if (await IsLocked())
                {
                    PerformSegue(SegueConstants.LOCK, this);
                }
                else
                {
                    if (_context.IsCreatingOrPreparingListForPasskey || _context.ServiceIdentifiers?.Length > 0)
                    {
                        PerformSegue(SegueConstants.LOGIN_LIST, this);
                    }
                    else
                    {
                        PerformSegue(SegueConstants.LOGIN_SEARCH, this);
                    }
                }
            }
            catch (Exception ex)
            {
                OnProvidingCredentialException(ex);
            }
        }

        [Export("provideCredentialWithoutUserInteractionForRequest:")]
        public override async void ProvideCredentialWithoutUserInteraction(IASCredentialRequest credentialRequest)
        {
            if (!UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
            {
                return;
            }

            _context.VaultUnlockedDuringThisSession = false;
            _context.IsExecutingWithoutUserInteraction = true;

            try
            {
                switch (credentialRequest?.Type)
                {
                    case ASCredentialRequestType.Password:
                        var passwordCredentialIdentity = Runtime.GetNSObject<ASPasswordCredentialIdentity>(credentialRequest.CredentialIdentity.GetHandle());
                        await ProvideCredentialWithoutUserInteractionAsync(passwordCredentialIdentity);
                        break;
                    case ASCredentialRequestType.PasskeyAssertion:
                        var asPasskeyCredentialRequest = Runtime.GetNSObject<ASPasskeyCredentialRequest>(credentialRequest.GetHandle());
                        await ProvideCredentialWithoutUserInteractionAsync(asPasskeyCredentialRequest);
                        break;
                    default:
                        CancelRequest(ASExtensionErrorCode.Failed);
                        break;
                }
            }
            catch (Exception ex)
            {
                OnProvidingCredentialException(ex);
            }
        }

        //public override async void ProvideCredentialWithoutUserInteraction(ASPasswordCredentialIdentity credentialIdentity)
        //{
        //    try
        //    {
        //        await ProvideCredentialWithoutUserInteractionAsync(credentialIdentity);
        //    }
        //    catch (Exception ex)
        //    {
        //        OnProvidingCredentialException(ex);
        //    }
        //}

        [Export("prepareInterfaceToProvideCredentialForRequest:")]
        public override async void PrepareInterfaceToProvideCredential(IASCredentialRequest credentialRequest)
        {
            if (!UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
            {
                return;
            }

            _context.VaultUnlockedDuringThisSession = false;

            try
            {
                switch (credentialRequest?.Type)
                {
                    case ASCredentialRequestType.Password:
                        var passwordCredentialIdentity = Runtime.GetNSObject<ASPasswordCredentialIdentity>(credentialRequest.CredentialIdentity.GetHandle());
                        await PrepareInterfaceToProvideCredentialAsync(c => c.PasswordCredentialIdentity = passwordCredentialIdentity);
                        break;
                    case ASCredentialRequestType.PasskeyAssertion:
                        var asPasskeyCredentialRequest = Runtime.GetNSObject<ASPasskeyCredentialRequest>(credentialRequest.GetHandle());
                        await PrepareInterfaceToProvideCredentialAsync(c => c.PasskeyCredentialRequest = asPasskeyCredentialRequest);
                        break;
                    default:
                        CancelRequest(ASExtensionErrorCode.Failed);
                        break;
                }
            }
            catch (Exception ex)
            {
                OnProvidingCredentialException(ex);
            }
        }

        //public override async void PrepareInterfaceToProvideCredential(ASPasswordCredentialIdentity credentialIdentity)
        //{
        //    try
        //    {
        //        await PrepareInterfaceToProvideCredentialAsync(c => c.PasswordCredentialIdentity = credentialIdentity);
        //    }
        //    catch (Exception ex)
        //    {
        //        OnProvidingCredentialException(ex);
        //    }
        //}

        public override async void PrepareInterfaceForExtensionConfiguration()
        {
            try
            {
                await InitAppIfNeededAsync();
                _context.Configuring = true;
                _context.VaultUnlockedDuringThisSession = false;

                if (!await IsAuthed())
                {
                    await _accountsManager.NavigateOnAccountChangeAsync(false);
                    return;
                }
                await CheckLockAsync(() => PerformSegue("setupSegue", this));
            }
            catch (Exception ex)
            {
                OnProvidingCredentialException(ex);
            }
        }
        
        private async Task ProvideCredentialWithoutUserInteractionAsync(ASPasswordCredentialIdentity credentialIdentity)
        {
            await InitAppIfNeededAsync();
            await _stateService.Value.SetPasswordRepromptAutofillAsync(false);
            await _stateService.Value.SetPasswordVerifiedAutofillAsync(false);
            if (!await IsAuthed() || await IsLocked())
            {
                var err = new NSError(new NSString("ASExtensionErrorDomain"),
                    Convert.ToInt32(ASExtensionErrorCode.UserInteractionRequired), null);
                ExtensionContext.CancelRequest(err);
                return;
            }
            _context.PasswordCredentialIdentity = credentialIdentity;
            await ProvideCredentialAsync(false);
        }

        private async Task PrepareInterfaceToProvideCredentialAsync(Action<Context> updateContext)
        {
            await InitAppIfNeededAsync();
            if (!await IsAuthed())
            {
                await _accountsManager.NavigateOnAccountChangeAsync(false);
                return;
            }
            updateContext(_context);
            await CheckLockAsync(async () => await ProvideCredentialAsync());
        }

        public void CompleteRequest(string id = null, string username = null,
            string password = null, string totp = null)
        {
            if ((_context?.Configuring ?? true) && string.IsNullOrWhiteSpace(password))
            {
                ServiceContainer.Reset();
                ASExtensionContext?.CompleteExtensionConfigurationRequest();
                return;
            }

            if (_context == null || string.IsNullOrWhiteSpace(username) || string.IsNullOrWhiteSpace(password))
            {
                ServiceContainer.Reset();
                var err = new NSError(new NSString("ASExtensionErrorDomain"),
                    Convert.ToInt32(ASExtensionErrorCode.UserCanceled), null);
                NSRunLoop.Main.BeginInvokeOnMainThread(() => ASExtensionContext?.CancelRequest(err));
                return;
            }

            _context.PickCredentialForFido2GetAssertionFromListTcs?.TrySetCanceled();

            if (!string.IsNullOrWhiteSpace(totp))
            {
                UIPasteboard.General.String = totp;
            }

            var cred = new ASPasswordCredential(username, password);
            NSRunLoop.Main.BeginInvokeOnMainThread(async () =>
            {
                if (!string.IsNullOrWhiteSpace(id))
                {
                    var eventService = ServiceContainer.Resolve<IEventService>("eventService");
                    await eventService.CollectAsync(Bit.Core.Enums.EventType.Cipher_ClientAutofilled, id);
                }
                ServiceContainer.Reset();
                ASExtensionContext?.CompleteRequest(cred, null);
            });
        }

        internal void OnProvidingCredentialException(Exception ex)
        {
            LoggerHelper.LogEvenIfCantBeResolved(ex);
            CancelRequest(ASExtensionErrorCode.Failed);
        }

        public void CancelRequest(ASExtensionErrorCode code)
        {
            if (_context?.IsPasskey == true)
            {
                _context.PickCredentialForFido2CreationTcs?.TrySetCanceled();
                _context.UnlockVaultTcs?.TrySetCanceled();
            }

            //var err = new NSError(new NSString("ASExtensionErrorDomain"), Convert.ToInt32(code), null);
            var err = new NSError(ASExtensionErrorCodeExtensions.GetDomain(code), (int)code);
            ExtensionContext?.CancelRequest(err);
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            try
            {
                if (segue.DestinationViewController is UINavigationController navController)
                {
                    if (navController.TopViewController is LoginListViewController listLoginController)
                    {
                        listLoginController.Context = _context;
                        listLoginController.CPViewController = this;
                        segue.DestinationViewController.PresentationController.Delegate =
                            new CustomPresentationControllerDelegate(listLoginController.DismissModalAction);
                    }
                    else if (navController.TopViewController is LoginSearchViewController listSearchController)
                    {
                        listSearchController.Context = _context;
                        listSearchController.CPViewController = this;
                        segue.DestinationViewController.PresentationController.Delegate =
                            new CustomPresentationControllerDelegate(listSearchController.DismissModalAction);
                    }
                    else if (navController.TopViewController is LockPasswordViewController passwordViewController)
                    {
                        passwordViewController.CPViewController = this;
                        passwordViewController.LaunchHomePage = () => DismissViewController(false, () => LaunchHomePage());
                        segue.DestinationViewController.PresentationController.Delegate =
                            new CustomPresentationControllerDelegate(passwordViewController.DismissModalAction);
                    }
                    else if (navController.TopViewController is SetupViewController setupViewController)
                    {
                        setupViewController.CPViewController = this;
                        segue.DestinationViewController.PresentationController.Delegate =
                            new CustomPresentationControllerDelegate(setupViewController.DismissModalAction);
                    }
                }

            }
            catch (Exception ex)
            {
                OnProvidingCredentialException(ex);
            }
        }

        public void DismissLockAndContinue()
        {
            DismissViewController(false, async () => await OnLockDismissedAsync());
        }

        private void NavigateToPage(ContentPage page)
        {
            var navigationPage = new NavigationPage(page);
            var uiController = navigationPage.ToUIViewController(MauiContextSingleton.Instance.MauiContext);
            uiController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;

            PresentViewController(uiController, true, null);
        }

        public async Task OnLockDismissedAsync()
        {
            try
            {
                _context.VaultUnlockedDuringThisSession = true;

                if (_context.IsCreatingPasskey)
                {
                    _context.UnlockVaultTcs.TrySetResult(true);
                    return;
                }

                if (_context.PasswordCredentialIdentity != null || _context.IsPasskey)
                {
                    await MainThread.InvokeOnMainThreadAsync(() => ProvideCredentialAsync());
                    return;
                }
                if (_context.Configuring)
                {
                    await MainThread.InvokeOnMainThreadAsync(() => PerformSegue("setupSegue", this));
                    return;
                }

                if (_context.ServiceIdentifiers == null || _context.ServiceIdentifiers.Length == 0)
                {
                    await MainThread.InvokeOnMainThreadAsync(() => PerformSegue("loginSearchSegue", this));
                }
                else
                {
                    await MainThread.InvokeOnMainThreadAsync(() => PerformSegue("loginListSegue", this));
                }
            }
            catch (Exception ex)
            {
                OnProvidingCredentialException(ex);
            }
        }

        private async Task ProvideCredentialAsync(bool userInteraction = true)
        {
            try
            {
                if (_context.IsPasskey && UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
                {
                    if (_context.PasskeyCredentialIdentity is null)
                    {
                        CancelRequest(ASExtensionErrorCode.Failed);
                    }

                    await CompleteAssertionRequestAsync(_context.PasskeyCredentialIdentity.RelyingPartyIdentifier,
                        _context.PasskeyCredentialIdentity.UserHandle,
                        _context.PasskeyCredentialIdentity.CredentialId,
                        _context.RecordIdentifier);
                    return;
                }

                if (!ServiceContainer.TryResolve<ICipherService>(out var cipherService)
                    ||
                    _context.RecordIdentifier == null)
                {
                    CancelRequest(ASExtensionErrorCode.CredentialIdentityNotFound);
                    return;
                }

                var cipher = await cipherService.GetAsync(_context.RecordIdentifier);
                if (cipher?.Login is null || cipher.Type != CipherType.Login)
                {
                    CancelRequest(ASExtensionErrorCode.CredentialIdentityNotFound);
                    return;
                }

                if (_context.IsPasskey)
                {
                    // this shouldn't happen but as a safeguard we've set it here:
                    // if somehow the flow got into here then it's impossible to find the credential identity
                    // i.e. if on iOS < 17 and somehow there is a PasskeyCredentialRequest that was passed along in the iOS callbacks
                    CancelRequest(ASExtensionErrorCode.CredentialIdentityNotFound);
                    return;
                }

                var decCipher = await cipher.DecryptAsync();

                if (decCipher.Reprompt != CipherRepromptType.None)
                {
                    // Prompt for password using either the lock screen or dialog unless
                    // already verified the password.
                    if (!userInteraction)
                    {
                        await _stateService.Value.SetPasswordRepromptAutofillAsync(true);
                        CancelRequest(ASExtensionErrorCode.UserInteractionRequired);
                        return;
                    }

                    if (!await _stateService.Value.GetPasswordVerifiedAutofillAsync())
                    {
                        // Add a timeout to resolve keyboard not always showing up.
                        await Task.Delay(250);
                        var passwordRepromptService = ServiceContainer.Resolve<IPasswordRepromptService>();
                        if (!await passwordRepromptService.PromptAndCheckPasswordIfNeededAsync())
                        {
                            CancelRequest(ASExtensionErrorCode.UserCanceled);
                            return;
                        }
                    }
                }

                string totpCode = null;
                if (await _stateService.Value.GetDisableAutoTotpCopyAsync() != true)
                {
                    if (!string.IsNullOrWhiteSpace(decCipher.Login.Totp)
                        &&
                        (cipher.OrganizationUseTotp || await _stateService.Value.CanAccessPremiumAsync()))
                    {
                        totpCode = await ServiceContainer.Resolve<ITotpService>().GetCodeAsync(decCipher.Login.Totp);
                    }
                }

                CompleteRequest(decCipher.Id, decCipher.Login.Username, decCipher.Login.Password, totpCode);
            }
            catch (Fido2AuthenticatorException)
            {
                CancelRequest(ASExtensionErrorCode.Failed);
            }
            catch (Exception ex)
            {
                OnProvidingCredentialException(ex);
            }
        }

        private async Task CheckLockAsync(Action notLockedAction)
        {
            if (await IsLocked() || await _stateService.Value.GetPasswordRepromptAutofillAsync())
            {
                DispatchQueue.MainQueue.DispatchAsync(() => PerformSegue("lockPasswordSegue", this));
            }
            else
            {
                notLockedAction();
            }
        }

        private Task<bool> IsLocked()
        {
            return _vaultTimeoutService.Value.IsLockedAsync();
        }

        private Task<bool> IsAuthed()
        {
            return _stateService.Value.IsAuthenticatedAsync();
        }

        private void LogoutIfAuthed()
        {
            NSRunLoop.Main.BeginInvokeOnMainThread(async () =>
            {
                try
                {
                    if (await IsAuthed())
                    {
                        await AppHelpers.LogOutAsync(await _stateService.Value.GetActiveUserIdAsync());
                        if (UIDevice.CurrentDevice.CheckSystemVersion(12, 0))
                        {
                            await ASCredentialIdentityStore.SharedStore?.RemoveAllCredentialIdentitiesAsync();
                        }
                    }
                }
                catch (Exception ex)
                {
                    LoggerHelper.LogEvenIfCantBeResolved(ex);
                }
            });
        }

        private void InitApp()
        {
            iOSCoreHelpers.InitApp(this, Bit.Core.Constants.iOSAutoFillClearCiphersCacheKey,
                _nfcSession, out _nfcDelegate, out _accountsManager);

            _broadcasterService.Value.Subscribe(nameof(CredentialProviderViewController), OnMessageReceived);
        }

        private async Task InitAppIfNeededAsync()
        {
            if (ServiceContainer.RegisteredServices == null || ServiceContainer.RegisteredServices.Count == 0)
            {
                await MainThread.InvokeOnMainThreadAsync(InitApp);
            }

            await _stateService.Value.ReloadStateAsync();
        }

        private void OnMessageReceived(Message message)
        {
            if (message?.Command == AccountsManagerMessageCommands.ACCOUNT_SWITCH_COMPLETED
                &&
                _context != null)
            {
                _context.VaultUnlockedDuringThisSession = false;
                _context.PickCredentialForFido2CreationTcs?.TrySetException(new AccountSwitchedException());
                _context.PickCredentialForFido2GetAssertionFromListTcs?.TrySetException(new AccountSwitchedException());
            }
        }

        private void LaunchHomePage()
        {
            var appOptions = new AppOptions { IosExtension = true };
            var homePage = new HomePage(appOptions);
            var app = new App.App(appOptions);
            ThemeManager.SetTheme(app.Resources);
            ThemeManager.ApplyResourcesTo(homePage);
            if (homePage.BindingContext is HomeViewModel vm)
            {
                vm.StartLoginAction = () => DismissViewController(false, () => LaunchLoginFlow(vm.Email));
                vm.StartRegisterAction = () => DismissViewController(false, () => LaunchRegisterFlow());
                vm.StartSsoLoginAction = () => DismissViewController(false, () => LaunchLoginSsoFlow());
                vm.StartEnvironmentAction = () => DismissViewController(false, () => LaunchEnvironmentFlow());
                vm.CloseAction = () => CompleteRequest();
            }

            NavigateToPage(homePage);

            LogoutIfAuthed();
        }

        private void LaunchEnvironmentFlow()
        {
            var environmentPage = new EnvironmentPage();
            var app = new App.App(new AppOptions { IosExtension = true });
            ThemeManager.SetTheme(app.Resources);
            ThemeManager.ApplyResourcesTo(environmentPage);
            if (environmentPage.BindingContext is EnvironmentPageViewModel vm)
            {
                vm.SubmitSuccessTask = async () =>
                {
                    await DismissViewControllerAsync(false);
                    await MainThread.InvokeOnMainThreadAsync(() => LaunchHomePage());
                };
                vm.CloseAction = () => DismissViewController(false, () => LaunchHomePage());
            }

            NavigateToPage(environmentPage);
        }

        private void LaunchRegisterFlow()
        {
            var registerPage = new RegisterPage(null);
            var app = new App.App(new AppOptions { IosExtension = true });
            ThemeManager.SetTheme(app.Resources);
            ThemeManager.ApplyResourcesTo(registerPage);
            if (registerPage.BindingContext is RegisterPageViewModel vm)
            {
                vm.RegistrationSuccess = () => DismissViewController(false, () => LaunchLoginFlow(vm.Email));
                vm.CloseAction = () => DismissViewController(false, () => LaunchHomePage());
            }

            NavigateToPage(registerPage);
        }

        private void LaunchLoginFlow(string email = null)
        {
            var appOptions = new AppOptions { IosExtension = true };
            var app = new App.App(appOptions);
            var loginPage = new LoginPage(email, appOptions);
            ThemeManager.SetTheme(app.Resources);
            ThemeManager.ApplyResourcesTo(loginPage);
            if (loginPage.BindingContext is LoginPageViewModel vm)
            {
                vm.StartTwoFactorAction = () => DismissViewController(false, () => LaunchTwoFactorFlow(false));
                vm.UpdateTempPasswordAction = () => DismissViewController(false, () => LaunchUpdateTempPasswordFlow());
                vm.StartSsoLoginAction = () => DismissViewController(false, () => LaunchLoginSsoFlow());
                vm.LogInWithDeviceAction = () => DismissViewController(false, () => LaunchLoginWithDevice(AuthRequestType.AuthenticateAndUnlock, email));
                vm.LogInSuccessAction = () => DismissLockAndContinue();
                vm.CloseAction = () => DismissViewController(false, () => LaunchHomePage());
            }

            NavigateToPage(loginPage);

            LogoutIfAuthed();
        }

        private void LaunchLoginWithDevice(AuthRequestType authRequestType, string email = null, bool authingWithSso = false)
        {
            var appOptions = new AppOptions { IosExtension = true };
            var app = new App.App(appOptions);
            var loginWithDevicePage = new LoginPasswordlessRequestPage(email, authRequestType, appOptions, authingWithSso);
            ThemeManager.SetTheme(app.Resources);
            ThemeManager.ApplyResourcesTo(loginWithDevicePage);
            if (loginWithDevicePage.BindingContext is LoginPasswordlessRequestViewModel vm)
            {
                vm.StartTwoFactorAction = () => DismissViewController(false, () => LaunchTwoFactorFlow(false));
                vm.UpdateTempPasswordAction = () => DismissViewController(false, () => LaunchUpdateTempPasswordFlow());
                vm.LogInSuccessAction = () => DismissLockAndContinue();
                vm.CloseAction = () => DismissViewController(false, () => LaunchHomePage());
            }

            NavigateToPage(loginWithDevicePage);

            LogoutIfAuthed();
        }

        private void LaunchLoginSsoFlow()
        {
            var appOptions = new AppOptions { IosExtension = true };
            var loginPage = new LoginSsoPage(appOptions);
            var app = new App.App(appOptions);
            ThemeManager.SetTheme(app.Resources);
            ThemeManager.ApplyResourcesTo(loginPage);
            if (loginPage.BindingContext is LoginSsoPageViewModel vm)
            {
                vm.StartTwoFactorAction = () => DismissViewController(false, () => LaunchTwoFactorFlow(true));
                vm.StartSetPasswordAction = () => DismissViewController(false, () => LaunchSetPasswordFlow());
                vm.UpdateTempPasswordAction = () => DismissViewController(false, () => LaunchUpdateTempPasswordFlow());
                vm.StartDeviceApprovalOptionsAction = () => DismissViewController(false, () => LaunchDeviceApprovalOptionsFlow());
                vm.SsoAuthSuccessAction = () => DismissLockAndContinue();
                vm.CloseAction = () => DismissViewController(false, () => LaunchHomePage());
            }

            NavigateToPage(loginPage);

            LogoutIfAuthed();
        }

        private void LaunchTwoFactorFlow(bool authingWithSso)
        {
            var twoFactorPage = new TwoFactorPage(authingWithSso);
            var app = new App.App(new AppOptions { IosExtension = true });
            ThemeManager.SetTheme(app.Resources);
            ThemeManager.ApplyResourcesTo(twoFactorPage);
            if (twoFactorPage.BindingContext is TwoFactorPageViewModel vm)
            {
                vm.TwoFactorAuthSuccessAction = () => DismissLockAndContinue();
                vm.StartSetPasswordAction = () => DismissViewController(false, () => LaunchSetPasswordFlow());
                vm.StartDeviceApprovalOptionsAction = () => DismissViewController(false, () => LaunchDeviceApprovalOptionsFlow());
                if (authingWithSso)
                {
                    vm.CloseAction = () => DismissViewController(false, () => LaunchLoginSsoFlow());
                }
                else
                {
                    vm.CloseAction = () => DismissViewController(false, () => LaunchLoginFlow());
                }
                vm.UpdateTempPasswordAction = () => DismissViewController(false, () => LaunchUpdateTempPasswordFlow());
            }

            NavigateToPage(twoFactorPage);
        }

        private void LaunchSetPasswordFlow()
        {
            var setPasswordPage = new SetPasswordPage();
            var app = new App.App(new AppOptions { IosExtension = true });
            ThemeManager.SetTheme(app.Resources);
            ThemeManager.ApplyResourcesTo(setPasswordPage);
            if (setPasswordPage.BindingContext is SetPasswordPageViewModel vm)
            {
                vm.UpdateTempPasswordAction = () => DismissViewController(false, () => LaunchUpdateTempPasswordFlow());
                vm.SetPasswordSuccessAction = () => DismissLockAndContinue();
                vm.CloseAction = () => DismissViewController(false, () => LaunchHomePage());
            }

            NavigateToPage(setPasswordPage);
        }

        private void LaunchUpdateTempPasswordFlow()
        {
            var updateTempPasswordPage = new UpdateTempPasswordPage();
            var app = new App.App(new AppOptions { IosExtension = true });
            ThemeManager.SetTheme(app.Resources);
            ThemeManager.ApplyResourcesTo(updateTempPasswordPage);
            if (updateTempPasswordPage.BindingContext is UpdateTempPasswordPageViewModel vm)
            {
                vm.UpdateTempPasswordSuccessAction = () => DismissViewController(false, () => LaunchHomePage());
                vm.LogOutAction = () => DismissViewController(false, () => LaunchHomePage());
            }

            NavigateToPage(updateTempPasswordPage);
        }

        private void LaunchDeviceApprovalOptionsFlow()
        {
            var loginApproveDevicePage = new LoginApproveDevicePage();
            var app = new App.App(new AppOptions { IosExtension = true });
            ThemeManager.SetTheme(app.Resources);
            ThemeManager.ApplyResourcesTo(loginApproveDevicePage);
            if (loginApproveDevicePage.BindingContext is LoginApproveDeviceViewModel vm)
            {
                vm.LogInWithMasterPasswordAction = () => DismissViewController(false, () => PerformSegue("lockPasswordSegue", this));
                vm.RequestAdminApprovalAction = () => DismissViewController(false, () => LaunchLoginWithDevice(AuthRequestType.AdminApproval, vm.Email, true));
                vm.LogInWithDeviceAction = () => DismissViewController(false, () => LaunchLoginWithDevice(AuthRequestType.AuthenticateAndUnlock, vm.Email, true));
            }

            NavigateToPage(loginApproveDevicePage);
        }

        public Task SetPreviousPageInfoAsync() => Task.CompletedTask;
        public Task UpdateThemeAsync() => Task.CompletedTask;

        public void Navigate(NavigationTarget navTarget, INavigationParams navParams = null)
        {
            if (_context?.IsCreatingPasskey == true
                &&
                _context.PickCredentialForFido2CreationTcs != null
                &&
                !_context.PickCredentialForFido2CreationTcs.Task.IsCompleted)
            {
                // if it's creating passkey
                // and we have an active pending TaskCompletionSource
                // then we let the Fido2 Authenticator flow manage the navigation to avoid issues
                // like duplicated navigation.
                return;
            }

            DoNavigate(navTarget, navParams);
        }

        internal void DoNavigate(NavigationTarget navTarget, INavigationParams navParams = null)
        {
            switch (navTarget)
            {
                case NavigationTarget.HomeLogin:
                    DismissViewController(false, () => LaunchHomePage());
                    break;
                case NavigationTarget.Login:
                    if (navParams is LoginNavigationParams loginParams)
                    {
                        DismissViewController(false, () => LaunchLoginFlow(loginParams.Email));
                    }
                    else
                    {
                        DismissViewController(false, () => LaunchLoginFlow());
                    }
                    break;
                case NavigationTarget.Lock:
                    DismissViewController(false, () => PerformSegue(SegueConstants.LOCK, this));
                    break;
                case NavigationTarget.AutofillCiphers:
                case NavigationTarget.Home:
                    DismissViewController(false, () => PerformSegue(SegueConstants.LOGIN_LIST, this));
                    break;
            }
        }
    }
}
