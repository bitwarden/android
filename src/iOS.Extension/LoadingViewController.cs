using System;
using System.Diagnostics;
using Foundation;
using UIKit;
using Bit.iOS.Core;
using Bit.iOS.Extension.Models;
using MobileCoreServices;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Controllers;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.iOS.Core.Models;
using Bit.Core.Utilities;
using Bit.Core.Abstractions;
using Bit.App.Abstractions;
using CoreNFC;
using Xamarin.Forms;
using Bit.App.Pages;
using Bit.App.Models;
using Bit.App.Utilities;
using Bit.iOS.Core.Views;

namespace Bit.iOS.Extension
{
    public partial class LoadingViewController : ExtendedUIViewController
    {
        private Context _context = new Context();
        private bool _initedAppCenter;
        private NFCNdefReaderSession _nfcSession = null;
        private Core.NFCReaderDelegate _nfcDelegate = null;

        public LoadingViewController(IntPtr handle)
            : base(handle)
        { }

        public override void ViewDidLoad()
        {
            InitApp();
            base.ViewDidLoad();
            Logo.Image = new UIImage(ThemeHelpers.LightTheme ? "logo.png" : "logo_white.png");
            View.BackgroundColor = ThemeHelpers.SplashBackgroundColor;
            _context.ExtContext = ExtensionContext;
            foreach (var item in ExtensionContext.InputItems)
            {
                var processed = false;
                foreach (var itemProvider in item.Attachments)
                {
                    if (ProcessWebUrlProvider(itemProvider)
                        || ProcessFindLoginProvider(itemProvider)
                        || ProcessFindLoginBrowserProvider(itemProvider, Constants.UTTypeAppExtensionFillBrowserAction)
                        || ProcessFindLoginBrowserProvider(itemProvider, Constants.UTTypeAppExtensionFillWebViewAction)
                        || ProcessFindLoginBrowserProvider(itemProvider, Constants.UTTypeAppExtensionUrl)
                        || ProcessSaveLoginProvider(itemProvider)
                        || ProcessChangePasswordProvider(itemProvider)
                        || ProcessExtensionSetupProvider(itemProvider))
                    {
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
            if (_context.ProviderType == Constants.UTTypeAppExtensionSetup)
            {
                PerformSegue("setupSegue", this);
                return;
            }
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
                ContinueOn();
            }
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            if (segue.DestinationViewController is UINavigationController navController)
            {
                if (navController.TopViewController is LoginListViewController listLoginController)
                {
                    listLoginController.Context = _context;
                    listLoginController.LoadingController = this;
                    segue.DestinationViewController.PresentationController.Delegate =
                        new CustomPresentationControllerDelegate(listLoginController.DismissModalAction);
                }
                else if (navController.TopViewController is LoginAddViewController addLoginController)
                {
                    addLoginController.Context = _context;
                    addLoginController.LoadingController = this;
                    segue.DestinationViewController.PresentationController.Delegate =
                        new CustomPresentationControllerDelegate(addLoginController.DismissModalAction);
                }
                else if (navController.TopViewController is LockPasswordViewController passwordViewController)
                {
                    passwordViewController.LoadingController = this;
                    segue.DestinationViewController.PresentationController.Delegate =
                        new CustomPresentationControllerDelegate(passwordViewController.DismissModalAction);
                }
                else if (navController.TopViewController is SetupViewController setupViewController)
                {
                    setupViewController.Context = _context;
                    setupViewController.LoadingController = this;
                    segue.DestinationViewController.PresentationController.Delegate =
                        new CustomPresentationControllerDelegate(setupViewController.DismissModalAction);
                }
            }
        }

        public void DismissLockAndContinue()
        {
            Debug.WriteLine("BW Log, Dismissing lock controller.");
            DismissViewController(false, () => ContinueOn());
        }

        private void ContinueOn()
        {
            Debug.WriteLine("BW Log, Segue to setup, login add or list.");
            if (_context.ProviderType == Constants.UTTypeAppExtensionSaveLoginAction)
            {
                PerformSegue("newLoginSegue", this);
            }
            else if (_context.ProviderType == Constants.UTTypeAppExtensionSetup)
            {
                PerformSegue("setupSegue", this);
            }
            else
            {
                PerformSegue("loginListSegue", this);
            }
        }

        public void CompleteUsernamePasswordRequest(string id, string username, string password,
            List<Tuple<string, string>> fields, string totp)
        {
            NSDictionary itemData = null;
            if (_context.ProviderType == UTType.PropertyList)
            {
                var fillScript = new FillScript(_context.Details, username, password, fields);
                var scriptJson = CoreHelpers.SerializeJson(fillScript, true);
                var scriptDict = new NSDictionary(Constants.AppExtensionWebViewPageFillScript, scriptJson);
                itemData = new NSDictionary(NSJavaScriptExtension.FinalizeArgumentKey, scriptDict);
            }
            else if (_context.ProviderType == Constants.UTTypeAppExtensionFindLoginAction)
            {
                itemData = new NSDictionary(
                    Constants.AppExtensionUsernameKey, username,
                    Constants.AppExtensionPasswordKey, password);
            }
            else if (_context.ProviderType == Constants.UTTypeAppExtensionFillBrowserAction
                || _context.ProviderType == Constants.UTTypeAppExtensionFillWebViewAction)
            {
                var fillScript = new FillScript(_context.Details, username, password, fields);
                var scriptJson = CoreHelpers.SerializeJson(fillScript, true);
                itemData = new NSDictionary(Constants.AppExtensionWebViewPageFillScript, scriptJson);
            }
            else if (_context.ProviderType == Constants.UTTypeAppExtensionSaveLoginAction)
            {
                itemData = new NSDictionary(
                    Constants.AppExtensionUsernameKey, username,
                    Constants.AppExtensionPasswordKey, password);
            }
            else if (_context.ProviderType == Constants.UTTypeAppExtensionChangePasswordAction)
            {
                itemData = new NSDictionary(
                    Constants.AppExtensionPasswordKey, string.Empty,
                    Constants.AppExtensionOldPasswordKey, password);
            }

            if (!string.IsNullOrWhiteSpace(totp))
            {
                UIPasteboard.General.String = totp;
            }
            CompleteRequest(id, itemData);
        }

        public void CompleteRequest(string id, NSDictionary itemData)
        {
            Debug.WriteLine("BW LOG, itemData: " + itemData);
            var resultsProvider = new NSItemProvider(itemData, UTType.PropertyList);
            var resultsItem = new NSExtensionItem { Attachments = new NSItemProvider[] { resultsProvider } };
            var returningItems = new NSExtensionItem[] { resultsItem };
            NSRunLoop.Main.BeginInvokeOnMainThread(async () =>
            {
                if (!string.IsNullOrWhiteSpace(id) && itemData != null)
                {
                    var eventService = ServiceContainer.Resolve<IEventService>("eventService");
                    await eventService.CollectAsync(Bit.Core.Enums.EventType.Cipher_ClientAutofilled, id);
                }
                ServiceContainer.Reset();
                ExtensionContext?.CompleteRequest(returningItems, null);
            });
        }

        private bool ProcessItemProvider(NSItemProvider itemProvider, string type, Action<NSDictionary> dictAction,
            Action<NSUrl> urlAction = null)
        {
            if (!itemProvider.HasItemConformingTo(type))
            {
                return false;
            }

            itemProvider.LoadItem(type, null, (NSObject list, NSError error) =>
            {
                if (list == null)
                {
                    return;
                }

                _context.ProviderType = type;
                if (list is NSDictionary dict && dictAction != null)
                {
                    dictAction(dict);
                }
                else if (list is NSUrl && urlAction != null)
                {
                    var url = list as NSUrl;
                    urlAction(url);
                }
                else
                {
                    throw new Exception("Cannot parse list for action. List is " +
                        (list?.GetType().ToString() ?? "null"));
                }

                Debug.WriteLine("BW LOG, ProviderType: " + _context.ProviderType);
                Debug.WriteLine("BW LOG, Url: " + _context.UrlString);
                Debug.WriteLine("BW LOG, Title: " + _context.LoginTitle);
                Debug.WriteLine("BW LOG, Username: " + _context.Username);
                Debug.WriteLine("BW LOG, Password: " + _context.Password);
                Debug.WriteLine("BW LOG, Old Password: " + _context.OldPassword);
                Debug.WriteLine("BW LOG, Notes: " + _context.Notes);
                Debug.WriteLine("BW LOG, Details: " + _context.Details);

                if (_context.PasswordOptions != null)
                {
                    Debug.WriteLine("BW LOG, PasswordOptions Min Length: " + _context.PasswordOptions.MinLength);
                    Debug.WriteLine("BW LOG, PasswordOptions Max Length: " + _context.PasswordOptions.MaxLength);
                    Debug.WriteLine("BW LOG, PasswordOptions Require Digits: " + _context.PasswordOptions.RequireDigits);
                    Debug.WriteLine("BW LOG, PasswordOptions Require Symbols: " + _context.PasswordOptions.RequireSymbols);
                    Debug.WriteLine("BW LOG, PasswordOptions Forbidden Chars: " + _context.PasswordOptions.ForbiddenCharacters);
                }
            });

            return true;
        }

        private bool ProcessWebUrlProvider(NSItemProvider itemProvider)
        {
            return ProcessItemProvider(itemProvider, UTType.PropertyList, dict =>
            {
                var result = dict[NSJavaScriptExtension.PreprocessingResultsKey];
                if (result == null)
                {
                    return;
                }
                _context.UrlString = result.ValueForKey(new NSString(Constants.AppExtensionUrlStringKey)) as NSString;
                var jsonStr = result.ValueForKey(new NSString(Constants.AppExtensionWebViewPageDetails)) as NSString;
                _context.Details = DeserializeString<PageDetails>(jsonStr);
            });
        }

        private bool ProcessFindLoginProvider(NSItemProvider itemProvider)
        {
            return ProcessItemProvider(itemProvider, Constants.UTTypeAppExtensionFindLoginAction, dict =>
            {
                var version = dict[Constants.AppExtensionVersionNumberKey] as NSNumber;
                var url = dict[Constants.AppExtensionUrlStringKey] as NSString;
                if (url != null)
                {
                    _context.UrlString = url;
                }
            });
        }

        private bool ProcessFindLoginBrowserProvider(NSItemProvider itemProvider, string action)
        {
            return ProcessItemProvider(itemProvider, action, dict =>
            {
                var version = dict[Constants.AppExtensionVersionNumberKey] as NSNumber;
                var url = dict[Constants.AppExtensionUrlStringKey] as NSString;
                if (url != null)
                {
                    _context.UrlString = url;
                }
                _context.Details = DeserializeDictionary<PageDetails>(dict[Constants.AppExtensionWebViewPageDetails] as NSDictionary);
            }, url =>
            {
                if (url != null)
                {
                    _context.UrlString = url.AbsoluteString;
                }
            });
        }

        private bool ProcessSaveLoginProvider(NSItemProvider itemProvider)
        {
            return ProcessItemProvider(itemProvider, Constants.UTTypeAppExtensionSaveLoginAction, dict =>
            {
                var version = dict[Constants.AppExtensionVersionNumberKey] as NSNumber;
                var url = dict[Constants.AppExtensionUrlStringKey] as NSString;
                var title = dict[Constants.AppExtensionTitleKey] as NSString;
                var sectionTitle = dict[Constants.AppExtensionSectionTitleKey] as NSString;
                var username = dict[Constants.AppExtensionUsernameKey] as NSString;
                var password = dict[Constants.AppExtensionPasswordKey] as NSString;
                var notes = dict[Constants.AppExtensionNotesKey] as NSString;
                var fields = dict[Constants.AppExtensionFieldsKey] as NSDictionary;
                if (url != null)
                {
                    _context.UrlString = url;
                }
                _context.LoginTitle = title;
                _context.Username = username;
                _context.Password = password;
                _context.Notes = notes;
                _context.PasswordOptions = DeserializeDictionary<PasswordGenerationOptions>(dict[Constants.AppExtensionPasswordGeneratorOptionsKey] as NSDictionary);
            });
        }

        private bool ProcessChangePasswordProvider(NSItemProvider itemProvider)
        {
            return ProcessItemProvider(itemProvider, Constants.UTTypeAppExtensionChangePasswordAction, dict =>
            {
                var version = dict[Constants.AppExtensionVersionNumberKey] as NSNumber;
                var url = dict[Constants.AppExtensionUrlStringKey] as NSString;
                var title = dict[Constants.AppExtensionTitleKey] as NSString;
                var sectionTitle = dict[Constants.AppExtensionSectionTitleKey] as NSString;
                var username = dict[Constants.AppExtensionUsernameKey] as NSString;
                var password = dict[Constants.AppExtensionPasswordKey] as NSString;
                var oldPassword = dict[Constants.AppExtensionOldPasswordKey] as NSString;
                var notes = dict[Constants.AppExtensionNotesKey] as NSString;
                var fields = dict[Constants.AppExtensionFieldsKey] as NSDictionary;
                if (url != null)
                {
                    _context.UrlString = url;
                }
                _context.LoginTitle = title;
                _context.Username = username;
                _context.Password = password;
                _context.OldPassword = oldPassword;
                _context.Notes = notes;
                _context.PasswordOptions = DeserializeDictionary<PasswordGenerationOptions>(dict[Constants.AppExtensionPasswordGeneratorOptionsKey] as NSDictionary);
            });
        }

        private bool ProcessExtensionSetupProvider(NSItemProvider itemProvider)
        {
            if (itemProvider.HasItemConformingTo(Constants.UTTypeAppExtensionSetup))
            {
                _context.ProviderType = Constants.UTTypeAppExtensionSetup;
                return true;
            }
            return false;
        }

        private T DeserializeDictionary<T>(NSDictionary dict)
        {
            if (dict != null)
            {
                var jsonData = NSJsonSerialization.Serialize(
                    dict, NSJsonWritingOptions.PrettyPrinted, out NSError jsonError);
                if (jsonData != null)
                {
                    var jsonString = new NSString(jsonData, NSStringEncoding.UTF8);
                    return DeserializeString<T>(jsonString);
                }
            }
            return default(T);
        }

        private T DeserializeString<T>(NSString jsonString)
        {
            if (jsonString != null)
            {
                var convertedObject = CoreHelpers.DeserializeJson<T>(jsonString.ToString());
                return convertedObject;
            }
            return default(T);
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
                Bit.Core.Constants.iOSExtensionClearCiphersCacheKey, Bit.Core.Constants.iOSAllClearCipherCacheKeys);
            if (!_initedAppCenter)
            {
                iOSCoreHelpers.RegisterAppCenter();
                _initedAppCenter = true;
            }
            iOSCoreHelpers.Bootstrap();
            iOSCoreHelpers.AppearanceAdjustments(deviceActionService);
            _nfcDelegate = new NFCReaderDelegate((success, message) =>
                messagingService.Send("gotYubiKeyOTP", message));
            iOSCoreHelpers.SubscribeBroadcastReceiver(this, _nfcSession, _nfcDelegate);
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
                vm.CloseAction = () => CompleteRequest(null, null);
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
                vm.CloseAction = () => CompleteRequest(null, null);
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
            var twoFactorPage = new TwoFactorPage();
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
