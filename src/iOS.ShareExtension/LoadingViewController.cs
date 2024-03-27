using System;
using System.Diagnostics;
using System.Linq;
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
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Views;
using Bit.iOS.ShareExtension.Models;
using CoreNFC;
using Foundation;
using Microsoft.Maui.ApplicationModel;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Platform;
using MobileCoreServices;
using UIKit;

namespace Bit.iOS.ShareExtension
{
    public partial class LoadingViewController : UIViewController, IAccountsManagerHost
    {
        const string STORYBOARD_NAME = "MainInterface";

        private Context _context = new Context();
        private NFCNdefReaderSession _nfcSession = null;
        private Core.NFCReaderDelegate _nfcDelegate = null;
        private IAccountsManager _accountsManager;

        readonly LazyResolve<IStateService> _stateService = new LazyResolve<IStateService>("stateService");
        readonly LazyResolve<IVaultTimeoutService> _vaultTimeoutService = new LazyResolve<IVaultTimeoutService>("vaultTimeoutService");

        Lazy<UIStoryboard> _storyboard = new Lazy<UIStoryboard>(() => UIStoryboard.FromName(STORYBOARD_NAME, null));

        private readonly Lazy<AppOptions> _appOptions = new Lazy<AppOptions>(() => new AppOptions { IosExtension = true });
        private App.App _app = null;
        private UIViewController _currentModalController;
        private bool _presentingOnNavigationPage;

        private ExtensionNavigationController ExtNavigationController
        {
            get
            {
                NavigationController.PresentationController.Delegate =
                    new CustomPresentationControllerDelegate(CompleteRequest);
                return NavigationController as ExtensionNavigationController;
            }
        }

        public LoadingViewController(IntPtr handle)
            : base(handle)
        { }

        public override void ViewDidLoad()
        {
            iOSCoreHelpers.InitApp(this, Bit.Core.Constants.iOSShareExtensionClearCiphersCacheKey,
                _nfcSession, out _nfcDelegate, out _accountsManager);

            base.ViewDidLoad();

            Logo.Image = new UIImage(ThemeHelpers.LightTheme ? "logo.png" : "logo_white.png");
            View.BackgroundColor = ThemeHelpers.SplashBackgroundColor;
            _context.ExtensionContext = ExtensionContext;
            _context.ProviderType = GetProviderTypeFromExtensionInputItems();
        }

        /// <summary>
        /// Gets the provider <see cref="UTType"/> given the input items
        /// </summary>
        private string GetProviderTypeFromExtensionInputItems()
        {
            foreach (var item in ExtensionContext.InputItems)
            {
                foreach (var itemProvider in item.Attachments)
                {
                    if (itemProvider.HasItemConformingTo(UTType.PlainText))
                    {
                        return UTType.PlainText;
                    }

                    if (itemProvider.HasItemConformingTo(UTType.Data))
                    {
                        return UTType.Data;
                    }
                }
            }
            return null;
        }

        public override async void ViewDidAppear(bool animated)
        {
            base.ViewDidAppear(animated);

            try
            {
                if (!await IsAuthed())
                {
                    await _accountsManager.NavigateOnAccountChangeAsync(false);
                    return;
                }
                else if (await IsLocked())
                {
                    NavigateToLockViewController();
                }
                else
                {
                    ContinueOnAsync().FireAndForget();
                }
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        void NavigateToLockViewController()
        {
            var viewController = _storyboard.Value.InstantiateViewController("lockVC") as LockPasswordViewController;
            viewController.LoadingController = this;
            viewController.LaunchHomePage = () => DismissViewController(false, () => LaunchHomePage());
            viewController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;

            if (_presentingOnNavigationPage)
            {
                _presentingOnNavigationPage = false;
                DismissViewController(true, () => ExtNavigationController.PushViewController(viewController, true));
            }
            else
            {
                ExtNavigationController.PushViewController(viewController, true);
            }
        }

        private void NavigateToPage(ContentPage page)
        {
            var navigationPage = new NavigationPage(page);

            _currentModalController = navigationPage.ToUIViewController(MauiContextSingleton.Instance.MauiContext);
            _currentModalController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            _presentingOnNavigationPage = true;
            PresentViewController(_currentModalController, true, null);
        }

        public void DismissLockAndContinue()
        {
            Debug.WriteLine("BW Log, Dismissing lock controller.");

            ClearBeforeNavigating();

            DismissViewController(false, () => ContinueOnAsync().FireAndForget());
        }

        private void DismissAndLaunch(Action pageToLaunch)
        {
            ClearBeforeNavigating();

            DismissViewController(false, pageToLaunch);
        }

        void ClearBeforeNavigating()
        {
            _currentModalController?.Dispose();
            _currentModalController = null;

            if (_storyboard.IsValueCreated)
            {
                _storyboard.Value.Dispose();
                _storyboard = null;
                _storyboard = new Lazy<UIStoryboard>(() => UIStoryboard.FromName(STORYBOARD_NAME, null));
            }
        }

        private async Task ContinueOnAsync()
        {
            Tuple<SendType, string, byte[], string> createSend = null;

            if (_context.ProviderType == UTType.Data)
            {
                var (filename, fileBytes) = await LoadDataBytesAsync();
                createSend = new Tuple<SendType, string, byte[], string>(SendType.File, filename, fileBytes, null);
            }
            else if (_context.ProviderType == UTType.PlainText)
            {
                createSend = new Tuple<SendType, string, byte[], string>(SendType.Text, null, null, LoadText());
            }

            var appOptions = new AppOptions
            {
                IosExtension = true,
                CreateSend = createSend,
                CopyInsteadOfShareAfterSaving = true
            };
            var sendPage = new SendAddOnlyPage(appOptions)
            {
                OnClose = () => CompleteRequest(),
                AfterSubmit = () => CompleteRequest()
            };

            SetupAppAndApplyResources(sendPage);

            NavigateToPage(sendPage);
        }

        private async Task<(string, byte[])> LoadDataBytesAsync()
        {
            var itemProvider = ExtensionContext?.InputItems.FirstOrDefault()?.Attachments?.FirstOrDefault();
            if (itemProvider is null || !itemProvider.HasItemConformingTo(UTType.Data))
                return default;

            var item = await itemProvider.LoadItemAsync(UTType.Data, null);
            if (item is NSUrl urlItem)
            {
                var filename = urlItem?.AbsoluteUrl?.LastPathComponent;

                var data = NSData.FromUrl(urlItem);
                var stream = NSInputStream.FromData(data);
                var bytes = new byte[data.Length];
                try
                {
                    stream.Open();
                    stream.Read(bytes, data.Length);
                }
                finally
                {
                    stream?.Close();
                }

                return (filename, bytes);
            }

            return default;
        }

        private string LoadText()
        {
            return ExtensionContext?.InputItems
                        .FirstOrDefault()
                        ?.AttributedContentText?.Value;
        }

        public void CompleteRequest()
        {
            NSRunLoop.Main.BeginInvokeOnMainThread(() =>
            {
                ServiceContainer.Reset();
                ExtensionContext?.CompleteRequest(new NSExtensionItem[0], null);
            });
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
                    throw;
                }
            });
        }

        private App.App SetupAppAndApplyResources(ContentPage page)
        {
            if (_app is null)
            {
                var app = new App.App(_appOptions.Value);
                ThemeManager.SetTheme(app.Resources);
            }
            ThemeManager.ApplyResourcesTo(page);
            return _app;
        }

        private void LaunchHomePage()
        {
            var homePage = new HomePage(_appOptions.Value);
            SetupAppAndApplyResources(homePage);
            if (homePage.BindingContext is HomeViewModel vm)
            {
                vm.StartLoginAction = () => DismissAndLaunch(() => LaunchLoginFlow(vm.Email));
                vm.StartRegisterAction = () => DismissAndLaunch(() => LaunchRegisterFlow());
                vm.StartSsoLoginAction = () => DismissAndLaunch(() => LaunchLoginSsoFlow());
                vm.StartEnvironmentAction = () => DismissAndLaunch(() => LaunchEnvironmentFlow());
                vm.CloseAction = () => CompleteRequest();
            }

            NavigateToPage(homePage);
            LogoutIfAuthed();
        }

        private void LaunchEnvironmentFlow()
        {
            var environmentPage = new EnvironmentPage();
            SetupAppAndApplyResources(environmentPage);
            ThemeManager.ApplyResourcesTo(environmentPage);
            if (environmentPage.BindingContext is EnvironmentPageViewModel vm)
            {
                vm.SubmitSuccessTask = async () =>
                {
                    await DismissViewControllerAsync(false);
                    await MainThread.InvokeOnMainThreadAsync(() => LaunchHomePage());
                };
                vm.CloseAction = () => DismissAndLaunch(() => LaunchHomePage());
            }

            NavigateToPage(environmentPage);
        }

        private void LaunchRegisterFlow()
        {
            var registerPage = new RegisterPage(null);
            SetupAppAndApplyResources(registerPage);
            if (registerPage.BindingContext is RegisterPageViewModel vm)
            {
                vm.RegistrationSuccess = () => DismissAndLaunch(() => LaunchLoginFlow(vm.Email));
                vm.CloseAction = () => DismissAndLaunch(() => LaunchHomePage());
            }
            NavigateToPage(registerPage);
        }

        private void LaunchLoginFlow(string email = null)
        {
            var loginPage = new LoginPage(email, _appOptions.Value);
            SetupAppAndApplyResources(loginPage);
            if (loginPage.BindingContext is LoginPageViewModel vm)
            {
                vm.StartTwoFactorAction = () => DismissAndLaunch(() => LaunchTwoFactorFlow(false));
                vm.UpdateTempPasswordAction = () => DismissAndLaunch(() => LaunchUpdateTempPasswordFlow());
                vm.StartSsoLoginAction = () => DismissAndLaunch(() => LaunchLoginSsoFlow());
                vm.LogInWithDeviceAction = () => DismissAndLaunch(() => LaunchLoginWithDevice(AuthRequestType.AuthenticateAndUnlock, email));
                vm.LogInSuccessAction = () => { DismissLockAndContinue(); };
                vm.CloseAction = () => DismissAndLaunch(() => LaunchHomePage());
            }
            NavigateToPage(loginPage);

            LogoutIfAuthed();
        }

        private void LaunchLoginWithDevice(AuthRequestType authRequestType, string email = null, bool authingWithSso = false)
        {
            var loginWithDevicePage = new LoginPasswordlessRequestPage(email, authRequestType, _appOptions.Value, authingWithSso);
            SetupAppAndApplyResources(loginWithDevicePage);
            if (loginWithDevicePage.BindingContext is LoginPasswordlessRequestViewModel vm)
            {
                vm.StartTwoFactorAction = () => DismissAndLaunch(() => LaunchTwoFactorFlow(false));
                vm.UpdateTempPasswordAction = () => DismissAndLaunch(() => LaunchUpdateTempPasswordFlow());
                vm.LogInSuccessAction = () => { DismissLockAndContinue(); };
                vm.CloseAction = () => DismissAndLaunch(() => LaunchHomePage());
            }
            NavigateToPage(loginWithDevicePage);

            LogoutIfAuthed();
        }

        private void LaunchLoginSsoFlow()
        {
            var loginPage = new LoginSsoPage(_appOptions.Value);
            SetupAppAndApplyResources(loginPage);
            if (loginPage.BindingContext is LoginSsoPageViewModel vm)
            {
                vm.StartTwoFactorAction = () => DismissAndLaunch(() => LaunchTwoFactorFlow(true));
                vm.StartSetPasswordAction = () => DismissAndLaunch(() => LaunchSetPasswordFlow());
                vm.UpdateTempPasswordAction = () => DismissAndLaunch(() => LaunchUpdateTempPasswordFlow());
                vm.StartDeviceApprovalOptionsAction = () => DismissViewController(false, () => LaunchDeviceApprovalOptionsFlow());
                vm.SsoAuthSuccessAction = () => DismissLockAndContinue();
                vm.CloseAction = () => DismissAndLaunch(() => LaunchHomePage());
            }
            NavigateToPage(loginPage);

            LogoutIfAuthed();
        }

        private void LaunchTwoFactorFlow(bool authingWithSso)
        {
            var twoFactorPage = new TwoFactorPage();
            SetupAppAndApplyResources(twoFactorPage);
            if (twoFactorPage.BindingContext is TwoFactorPageViewModel vm)
            {
                vm.TwoFactorAuthSuccessAction = () => DismissLockAndContinue();
                vm.StartSetPasswordAction = () => DismissAndLaunch(() => LaunchSetPasswordFlow());
                vm.StartDeviceApprovalOptionsAction = () => DismissViewController(false, () => LaunchDeviceApprovalOptionsFlow());
                if (authingWithSso)
                {
                    vm.CloseAction = () => DismissAndLaunch(() => LaunchLoginSsoFlow());
                }
                else
                {
                    vm.CloseAction = () => DismissAndLaunch(() => LaunchLoginFlow());
                }
                vm.UpdateTempPasswordAction = () => DismissAndLaunch(() => LaunchUpdateTempPasswordFlow());
            }
            NavigateToPage(twoFactorPage);
        }

        private void LaunchSetPasswordFlow()
        {
            var setPasswordPage = new SetPasswordPage();
            SetupAppAndApplyResources(setPasswordPage);
            if (setPasswordPage.BindingContext is SetPasswordPageViewModel vm)
            {
                vm.UpdateTempPasswordAction = () => DismissAndLaunch(() => LaunchUpdateTempPasswordFlow());
                vm.SetPasswordSuccessAction = () => DismissLockAndContinue();
                vm.CloseAction = () => DismissAndLaunch(() => LaunchHomePage());
            }
            NavigateToPage(setPasswordPage);
        }

        private void LaunchUpdateTempPasswordFlow()
        {
            var updateTempPasswordPage = new UpdateTempPasswordPage();
            SetupAppAndApplyResources(updateTempPasswordPage);
            if (updateTempPasswordPage.BindingContext is UpdateTempPasswordPageViewModel vm)
            {
                vm.UpdateTempPasswordSuccessAction = () => DismissAndLaunch(() => LaunchHomePage());
                vm.LogOutAction = () => DismissAndLaunch(() => LaunchHomePage());
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

            var navigationPage = new NavigationPage(loginApproveDevicePage);
            var loginApproveDeviceController = navigationPage.ToUIViewController(MauiContextSingleton.Instance.MauiContext);
            loginApproveDeviceController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(loginApproveDeviceController, true, null);
        }

        public void Navigate(NavigationTarget navTarget, INavigationParams navParams = null)
        {
            if (ExtNavigationController?.ViewControllers?.Any() ?? false)
            {
                ExtNavigationController.PopViewController(false);
            }
            else if (ExtNavigationController?.ModalViewController != null)
            {
                ExtNavigationController.DismissModalViewController(false);
            }

            switch (navTarget)
            {
                case NavigationTarget.HomeLogin:
                    ExecuteLaunch(() => LaunchHomePage());
                    break;
                case NavigationTarget.Login:
                    if (navParams is LoginNavigationParams loginParams)
                    {
                        ExecuteLaunch(() => LaunchLoginFlow(loginParams.Email));
                    }
                    else
                    {
                        ExecuteLaunch(() => LaunchLoginFlow());
                    }
                    break;
                case NavigationTarget.Lock:
                    NavigateToLockViewController();
                    break;
                case NavigationTarget.Home:
                    DismissLockAndContinue();
                    break;
            }
        }

        private void ExecuteLaunch(Action launchAction)
        {
            if (_presentingOnNavigationPage)
            {
                DismissAndLaunch(launchAction);
            }
            else
            {
                launchAction();
            }
        }

        public Task SetPreviousPageInfoAsync() => Task.CompletedTask;
        public Task UpdateThemeAsync() => Task.CompletedTask;
    }
}
