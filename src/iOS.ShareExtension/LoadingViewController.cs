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
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Bit.iOS.Core;
using Bit.iOS.Core.Controllers;
using Bit.iOS.Core.Services;
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

        readonly LazyResolve<IUserService> _userService = new LazyResolve<IUserService>("userService");
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
                    if (itemProvider.HasItemConformingTo(UTType.Image))
                    {
                        _context.ProviderType = UTType.Image;

                        processed = true;
                        break;
                    }
                    else if (itemProvider.HasItemConformingTo(UTType.PlainText))
                    {
                        _context.ProviderType = UTType.PlainText;

                        processed = true;
                        break;
                    }
                }
                if (processed)
                {
                    break;
                }
            }

            //ProcessAttachmentsAsync().FireAndForget(ex => Crashes.TrackError(ex));
        }

        //private async Task ProcessAttachmentsAsync()
        //{
        //    foreach (var item in ExtensionContext.InputItems)
        //    {
        //        var processed = false;
        //        foreach (var itemProvider in item.Attachments)
        //        {
        //            if (await ProcessImageProviderAsync(itemProvider))
        //            {
        //                processed = true;
        //                break;
        //            }
        //        }
        //        if (processed)
        //        {
        //            break;
        //        }
        //    }
        //}

        //private async Task<bool> ProcessImageProviderAsync(NSItemProvider itemProvider)
        //{
        //    if (!itemProvider.HasItemConformingTo(UTType.Image))
        //        return false;

        //    var image = await itemProvider.LoadObjectAsync<UIImage>();
        //    _context.image
        //}

        public override async void ViewDidAppear(bool animated)
        {
            base.ViewDidAppear(animated);
            //if (_context.ProviderType == Constants.UTTypeAppExtensionSetup)
            //{
            //    PerformSegue("setupSegue", this);
            //    return;
            //}
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
                    ContinueOnAsync().FireAndForget(ex => Crashes.TrackError(ex));
                }
            }
            catch (Exception ex)
            {
                Crashes.TrackError(ex);
            }
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            if (segue.DestinationViewController is UINavigationController navController)
            {
                if (navController.TopViewController is LockPasswordViewController passwordViewController)
                {
                    passwordViewController.LoadingController = this;
                    segue.DestinationViewController.PresentationController.Delegate =
                        new CustomPresentationControllerDelegate(passwordViewController.DismissModalAction);
                }
                //else if (navController.TopViewController is ShareViewController shareViewController)
                //{
                //    shareViewController.Context = _context;
                //    shareViewController.LoadingController = this;
                //    segue.DestinationViewController.PresentationController.Delegate =
                //        new CustomPresentationControllerDelegate(shareViewController.DismissModalAction);
                //}
            }
        }

        public void DismissLockAndContinue()
        {
            Debug.WriteLine("BW Log, Dismissing lock controller.");
            DismissViewController(false, () => ContinueOnAsync().FireAndForget(ex => Crashes.TrackError(ex)));
        }

        private async Task ContinueOnAsync()
        {
            //Debug.WriteLine("BW Log, Segue to share.");
            //if (_context.ProviderType == Constants.UTTypeAppExtensionSaveLoginAction)
            //{
            //    PerformSegue("newLoginSegue", this);
            //}
            //else if (_context.ProviderType == Constants.UTTypeAppExtensionSetup)
            //{
            //    PerformSegue("setupSegue", this);
            //}
            //else
            //{
            //PerformSegue("shareSegue", this);
            //}

            Tuple<SendType, string, byte[], string> createSend = null;

            if (_context.ProviderType == UTType.Image)
            {
                var (filename, fileBytes) = await LoadImageBytesAsync();
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
                CanShareSendOnSave = true
            };
            var sendAddEditPage = new SendAddEditPage(appOptions)
            {
                OnClose = () => CompleteRequest(null, null),
                AfterSubmit = () => CompleteRequest(null, null)
            };

            var app = new App.App(appOptions);
            ThemeManager.SetTheme(false, app.Resources);
            ThemeManager.ApplyResourcesToPage(sendAddEditPage);
            //if (sendAddEditPage.BindingContext is SendAddEditPageViewModel vm)
            //{
            //    vm.UpdateTempPasswordSuccessAction = () => DismissViewController(false, () => LaunchHomePage());
            //    vm.LogOutAction = () => DismissViewController(false, () => LaunchHomePage());
            //}

            var navigationPage = new NavigationPage(sendAddEditPage);
            var sendAddEditController = navigationPage.CreateViewController();
            sendAddEditController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(sendAddEditController, true, null);



            //var page = new SendAddEditPage();
            //var vc = page.CreateViewController();
            //var shareNavController = new UINavigationController(vc);
            //NavigationController.PushViewController(shareNavController, true);

        }

        private string LoadText()
        {
            return ExtensionContext?.InputItems
                        .FirstOrDefault()
                        ?.AttributedContentText?.Value;
        }

        private async Task<(string, byte[])> LoadImageBytesAsync()
        {
            var extensionItem = ExtensionContext?.InputItems.FirstOrDefault();
            if (extensionItem?.Attachments is null)
                return default;

            string GetDefaultFileName(NSItemProvider itemProvider)
            {
                var filename = Guid.NewGuid().ToString();
                if (itemProvider.HasItemConformingTo(UTType.JPEG)
                    ||
                    itemProvider.HasItemConformingTo(UTType.JPEG2000))
                {
                    filename += ".jpg";
                }
                else if (itemProvider.HasItemConformingTo(UTType.PNG))
                {
                    filename += ".png";
                }
                else if (itemProvider.HasItemConformingTo(UTType.GIF))
                {
                    filename += ".gif";
                }
                else
                {
                    // Just default to png just in case
                    filename += ".png";
                }

                return filename;
            }

            foreach (var item in extensionItem.Attachments)
            {
                if (!item.HasItemConformingTo(UTType.Image))
                    continue;
                
                var image = await item.LoadObjectAsync<UIImage>();
                
                string filename = null;
                if (UIDevice.CurrentDevice.CheckSystemVersion(11, 0))
                {
                    var fileRepresentation = await item.LoadFileRepresentationAsync(UTType.Image);
                    filename = fileRepresentation?.LastPathComponent ?? GetDefaultFileName(item);
                }
                else
                {
                    // TODO: check how we could get the filename on iOS 10
                    filename = GetDefaultFileName(item);
                }

                byte[] data;
                using (var imageData = image.AsPNG())
                {
                    data = new byte[imageData.Length];
                    System.Runtime.InteropServices.Marshal.Copy(imageData.Bytes, data, 0,
                        Convert.ToInt32(imageData.Length));
                    return (filename, data);
                }
            }
            return default;
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
                    await _eventService.Value.CollectAsync(Bit.Core.Enums.EventType.Cipher_ClientAutofilled, id);
                }
                ServiceContainer.Reset();
                ExtensionContext?.CompleteRequest(returningItems, null);
            });
        }

        //private bool ProcessItemProvider(NSItemProvider itemProvider, string type, Action<NSDictionary> dictAction,
        //    Action<NSUrl> urlAction = null)
        //{
        //    if (!itemProvider.HasItemConformingTo(type))
        //    {
        //        return false;
        //    }

        //    itemProvider.LoadItem(type, null, (NSObject list, NSError error) =>
        //    {
        //        if (list == null)
        //        {
        //            return;
        //        }

        //        _context.ProviderType = type;
        //        if (list is NSDictionary dict && dictAction != null)
        //        {
        //            dictAction(dict);
        //        }
        //        else if (list is NSUrl && urlAction != null)
        //        {
        //            var url = list as NSUrl;
        //            urlAction(url);
        //        }
        //        else
        //        {
        //            throw new Exception("Cannot parse list for action. List is " +
        //                (list?.GetType().ToString() ?? "null"));
        //        }

        //        Debug.WriteLine("BW LOG, ProviderType: " + _context.ProviderType);
        //        Debug.WriteLine("BW LOG, Url: " + _context.UrlString);
        //        Debug.WriteLine("BW LOG, Title: " + _context.LoginTitle);
        //        Debug.WriteLine("BW LOG, Username: " + _context.Username);
        //        Debug.WriteLine("BW LOG, Password: " + _context.Password);
        //        Debug.WriteLine("BW LOG, Old Password: " + _context.OldPassword);
        //        Debug.WriteLine("BW LOG, Notes: " + _context.Notes);
        //        Debug.WriteLine("BW LOG, Details: " + _context.Details);

        //        if (_context.PasswordOptions != null)
        //        {
        //            Debug.WriteLine("BW LOG, PasswordOptions Min Length: " + _context.PasswordOptions.MinLength);
        //            Debug.WriteLine("BW LOG, PasswordOptions Max Length: " + _context.PasswordOptions.MaxLength);
        //            Debug.WriteLine("BW LOG, PasswordOptions Require Digits: " + _context.PasswordOptions.RequireDigits);
        //            Debug.WriteLine("BW LOG, PasswordOptions Require Symbols: " + _context.PasswordOptions.RequireSymbols);
        //            Debug.WriteLine("BW LOG, PasswordOptions Forbidden Chars: " + _context.PasswordOptions.ForbiddenCharacters);
        //        }
        //    });

        //    return true;
        //}

        //private bool ProcessPlainTextProvider(NSItemProvider itemProvider)
        //{
        //    return ProcessItemProvider(itemProvider, UTType.PlainText, dict =>
        //    {
        //        var result = dict[NSJavaScriptExtension.PreprocessingResultsKey];
        //        if (result == null)
        //        {
        //            return;
        //        }
        //        _context.UrlString = result.ValueForKey(new NSString(Constants.AppExtensionUrlStringKey)) as NSString;
        //        var jsonStr = result.ValueForKey(new NSString(Constants.AppExtensionWebViewPageDetails)) as NSString;
        //        _context.Details = DeserializeString<PageDetails>(jsonStr);
        //    });
        //}

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
            ThemeManager.SetTheme(false, app.Resources);

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
            return _userService.Value.IsAuthenticatedAsync();
        }

        private void LogoutIfAuthed()
        {
            NSRunLoop.Main.BeginInvokeOnMainThread(async () =>
            {
                if (await IsAuthed())
                {
                    await AppHelpers.LogOutAsync();
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

            LogoutIfAuthed();
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
                vm.UpdateTempPasswordAction = () => DismissViewController(false, () => LaunchUpdateTempPasswordFlow());
                vm.LogInSuccessAction = () => DismissLockAndContinue();
                vm.CloseAction = () => CompleteRequest(null, null);
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
            ThemeManager.SetTheme(false, app.Resources);
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
            ThemeManager.SetTheme(false, app.Resources);
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
            ThemeManager.SetTheme(false, app.Resources);
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
