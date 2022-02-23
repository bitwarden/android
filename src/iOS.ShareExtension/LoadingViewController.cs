using System;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using AuthenticationServices;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Pages;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Bit.iOS.Core;
using Bit.iOS.Core.Controllers;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Views;
using Bit.iOS.ShareExtension.Models;
using CoreNFC;
using Foundation;
using Microsoft.AppCenter.Crashes;
using MobileCoreServices;
using UIKit;
using Xamarin.Forms;

namespace Bit.iOS.ShareExtension
{
    public partial class LoadingViewController : ExtendedUIViewController
    {
        private Context _context = new Context();
        private bool _initedAppCenter;
        private NFCNdefReaderSession _nfcSession = null;
        private Core.NFCReaderDelegate _nfcDelegate = null;

        readonly LazyResolve<IStateService> _stateService = new LazyResolve<IStateService>("stateervice");
        readonly LazyResolve<IVaultTimeoutService> _vaultTimeoutService = new LazyResolve<IVaultTimeoutService>("vaultTimeoutService");
        readonly LazyResolve<IDeviceActionService> _deviceActionService = new LazyResolve<IDeviceActionService>("deviceActionService");
        readonly LazyResolve<IEventService> _eventService = new LazyResolve<IEventService>("eventService");

        public LoadingViewController(IntPtr handle)
            : base(handle)
        { }

        public override void ViewDidLoad()
        {
            InitApp();

            base.ViewDidLoad();

            Logo.Image = new UIImage(ThemeHelpers.LightTheme ? "logo.png" : "logo_white.png");
            View.BackgroundColor = ThemeHelpers.SplashBackgroundColor;
            _context.ExtensionContext = ExtensionContext;

            foreach (var item in ExtensionContext.InputItems)
            {
                var processed = false;
                foreach (var itemProvider in item.Attachments)
                {
                    if (itemProvider.HasItemConformingTo(UTType.PlainText))
                    {
                        _context.ProviderType = UTType.PlainText;

                        processed = true;
                        break;
                    }
                    else if (itemProvider.HasItemConformingTo(UTType.Data))
                    {
                        _context.ProviderType = UTType.Data;

                        processed = true;
                        break;
                    }
                }
                if (processed)
                {
                    break;
                }
            }
        }

        public override async void ViewDidAppear(bool animated)
        {
            base.ViewDidAppear(animated);

            try
            {
                if (!await IsAuthed())
                {
                    LaunchHomePage();
                    return;
                }
                else if (await IsLocked())
                {
                    PerformSegue("lockPasswordSegue", this);
                }
                else
                {
                    ContinueOnAsync().FireAndForget();
                }
            }
            catch (Exception ex)
            {
                Crashes.TrackError(ex);
            }
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            if (segue.DestinationViewController is UINavigationController navController
                &&
                navController.TopViewController is LockPasswordViewController passwordViewController)
            {
                passwordViewController.LoadingController = this;
                segue.DestinationViewController.PresentationController.Delegate =
                    new CustomPresentationControllerDelegate(passwordViewController.DismissModalAction);
            }
        }

        public void DismissLockAndContinue()
        {
            Debug.WriteLine("BW Log, Dismissing lock controller.");
            DismissViewController(false, () => ContinueOnAsync().FireAndForget());
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
            var sendAddEditPage = new SendAddEditPage(appOptions)
            {
                OnClose = () => CompleteRequest(),
                AfterSubmit = () => CompleteRequest()
            };

            var app = new App.App(appOptions);
            ThemeManager.SetTheme(app.Resources);
            ThemeManager.ApplyResourcesToPage(sendAddEditPage);

            var navigationPage = new NavigationPage(sendAddEditPage);
            var sendAddEditController = navigationPage.CreateViewController();
            sendAddEditController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(sendAddEditController, true, null);
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

        private void InitApp()
        {
            // Init Xamarin Forms
            Forms.Init();

            if (ServiceContainer.RegisteredServices.Count > 0)
            {
                ServiceContainer.Reset();
            }
            iOSCoreHelpers.RegisterLocalServices();
            var messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            ServiceContainer.Init(_deviceActionService.Value.DeviceUserAgent,
                Bit.Core.Constants.iOSExtensionClearCiphersCacheKey, Bit.Core.Constants.iOSAllClearCipherCacheKeys);
            if (!_initedAppCenter)
            {
                iOSCoreHelpers.RegisterAppCenter();
                _initedAppCenter = true;
            }
            iOSCoreHelpers.Bootstrap();

            var app = new App.App(new AppOptions { IosExtension = true });
            ThemeManager.SetTheme(app.Resources);

            iOSCoreHelpers.AppearanceAdjustments();
            _nfcDelegate = new NFCReaderDelegate((success, message) =>
                messagingService.Send("gotYubiKeyOTP", message));
            iOSCoreHelpers.SubscribeBroadcastReceiver(this, _nfcSession, _nfcDelegate);
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
                if (await IsAuthed())
                {
                    await AppHelpers.LogOutAsync(await _stateService.Value.GetActiveUserIdAsync());
                    if (_deviceActionService.Value.SystemMajorVersion() >= 12)
                    {
                        await ASCredentialIdentityStore.SharedStore?.RemoveAllCredentialIdentitiesAsync();
                    }
                }
            });
        }

        private void LaunchHomePage()
        {
            var homePage = new HomePage();
            var app = new App.App(new AppOptions { IosExtension = true });
            ThemeManager.SetTheme(app.Resources);
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

            LogoutIfAuthed();
        }

        private void LaunchEnvironmentFlow()
        {
            var environmentPage = new EnvironmentPage();
            var app = new App.App(new AppOptions { IosExtension = true });
            ThemeManager.SetTheme(app.Resources);
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
            ThemeManager.SetTheme(app.Resources);
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
            ThemeManager.SetTheme(app.Resources);
            ThemeManager.ApplyResourcesToPage(loginPage);
            if (loginPage.BindingContext is LoginPageViewModel vm)
            {
                vm.StartTwoFactorAction = () => DismissViewController(false, () => LaunchTwoFactorFlow(false));
                vm.UpdateTempPasswordAction = () => DismissViewController(false, () => LaunchUpdateTempPasswordFlow());
                vm.LogInSuccessAction = () => DismissLockAndContinue();
                vm.CloseAction = () => CompleteRequest();
            }

            var navigationPage = new NavigationPage(loginPage);
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
            ThemeManager.ApplyResourcesToPage(loginPage);
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
            var twoFactorPage = new TwoFactorPage();
            var app = new App.App(new AppOptions { IosExtension = true });
            ThemeManager.SetTheme(app.Resources);
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
            ThemeManager.ApplyResourcesToPage(setPasswordPage);
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
            ThemeManager.ApplyResourcesToPage(updateTempPasswordPage);
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
    }
}
