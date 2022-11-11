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
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.iOS.Autofill.Models;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Views;
using CoreFoundation;
using CoreNFC;
using Foundation;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

namespace Bit.iOS.Autofill
{
    public partial class CredentialProviderViewController : ASCredentialProviderViewController, IAccountsManagerHost
    {
        private Context _context;
        private NFCNdefReaderSession _nfcSession = null;
        private Core.NFCReaderDelegate _nfcDelegate = null;
        private IAccountsManager _accountsManager;

        private readonly LazyResolve<IStateService> _stateService = new LazyResolve<IStateService>("stateService");

        public CredentialProviderViewController(IntPtr handle)
            : base(handle)
        {
            ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
        }

        public override void ViewDidLoad()
        {
            try
            {
                InitApp();
                base.ViewDidLoad();
                Logo.Image = new UIImage(ThemeHelpers.LightTheme ? "logo.png" : "logo_white.png");
                View.BackgroundColor = ThemeHelpers.SplashBackgroundColor;
                _context = new Context
                {
                    ExtContext = ExtensionContext
                };
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw;
            }
        }

        public override async void PrepareCredentialList(ASCredentialServiceIdentifier[] serviceIdentifiers)
        {
            try
            {
                InitAppIfNeeded();
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
                    PerformSegue("lockPasswordSegue", this);
                }
                else
                {
                    if (_context.ServiceIdentifiers == null || _context.ServiceIdentifiers.Length == 0)
                    {
                        PerformSegue("loginSearchSegue", this);
                    }
                    else
                    {
                        PerformSegue("loginListSegue", this);
                    }
                }
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw;
            }
        }

        public override async void ProvideCredentialWithoutUserInteraction(ASPasswordCredentialIdentity credentialIdentity)
        {
            try
            {
                InitAppIfNeeded();
                await _stateService.Value.SetPasswordRepromptAutofillAsync(false);
                await _stateService.Value.SetPasswordVerifiedAutofillAsync(false);
                if (!await IsAuthed() || await IsLocked())
                {
                    var err = new NSError(new NSString("ASExtensionErrorDomain"),
                        Convert.ToInt32(ASExtensionErrorCode.UserInteractionRequired), null);
                    ExtensionContext.CancelRequest(err);
                    return;
                }
                _context.CredentialIdentity = credentialIdentity;
                await ProvideCredentialAsync(false);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw;
            }
        }

        public override async void PrepareInterfaceToProvideCredential(ASPasswordCredentialIdentity credentialIdentity)
        {
            try
            {
                InitAppIfNeeded();
                if (!await IsAuthed())
                {
                    await _accountsManager.NavigateOnAccountChangeAsync(false);
                    return;
                }
                _context.CredentialIdentity = credentialIdentity;
                await CheckLockAsync(async () => await ProvideCredentialAsync());
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw;
            }
        }

        public override async void PrepareInterfaceForExtensionConfiguration()
        {
            try
            {
                InitAppIfNeeded();
                _context.Configuring = true;
                if (!await IsAuthed())
                {
                    await _accountsManager.NavigateOnAccountChangeAsync(false);
                    return;
                }
                await CheckLockAsync(() => PerformSegue("setupSegue", this));
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw;
            }
        }

        public void CompleteRequest(string id = null, string username = null,
            string password = null, string totp = null)
        {
            if ((_context?.Configuring ?? true) && string.IsNullOrWhiteSpace(password))
            {
                ServiceContainer.Reset();
                ExtensionContext?.CompleteExtensionConfigurationRequest();
                return;
            }

            if (_context == null || string.IsNullOrWhiteSpace(username) || string.IsNullOrWhiteSpace(password))
            {
                ServiceContainer.Reset();
                var err = new NSError(new NSString("ASExtensionErrorDomain"),
                    Convert.ToInt32(ASExtensionErrorCode.UserCanceled), null);
                NSRunLoop.Main.BeginInvokeOnMainThread(() => ExtensionContext?.CancelRequest(err));
                return;
            }

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
                ExtensionContext?.CompleteRequest(cred, null);
            });
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
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw;
            }
        }

        public void DismissLockAndContinue()
        {
            DismissViewController(false, async () =>
            {
                try
                {
                    if (_context.CredentialIdentity != null)
                    {
                        await ProvideCredentialAsync();
                        return;
                    }
                    if (_context.Configuring)
                    {
                        PerformSegue("setupSegue", this);
                        return;
                    }
                    if (_context.ServiceIdentifiers == null || _context.ServiceIdentifiers.Length == 0)
                    {
                        PerformSegue("loginSearchSegue", this);
                    }
                    else
                    {
                        PerformSegue("loginListSegue", this);
                    }
                }
                catch (Exception ex)
                {
                    LoggerHelper.LogEvenIfCantBeResolved(ex);
                    throw;
                }
            });
        }

        private async Task ProvideCredentialAsync(bool userInteraction = true)
        {
            try
            {
                var cipherService = ServiceContainer.Resolve<ICipherService>("cipherService", true);
                Bit.Core.Models.Domain.Cipher cipher = null;
                var cancel = cipherService == null || _context.CredentialIdentity?.RecordIdentifier == null;
                if (!cancel)
                {
                    cipher = await cipherService.GetAsync(_context.CredentialIdentity.RecordIdentifier);
                    cancel = cipher == null || cipher.Type != Bit.Core.Enums.CipherType.Login || cipher.Login == null;
                }
                if (cancel)
                {
                    var err = new NSError(new NSString("ASExtensionErrorDomain"),
                        Convert.ToInt32(ASExtensionErrorCode.CredentialIdentityNotFound), null);
                    ExtensionContext?.CancelRequest(err);
                    return;
                }

                var decCipher = await cipher.DecryptAsync();
                if (decCipher.Reprompt != Bit.Core.Enums.CipherRepromptType.None)
                {
                    // Prompt for password using either the lock screen or dialog unless
                    // already verified the password.
                    if (!userInteraction)
                    {
                        await _stateService.Value.SetPasswordRepromptAutofillAsync(true);
                        var err = new NSError(new NSString("ASExtensionErrorDomain"),
                        Convert.ToInt32(ASExtensionErrorCode.UserInteractionRequired), null);
                        ExtensionContext?.CancelRequest(err);
                        return;
                    }
                    else if (!await _stateService.Value.GetPasswordVerifiedAutofillAsync())
                    {
                        // Add a timeout to resolve keyboard not always showing up.
                        await Task.Delay(250);
                        var passwordRepromptService = ServiceContainer.Resolve<IPasswordRepromptService>("passwordRepromptService");
                        if (!await passwordRepromptService.ShowPasswordPromptAsync())
                        {
                            var err = new NSError(new NSString("ASExtensionErrorDomain"),
                                Convert.ToInt32(ASExtensionErrorCode.UserCanceled), null);
                            ExtensionContext?.CancelRequest(err);
                            return;
                        }
                    }
                }
                string totpCode = null;
                var disableTotpCopy = await _stateService.Value.GetDisableAutoTotpCopyAsync();
                if (!disableTotpCopy.GetValueOrDefault(false))
                {
                    var canAccessPremiumAsync = await _stateService.Value.CanAccessPremiumAsync();
                    if (!string.IsNullOrWhiteSpace(decCipher.Login.Totp) &&
                        (canAccessPremiumAsync || cipher.OrganizationUseTotp))
                    {
                        var totpService = ServiceContainer.Resolve<ITotpService>("totpService");
                        totpCode = await totpService.GetCodeAsync(decCipher.Login.Totp);
                    }
                }

                CompleteRequest(decCipher.Id, decCipher.Login.Username, decCipher.Login.Password, totpCode);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw;
            }
        }

        private async Task CheckLockAsync(Action notLockedAction)
        {
            if (await IsLocked() || await _stateService.Value.GetPasswordRepromptAutofillAsync())
            {
                DispatchQueue.MainQueue.DispatchAsync(() =>  PerformSegue("lockPasswordSegue", this));
            }
            else
            {
                notLockedAction();
            }
        }

        private Task<bool> IsLocked()
        {
            var vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            return vaultTimeoutService.IsLockedAsync();
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
            // Init Xamarin Forms
            Forms.Init();

            if (ServiceContainer.RegisteredServices.Count > 0)
            {
                ServiceContainer.Reset();
            }
            iOSCoreHelpers.RegisterLocalServices();
            var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            var messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            ServiceContainer.Init(deviceActionService.DeviceUserAgent, 
                Bit.Core.Constants.iOSAutoFillClearCiphersCacheKey, Bit.Core.Constants.iOSAllClearCipherCacheKeys);
            iOSCoreHelpers.InitLogger();
            iOSCoreHelpers.Bootstrap();
            var appOptions = new AppOptions { IosExtension = true };
            var app = new App.App(appOptions);
            ThemeManager.SetTheme(app.Resources);
            iOSCoreHelpers.AppearanceAdjustments();
            _nfcDelegate = new Core.NFCReaderDelegate((success, message) =>
                messagingService.Send("gotYubiKeyOTP", message));
            iOSCoreHelpers.SubscribeBroadcastReceiver(this, _nfcSession, _nfcDelegate);

            _accountsManager = ServiceContainer.Resolve<IAccountsManager>("accountsManager");
            _accountsManager.Init(() => appOptions, this);
        }

        private void InitAppIfNeeded()
        {
            if (ServiceContainer.RegisteredServices == null || ServiceContainer.RegisteredServices.Count == 0)
            {
                InitApp();
            }
        }

        private void LaunchHomePage(bool shouldCheckRememberEmail = true)
        {
            var appOptions = new AppOptions { IosExtension = true };
            var homePage = new HomePage(appOptions, shouldCheckRememberEmail);
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

            var navigationPage = new NavigationPage(homePage);
            var loginController = navigationPage.CreateViewController();
            loginController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(loginController, true, null);

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
                vm.SubmitSuccessAction = () => DismissViewController(false, () => LaunchHomePage(shouldCheckRememberEmail: false));
                vm.CloseAction = () => DismissViewController(false, () => LaunchHomePage(shouldCheckRememberEmail: false));
            }

            var navigationPage = new NavigationPage(environmentPage);
            var loginController = navigationPage.CreateViewController();
            loginController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(loginController, true, null);
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
                vm.CloseAction = () => DismissViewController(false, () => LaunchHomePage(shouldCheckRememberEmail: false));
            }

            var navigationPage = new NavigationPage(registerPage);
            var loginController = navigationPage.CreateViewController();
            loginController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(loginController, true, null);
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
                vm.LogInWithDeviceAction = () => DismissViewController(false, () => LaunchLoginWithDevice(email));
                vm.LogInSuccessAction = () => DismissLockAndContinue();
                vm.CloseAction = () => DismissViewController(false, () => LaunchHomePage(shouldCheckRememberEmail: false));
            }

            var navigationPage = new NavigationPage(loginPage);
            var loginController = navigationPage.CreateViewController();
            loginController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(loginController, true, null);

            LogoutIfAuthed();
        }

        private void LaunchLoginWithDevice(string email = null)
        {
            var appOptions = new AppOptions { IosExtension = true };
            var app = new App.App(appOptions);
            var loginWithDevicePage = new LoginPasswordlessRequestPage(email, appOptions);
            ThemeManager.SetTheme(app.Resources);
            ThemeManager.ApplyResourcesTo(loginWithDevicePage);
            if (loginWithDevicePage.BindingContext is LoginPasswordlessRequestViewModel vm)
            {
                vm.StartTwoFactorAction = () => DismissViewController(false, () => LaunchTwoFactorFlow(false));
                vm.UpdateTempPasswordAction = () => DismissViewController(false, () => LaunchUpdateTempPasswordFlow());
                vm.LogInSuccessAction = () => DismissLockAndContinue();
                vm.CloseAction = () => DismissViewController(false, () => LaunchHomePage());
            }

            var navigationPage = new NavigationPage(loginWithDevicePage);
            var loginController = navigationPage.CreateViewController();
            loginController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(loginController, true, null);

            LogoutIfAuthed();
        }

        private void LaunchLoginSsoFlow()
        {
            var loginPage = new LoginSsoPage();
            var app = new App.App(new AppOptions { IosExtension = true });
            ThemeManager.SetTheme(app.Resources);
            ThemeManager.ApplyResourcesTo(loginPage);
            if (loginPage.BindingContext is LoginSsoPageViewModel vm)
            {
                vm.StartTwoFactorAction = () => DismissViewController(false, () => LaunchTwoFactorFlow(true));
                vm.StartSetPasswordAction = () => DismissViewController(false, () => LaunchSetPasswordFlow());
                vm.UpdateTempPasswordAction = () => DismissViewController(false, () => LaunchUpdateTempPasswordFlow());
                vm.SsoAuthSuccessAction = () => DismissLockAndContinue();
                vm.CloseAction = () => DismissViewController(false, () => LaunchHomePage());
            }

            var navigationPage = new NavigationPage(loginPage);
            var loginController = navigationPage.CreateViewController();
            loginController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(loginController, true, null);

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

            var navigationPage = new NavigationPage(twoFactorPage);
            var twoFactorController = navigationPage.CreateViewController();
            twoFactorController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(twoFactorController, true, null);
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

            var navigationPage = new NavigationPage(setPasswordPage);
            var setPasswordController = navigationPage.CreateViewController();
            setPasswordController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(setPasswordController, true, null);
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

            var navigationPage = new NavigationPage(updateTempPasswordPage);
            var updateTempPasswordController = navigationPage.CreateViewController();
            updateTempPasswordController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(updateTempPasswordController, true, null);
        }

        public Task SetPreviousPageInfoAsync() => Task.CompletedTask;
        public Task UpdateThemeAsync() => Task.CompletedTask;

        public void Navigate(NavigationTarget navTarget, INavigationParams navParams = null)
        {
            switch (navTarget)
            {
                case NavigationTarget.HomeLogin:
                    if (navParams is HomeNavigationParams homeParams)
                    {
                        DismissViewController(false, () => LaunchHomePage(homeParams.ShouldCheckRememberEmail));
                    }
                    else
                    {
                        DismissViewController(false, () => LaunchHomePage());
                    }
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
                    DismissViewController(false, () => PerformSegue("lockPasswordSegue", this));
                    break;
                case NavigationTarget.AutofillCiphers:
                case NavigationTarget.Home:
                    DismissViewController(false, () => PerformSegue("loginListSegue", this));
                    break;
            }
        }
    }
}
