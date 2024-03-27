using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Threading.Tasks;
using AuthenticationServices;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Pages;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.iOS.Core;
using Bit.iOS.Core.Models;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Views;
using Bit.iOS.Extension.Models;
using CoreNFC;
using Foundation;
using Microsoft.Maui.ApplicationModel;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Platform;
using MobileCoreServices;
using UIKit;

namespace Bit.iOS.Extension
{
    public partial class LoadingViewController : UIViewController
    {
        private Context _context = new Context();
        private NFCNdefReaderSession _nfcSession = null;
        private Core.NFCReaderDelegate _nfcDelegate = null;

        private bool _shouldInitialize = true;

        public LoadingViewController(IntPtr handle)
            : base(handle)
        { }

        public override void ViewDidLoad()
        {
            try
            {
                InitApp();
                base.ViewDidLoad();
                Logo.Image = new UIImage(ThemeHelpers.LightTheme ? "logo.png" : "logo_white.png");
                View.BackgroundColor = ThemeHelpers.SplashBackgroundColor;

                if (!_shouldInitialize)
                {
                    return;
                }

                _context.ExtContext = ExtensionContext;
                foreach (var item in _context.ExtContext.InputItems)
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
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        public override async void ViewDidAppear(bool animated)
        {
            if (!_shouldInitialize)
            {
                return;
            }

            try
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
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
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
                    passwordViewController.LaunchHomePage = () => DismissViewController(false, () => LaunchHomePage());
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

        private void NavigateToPage(ContentPage page)
        {
            var navigationPage = new NavigationPage(page);
            var uiController = navigationPage.ToUIViewController(MauiContextSingleton.Instance.MauiContext);
            uiController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;

            PresentViewController(uiController, true, null);
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
                _context?.ExtContext?.CompleteRequest(returningItems, null);
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
            if (!_shouldInitialize)
            {
                return;
            }

            // TODO: Change for iOSCoreHelpers.InitApp(...) when implementing IAccountsManager here
            iOSCoreHelpers.SetupMaui();

            if (ServiceContainer.RegisteredServices.Count > 0)
            {
                ServiceContainer.Reset();
            }
            iOSCoreHelpers.RegisterLocalServices();
            var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            var messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            ServiceContainer.Init(deviceActionService.DeviceUserAgent, 
                Bit.Core.Constants.iOSExtensionClearCiphersCacheKey, Bit.Core.Constants.iOSAllClearCipherCacheKeys);
            iOSCoreHelpers.InitLogger();
            iOSCoreHelpers.RegisterFinallyBeforeBootstrap();
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
            var vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            return vaultTimeoutService.IsLockedAsync();
        }

        private Task<bool> IsAuthed()
        {
            var stateService = ServiceContainer.Resolve<IStateService>("stateService");
            return stateService.IsAuthenticatedAsync();
        }

        private void LogoutIfAuthed()
        {
            NSRunLoop.Main.BeginInvokeOnMainThread(async () =>
            {
                try
                {
                    if (await IsAuthed())
                    {
                        var stateService = ServiceContainer.Resolve<IStateService>("stateService");
                        await AppHelpers.LogOutAsync(await stateService.GetActiveUserIdAsync());
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
                vm.CloseAction = () => CompleteRequest(null, null);
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
            var twoFactorPage = new TwoFactorPage();
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
    }
}
