using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.App.Services;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.iOS.Core.Services;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Services;
using CoreNFC;
using Foundation;
using HockeyApp.iOS;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

namespace Bit.iOS
{
    [Register("AppDelegate")]
    public partial class AppDelegate : Xamarin.Forms.Platform.iOS.FormsApplicationDelegate
    {
        private const string AppId = "com.8bit.bitwarden";
        private const string AppGroupId = "group.com.8bit.bitwarden";
        private const string AccessGroup = "LTZ2PFU5D6.com.8bit.bitwarden";

        private NFCNdefReaderSession _nfcSession = null;
        private iOSPushNotificationHandler _pushHandler = null;
        private NFCReaderDelegate _nfcDelegate = null;

        private IDeviceActionService _deviceActionService;
        private IMessagingService _messagingService;
        private IBroadcasterService _broadcasterService;
        private IStorageService _storageService;

        public override bool FinishedLaunching(UIApplication app, NSDictionary options)
        {
            Xamarin.Forms.Forms.Init();
            InitApp();
            Bootstrap();
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");

            LoadApplication(new App.App(null));
            AppearanceAdjustments();
            ZXing.Net.Mobile.Forms.iOS.Platform.Init();

            _broadcasterService.Subscribe(nameof(AppDelegate), (message) =>
            {
                if(message.Command == "scheduleLockTimer")
                {
                    var lockOptionMinutes = (int)message.Data;
                }
                else if(message.Command == "cancelLockTimer")
                {

                }
                else if(message.Command == "updatedTheme")
                {
                    // ThemeManager.SetThemeStyle(message.Data as string);
                }
                else if(message.Command == "copiedToClipboard")
                {

                }
                else if(message.Command == "listenYubiKeyOTP")
                {
                    ListenYubiKey((bool)message.Data);
                }
                else if(message.Command == "showAppExtension")
                {

                }
                else if(message.Command == "showStatusBar")
                {
                    UIApplication.SharedApplication.SetStatusBarHidden(!(bool)message.Data, false);
                }
                else if(message.Command == "syncCompleted")
                {
                    if(message.Data is Dictionary<string, object> data && data.ContainsKey("successfully"))
                    {
                        var success = data["successfully"] as bool?;
                        if(success.GetValueOrDefault())
                        {

                        }
                    }
                }
                else if(message.Command == "addedCipher" || message.Command == "editedCipher")
                {

                }
                else if(message.Command == "deletedCipher")
                {

                }
                else if(message.Command == "loggedOut")
                {

                }
            });

            return base.FinishedLaunching(app, options);
        }

        public override void DidEnterBackground(UIApplication uiApplication)
        {
            var view = new UIView(UIApplication.SharedApplication.KeyWindow.Frame)
            {
                Tag = 4321
            };
            var backgroundView = new UIView(UIApplication.SharedApplication.KeyWindow.Frame)
            {
                BackgroundColor = ((Color)Xamarin.Forms.Application.Current.Resources["SplashBackgroundColor"])
                    .ToUIColor()
            };
            var theme = ThemeManager.GetTheme(false);
            var darkbasedTheme = theme == "dark" || theme == "black" || theme == "nord";
            var logo = new UIImage(darkbasedTheme ? "logo_white.png" : "logo.png");
            var imageView = new UIImageView(logo)
            {
                Center = new CoreGraphics.CGPoint(view.Center.X, view.Center.Y - 30)
            };
            view.AddSubview(backgroundView);
            view.AddSubview(imageView);
            UIApplication.SharedApplication.KeyWindow.AddSubview(view);
            UIApplication.SharedApplication.KeyWindow.BringSubviewToFront(view);
            UIApplication.SharedApplication.KeyWindow.EndEditing(true);
            UIApplication.SharedApplication.SetStatusBarHidden(true, false);
            _storageService.SaveAsync(Bit.Core.Constants.LastActiveKey, DateTime.UtcNow);
            base.DidEnterBackground(uiApplication);
        }

        public override void OnActivated(UIApplication uiApplication)
        {
            base.OnActivated(uiApplication);
            UIApplication.SharedApplication.ApplicationIconBadgeNumber = 0;
            var view = UIApplication.SharedApplication.KeyWindow.ViewWithTag(4321);
            if(view != null)
            {
                view.RemoveFromSuperview();
                UIApplication.SharedApplication.SetStatusBarHidden(false, false);
            }
        }

        public override void WillEnterForeground(UIApplication uiApplication)
        {
            _messagingService.Send("resumed");
            base.WillEnterForeground(uiApplication);
        }

        public override bool OpenUrl(UIApplication application, NSUrl url, string sourceApplication,
            NSObject annotation)
        {
            return true;
        }

        public override void FailedToRegisterForRemoteNotifications(UIApplication application, NSError error)
        {
            _pushHandler?.OnErrorReceived(error);
        }

        public override void RegisteredForRemoteNotifications(UIApplication application, NSData deviceToken)
        {
            _pushHandler?.OnRegisteredSuccess(deviceToken);
        }

        public override void DidRegisterUserNotificationSettings(UIApplication application,
            UIUserNotificationSettings notificationSettings)
        {
            application.RegisterForRemoteNotifications();
        }

        public override void DidReceiveRemoteNotification(UIApplication application, NSDictionary userInfo,
            Action<UIBackgroundFetchResult> completionHandler)
        {
            _pushHandler?.OnMessageReceived(userInfo);
        }

        public override void ReceivedRemoteNotification(UIApplication application, NSDictionary userInfo)
        {
            _pushHandler?.OnMessageReceived(userInfo);
        }

        private void InitApp()
        {
            if(ServiceContainer.RegisteredServices.Count > 0)
            {
                return;
            }
            RegisterLocalServices();
            ServiceContainer.Init();
            _pushHandler = new iOSPushNotificationHandler(
                ServiceContainer.Resolve<IPushNotificationListenerService>("pushNotificationListenerService"));
            _nfcDelegate = new NFCReaderDelegate((success, message) =>
                _messagingService.Send("gotYubiKeyOTP", message));

            var crashManagerDelegate = new HockeyAppCrashManagerDelegate(
                ServiceContainer.Resolve<IAppIdService>("appIdService"),
                ServiceContainer.Resolve<IUserService>("userService"));
            var manager = BITHockeyManager.SharedHockeyManager;
            manager.Configure("51f96ae568ba45f699a18ad9f63046c3", crashManagerDelegate);
            manager.CrashManager.CrashManagerStatus = BITCrashManagerStatus.AutoSend;
            manager.StartManager();
            manager.Authenticator.AuthenticateInstallation();
            manager.DisableMetricsManager = manager.DisableFeedbackManager = manager.DisableUpdateManager = true;
            var task = crashManagerDelegate.InitAsync(manager);
        }

        private void RegisterLocalServices()
        {
            ServiceContainer.Register<ILogService>("logService", new ConsoleLogService());
            ServiceContainer.Register("settingsShim", new App.Migration.SettingsShim());
            if(false && App.Migration.MigrationHelpers.NeedsMigration())
            {
                ServiceContainer.Register<App.Migration.Abstractions.IOldSecureStorageService>(
                    "oldSecureStorageService", new Migration.KeyChainStorageService());
            }

            // Note: This might cause a race condition. Investigate more.
            Task.Run(() =>
            {
                FFImageLoading.Forms.Platform.CachedImageRenderer.Init();
                FFImageLoading.ImageService.Instance.Initialize(new FFImageLoading.Config.Configuration
                {
                    FadeAnimationEnabled = false,
                    FadeAnimationForCachedImages = false
                });
            });

            var preferencesStorage = new PreferencesStorageService(AppGroupId);
            var appGroupContainer = new NSFileManager().GetContainerUrl(AppGroupId);
            var liteDbStorage = new LiteDbStorageService(
                Path.Combine(appGroupContainer.Path, "Library", "bitwarden.db"));
            liteDbStorage.InitAsync();
            var localizeService = new LocalizeService();
            var broadcasterService = new BroadcasterService();
            var messagingService = new MobileBroadcasterMessagingService(broadcasterService);
            var i18nService = new MobileI18nService(localizeService.GetCurrentCultureInfo());
            var secureStorageService = new KeyChainStorageService(AppId, AccessGroup);
            var cryptoPrimitiveService = new CryptoPrimitiveService();
            var mobileStorageService = new MobileStorageService(preferencesStorage, liteDbStorage);
            var deviceActionService = new DeviceActionService(mobileStorageService, messagingService);
            var platformUtilsService = new MobilePlatformUtilsService(deviceActionService, messagingService,
                broadcasterService);

            ServiceContainer.Register<IBroadcasterService>("broadcasterService", broadcasterService);
            ServiceContainer.Register<IMessagingService>("messagingService", messagingService);
            ServiceContainer.Register<ILocalizeService>("localizeService", localizeService);
            ServiceContainer.Register<II18nService>("i18nService", i18nService);
            ServiceContainer.Register<ICryptoPrimitiveService>("cryptoPrimitiveService", cryptoPrimitiveService);
            ServiceContainer.Register<IStorageService>("storageService", mobileStorageService);
            ServiceContainer.Register<IStorageService>("secureStorageService", secureStorageService);
            ServiceContainer.Register<IDeviceActionService>("deviceActionService", deviceActionService);
            ServiceContainer.Register<IPlatformUtilsService>("platformUtilsService", platformUtilsService);

            // Push
            var notificationListenerService = new PushNotificationListenerService();
            ServiceContainer.Register<IPushNotificationListenerService>(
                "pushNotificationListenerService", notificationListenerService);
            var iosPushNotificationService = new iOSPushNotificationService();
            ServiceContainer.Register<IPushNotificationService>(
                "pushNotificationService", iosPushNotificationService);
        }

        private void Bootstrap()
        {
            (ServiceContainer.Resolve<II18nService>("i18nService") as MobileI18nService).Init();
            ServiceContainer.Resolve<IAuthService>("authService").Init();
            // Note: This is not awaited
            var bootstrapTask = BootstrapAsync();
        }

        private async Task BootstrapAsync()
        {
            var disableFavicon = await ServiceContainer.Resolve<IStorageService>("storageService").GetAsync<bool?>(
                Bit.Core.Constants.DisableFaviconKey);
            await ServiceContainer.Resolve<IStateService>("stateService").SaveAsync(
                Bit.Core.Constants.DisableFaviconKey, disableFavicon);
            await ServiceContainer.Resolve<IEnvironmentService>("environmentService").SetUrlsFromStorageAsync();
        }

        private void AppearanceAdjustments()
        {
            ThemeHelpers.SetAppearance(ThemeManager.GetTheme(false));
            /*
            var primaryColor = new UIColor(red: 0.24f, green: 0.55f, blue: 0.74f, alpha: 1.0f);
            var grayLight = new UIColor(red: 0.47f, green: 0.47f, blue: 0.47f, alpha: 1.0f);
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);
            UIBarButtonItem.AppearanceWhenContainedIn(new Type[] { typeof(UISearchBar) }).TintColor = primaryColor;
            UIButton.AppearanceWhenContainedIn(new Type[] { typeof(UISearchBar) }).SetTitleColor(primaryColor,
                UIControlState.Normal);
            UIButton.AppearanceWhenContainedIn(new Type[] { typeof(UISearchBar) }).TintColor = primaryColor;
            UIStepper.Appearance.TintColor = grayLight;
            UISlider.Appearance.TintColor = primaryColor;
            */
            UIApplication.SharedApplication.StatusBarHidden = false;
            UIApplication.SharedApplication.StatusBarStyle = UIStatusBarStyle.LightContent;
        }

        private void ListenYubiKey(bool listen)
        {
            if(_deviceActionService.SupportsNfc())
            {
                _nfcSession?.InvalidateSession();
                _nfcSession?.Dispose();
                _nfcSession = null;
                if(listen)
                {
                    _nfcSession = new NFCNdefReaderSession(_nfcDelegate, null, true)
                    {
                        AlertMessage = AppResources.HoldYubikeyNearTop
                    };
                    _nfcSession.BeginSession();
                }
            }
        }

        private void ShowAppExtension()
        {
            var itemProvider = new NSItemProvider(new NSDictionary(), Core.Constants.UTTypeAppExtensionSetup);
            var extensionItem = new NSExtensionItem();
            extensionItem.Attachments = new NSItemProvider[] { itemProvider };
            var activityViewController = new UIActivityViewController(new NSExtensionItem[] { extensionItem }, null);
            activityViewController.CompletionHandler = (activityType, completed) =>
            {
                // TODO
                //page.EnabledExtension(completed && activityType == "com.8bit.bitwarden.find-login-action-extension");
            };
            var modal = UIApplication.SharedApplication.KeyWindow.RootViewController.ModalViewController;
            if(activityViewController.PopoverPresentationController != null)
            {
                activityViewController.PopoverPresentationController.SourceView = modal.View;
                var frame = UIScreen.MainScreen.Bounds;
                frame.Height /= 2;
                activityViewController.PopoverPresentationController.SourceRect = frame;
            }
            modal.PresentViewController(activityViewController, true, null);
        }
    }
}
