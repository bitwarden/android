using AuthenticationServices;
using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Bit.iOS.Autofill.Models;
using Bit.iOS.Core.Utilities;
using Foundation;
using System;
using System.Threading.Tasks;
using Bit.App.Pages;
using UIKit;
using Xamarin.Forms;
using Bit.App.Utilities;
using Bit.App.Models;
using Bit.iOS.Core.Views;
using CoreNFC;

namespace Bit.iOS.Autofill
{
    public partial class CredentialProviderViewController : ASCredentialProviderViewController
    {
        private Context _context;
        private bool _initedAppCenter;
        private NFCNdefReaderSession _nfcSession = null;
        private Core.NFCReaderDelegate _nfcDelegate = null;

        public CredentialProviderViewController(IntPtr handle)
            : base(handle)
        {
            ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
        }

        public override void ViewDidLoad()
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

        public override async void PrepareCredentialList(ASCredentialServiceIdentifier[] serviceIdentifiers)
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
                LaunchHomePage();
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

        public override async void ProvideCredentialWithoutUserInteraction(ASPasswordCredentialIdentity credentialIdentity)
        {
            InitAppIfNeeded();
            var storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            await storageService.SaveAsync(Bit.Core.Constants.PasswordRepromptAutofillKey, false);
            await storageService.SaveAsync(Bit.Core.Constants.PasswordVerifiedAutofillKey, false);
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

        public override async void PrepareInterfaceToProvideCredential(ASPasswordCredentialIdentity credentialIdentity)
        {
            InitAppIfNeeded();
            if (!await IsAuthed())
            {
                LaunchHomePage();
                return;
            }
            _context.CredentialIdentity = credentialIdentity;
            CheckLock(async () => await ProvideCredentialAsync());
        }

        public override async void PrepareInterfaceForExtensionConfiguration()
        {
            InitAppIfNeeded();
            _context.Configuring = true;
            if (!await IsAuthed())
            {
                LaunchHomePage();
                return;
            }
            CheckLock(() => PerformSegue("setupSegue", this));
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

        public void DismissLockAndContinue()
        {
            DismissViewController(false, async () =>
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
            });
        }

        private async Task ProvideCredentialAsync(bool userInteraction = true)
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

            var storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            var decCipher = await cipher.DecryptAsync();
            if (decCipher.Reprompt != Bit.Core.Enums.CipherRepromptType.None)
            {
                // Prompt for password using either the lock screen or dialog unless
                // already verified the password.
                if (!userInteraction)
                {
                    await storageService.SaveAsync(Bit.Core.Constants.PasswordRepromptAutofillKey, true);
                    var err = new NSError(new NSString("ASExtensionErrorDomain"),
                    Convert.ToInt32(ASExtensionErrorCode.UserInteractionRequired), null);
                    ExtensionContext?.CancelRequest(err);
                    return;
                }
                else if (!await storageService.GetAsync<bool>(Bit.Core.Constants.PasswordVerifiedAutofillKey))
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
            var disableTotpCopy = await storageService.GetAsync<bool?>(Bit.Core.Constants.DisableAutoTotpCopyKey);
            if (!disableTotpCopy.GetValueOrDefault(false))
            {
                var userService = ServiceContainer.Resolve<IUserService>("userService");
                var canAccessPremiumAsync = await userService.CanAccessPremiumAsync();
                if (!string.IsNullOrWhiteSpace(decCipher.Login.Totp) &&
                    (canAccessPremiumAsync || cipher.OrganizationUseTotp))
                {
                    var totpService = ServiceContainer.Resolve<ITotpService>("totpService");
                    totpCode = await totpService.GetCodeAsync(decCipher.Login.Totp);
                }
            }

            CompleteRequest(decCipher.Id, decCipher.Login.Username, decCipher.Login.Password, totpCode);
        }

        private async void CheckLock(Action notLockedAction)
        {
            var storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            if (await IsLocked() || await storageService.GetAsync<bool>(Bit.Core.Constants.PasswordRepromptAutofillKey))
            {
                PerformSegue("lockPasswordSegue", this);
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
            var userService = ServiceContainer.Resolve<IUserService>("userService");
            return userService.IsAuthenticatedAsync();
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
            if (!_initedAppCenter)
            {
                iOSCoreHelpers.RegisterAppCenter();
                _initedAppCenter = true;
            }
            iOSCoreHelpers.Bootstrap();
            iOSCoreHelpers.AppearanceAdjustments(deviceActionService);
            _nfcDelegate = new Core.NFCReaderDelegate((success, message) =>
                messagingService.Send("gotYubiKeyOTP", message));
            iOSCoreHelpers.SubscribeBroadcastReceiver(this, _nfcSession, _nfcDelegate);
        }

        private void InitAppIfNeeded()
        {
            if (ServiceContainer.RegisteredServices == null || ServiceContainer.RegisteredServices.Count == 0)
            {
                InitApp();
            }
        }

        private void LaunchHomePage()
        {
            var homePage = new HomePage();
            var app = new App.App(new AppOptions { IosExtension = true });
            ThemeManager.SetTheme(false, app.Resources);
            ThemeManager.ApplyResourcesToPage(homePage);
            if (homePage.BindingContext is HomeViewModel vm)
            {
                vm.StartLoginAction = () => DismissViewController(false, () => LaunchLoginFlow());
                vm.StartRegisterAction = () => DismissViewController(false, () => LaunchRegisterFlow());
                vm.StartSsoLoginAction = () => DismissViewController(false, () => LaunchLoginSsoFlow());
                vm.StartEnvironmentAction = () => DismissViewController(false, () => LaunchEnvironmentFlow());
                vm.CloseAction = () => CompleteRequest();
            }

            var navigationPage = new NavigationPage(homePage);
            var loginController = navigationPage.CreateViewController();
            loginController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(loginController, true, null);
        }

        private void LaunchEnvironmentFlow()
        {
            var environmentPage = new EnvironmentPage();
            var app = new App.App(new AppOptions { IosExtension = true });
            ThemeManager.SetTheme(false, app.Resources);
            ThemeManager.ApplyResourcesToPage(environmentPage);
            if (environmentPage.BindingContext is EnvironmentPageViewModel vm)
            {
                vm.SubmitSuccessAction = () => DismissViewController(false, () => LaunchHomePage());
                vm.CloseAction = () => DismissViewController(false, () => LaunchHomePage());
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
            ThemeManager.SetTheme(false, app.Resources);
            ThemeManager.ApplyResourcesToPage(registerPage);
            if (registerPage.BindingContext is RegisterPageViewModel vm)
            {
                vm.RegistrationSuccess = () => DismissViewController(false, () => LaunchLoginFlow(vm.Email));
                vm.CloseAction = () => DismissViewController(false, () => LaunchHomePage());
            }

            var navigationPage = new NavigationPage(registerPage);
            var loginController = navigationPage.CreateViewController();
            loginController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(loginController, true, null);
        }

        private void LaunchLoginFlow(string email = null)
        {
            var loginPage = new LoginPage(email);
            var app = new App.App(new AppOptions { IosExtension = true });
            ThemeManager.SetTheme(false, app.Resources);
            ThemeManager.ApplyResourcesToPage(loginPage);
            if (loginPage.BindingContext is LoginPageViewModel vm)
            {
                vm.StartTwoFactorAction = () => DismissViewController(false, () => LaunchTwoFactorFlow(false));
                vm.LogInSuccessAction = () => DismissLockAndContinue();
                vm.CloseAction = () => DismissViewController(false, () => LaunchHomePage());
            }

            var navigationPage = new NavigationPage(loginPage);
            var loginController = navigationPage.CreateViewController();
            loginController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(loginController, true, null);
        }

        private void LaunchLoginSsoFlow()
        {
            var loginPage = new LoginSsoPage();
            var app = new App.App(new AppOptions { IosExtension = true });
            ThemeManager.SetTheme(false, app.Resources);
            ThemeManager.ApplyResourcesToPage(loginPage);
            if (loginPage.BindingContext is LoginSsoPageViewModel vm)
            {
                vm.StartTwoFactorAction = () => DismissViewController(false, () => LaunchTwoFactorFlow(true));
                vm.StartSetPasswordAction = () => DismissViewController(false, () => LaunchSetPasswordFlow());
                vm.SsoAuthSuccessAction = () => DismissLockAndContinue();
                vm.CloseAction = () => DismissViewController(false, () => LaunchHomePage());
            }

            var navigationPage = new NavigationPage(loginPage);
            var loginController = navigationPage.CreateViewController();
            loginController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(loginController, true, null);
        }

        private void LaunchTwoFactorFlow(bool authingWithSso)
        {
            var twoFactorPage = new TwoFactorPage(authingWithSso);
            var app = new App.App(new AppOptions { IosExtension = true });
            ThemeManager.SetTheme(false, app.Resources);
            ThemeManager.ApplyResourcesToPage(twoFactorPage);
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
            ThemeManager.SetTheme(false, app.Resources);
            ThemeManager.ApplyResourcesToPage(setPasswordPage);
            if (setPasswordPage.BindingContext is SetPasswordPageViewModel vm)
            {
                vm.SetPasswordSuccessAction = () => DismissLockAndContinue();
                vm.CloseAction = () => DismissViewController(false, () => LaunchHomePage());
            }

            var navigationPage = new NavigationPage(setPasswordPage);
            var setPasswordController = navigationPage.CreateViewController();
            setPasswordController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(setPasswordController, true, null);
        }
    }
}
