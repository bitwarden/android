using AuthenticationServices;
using Bit.App.Abstractions;
using Bit.App.Pages;
using Bit.App.Services;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Services;
using CoreGraphics;
using CoreNFC;
using Foundation;
using Microsoft.Maui.Platform;
using UIKit;
using UserNotifications;
using WatchConnectivity;

namespace Bit.iOS
{
    [Register("AppDelegate")]
    public partial class AppDelegate : MauiUIApplicationDelegate
    {
        protected override MauiApp CreateMauiApp() => App.MauiProgram.CreateMauiApp();

        const int SPLASH_VIEW_TAG = 4321;

        private NFCNdefReaderSession _nfcSession = null;
        private iOSPushNotificationHandler _pushHandler = null;
        private Core.NFCReaderDelegate _nfcDelegate = null;
        private NSTimer _clipboardTimer = null;
        private nint _clipboardBackgroundTaskId;
        private NSTimer _eventTimer = null;
        private nint _eventBackgroundTaskId;

        private IDeviceActionService _deviceActionService;
        private IMessagingService _messagingService;
        private IBroadcasterService _broadcasterService;
        private IStorageService _storageService;
        private IStateService _stateService;
        private IEventService _eventService;

        private readonly LazyResolve<IDeepLinkContext> _deepLinkContext = new LazyResolve<IDeepLinkContext>();

        public override bool FinishedLaunching(UIApplication app, NSDictionary options)
        {
            try
            {
                InitApp();

                _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
                _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
                _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
                _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
                _stateService = ServiceContainer.Resolve<IStateService>("stateService");
                _eventService = ServiceContainer.Resolve<IEventService>("eventService");

                ConnectToWatchIfNeededAsync().FireAndForget();

                _broadcasterService.Subscribe(nameof(AppDelegate), async (message) =>
                {
                    try
                    {
                        if (message.Command == "startEventTimer")
                        {
                            StartEventTimer();
                        }
                        else if (message.Command == "stopEventTimer")
                        {
                            var task = StopEventTimerAsync();
                        }
                        else if (message.Command is ThemeManager.UPDATED_THEME_MESSAGE_KEY)
                        {
                            await MainThread.InvokeOnMainThreadAsync(() =>
                            {
                                iOSCoreHelpers.AppearanceAdjustments();
                            });
                        }
                        else if (message.Command == "listenYubiKeyOTP" && message.Data is bool listen)
                        {
                            iOSCoreHelpers.ListenYubiKey(listen, _deviceActionService, _nfcSession, _nfcDelegate);
                        }
                        else if (message.Command == "unlocked")
                        {
                            var needsAutofillReplacement = await _storageService.GetAsync<bool?>(
                                Core.Constants.AutofillNeedsIdentityReplacementKey);
                            if (needsAutofillReplacement.GetValueOrDefault())
                            {
                                await ASHelpers.ReplaceAllIdentitiesAsync();
                            }
                        }
                        else if (message.Command == "showAppExtension")
                        {
                            await MainThread.InvokeOnMainThreadAsync(() => ShowAppExtension((ExtensionPageViewModel)message.Data));
                        }
                        else if (message.Command == "syncCompleted")
                        {
                            if (message.Data is Dictionary<string, object> data && data.TryGetValue("successfully", out var value))
                            {
                                var success = value as bool?;
                                if (success.GetValueOrDefault() && _deviceActionService.SystemMajorVersion() >= 12)
                                {
                                    await ASHelpers.ReplaceAllIdentitiesAsync();
                                }
                            }
                        }
                        else if (message.Command == "addedCipher" || message.Command == "editedCipher" ||
                            message.Command == "restoredCipher")
                        {
                            if (!UIDevice.CurrentDevice.CheckSystemVersion(12, 0))
                            {
                                return;
                            }

                            if (await ASHelpers.IdentitiesSupportIncrementalAsync())
                            {
                                var cipherId = message.Data as string;
                                if (message.Command == "addedCipher" && !string.IsNullOrWhiteSpace(cipherId))
                                {
                                    var identity = await ASHelpers.GetCipherPasswordIdentityAsync(cipherId);
                                    if (identity == null)
                                    {
                                        return;
                                    }
                                    await ASCredentialIdentityStoreExtensions.SaveCredentialIdentitiesAsync(identity);
                                    return;
                                }
                            }
                            await ASHelpers.ReplaceAllIdentitiesAsync();
                        }
                        else if (message.Command == "deletedCipher" || message.Command == "softDeletedCipher")
                        {
                            if (!UIDevice.CurrentDevice.CheckSystemVersion(12, 0))
                            {
                                return;
                            }

                            if (await ASHelpers.IdentitiesSupportIncrementalAsync())
                            {
                                var identity = ASHelpers.ToPasswordCredentialIdentity(
                                    message.Data as Bit.Core.Models.View.CipherView);
                                if (identity == null)
                                {
                                    return;
                                }
                                await ASCredentialIdentityStoreExtensions.RemoveCredentialIdentitiesAsync(identity);
                                return;
                            }
                            await ASHelpers.ReplaceAllIdentitiesAsync();
                        }
                        else if (message.Command == "logout" && UIDevice.CurrentDevice.CheckSystemVersion(12, 0))
                        {
                            await ASCredentialIdentityStore.SharedStore.RemoveAllCredentialIdentitiesAsync();
                        }
                        else if ((message.Command == "softDeletedCipher" || message.Command == "restoredCipher")
                            && UIDevice.CurrentDevice.CheckSystemVersion(12, 0))
                        {
                            await ASHelpers.ReplaceAllIdentitiesAsync();
                        }
                        else if (message.Command == AppHelpers.VAULT_TIMEOUT_ACTION_CHANGED_MESSAGE_COMMAND)
                        {
                            var timeoutAction = await _stateService.GetVaultTimeoutActionAsync();
                            if (timeoutAction == VaultTimeoutAction.Logout)
                            {
                                if (UIDevice.CurrentDevice.CheckSystemVersion(12, 0))
                                {
                                    await ASCredentialIdentityStore.SharedStore.RemoveAllCredentialIdentitiesAsync();
                                }
                            }
                            else
                            {
                                await ASHelpers.ReplaceAllIdentitiesAsync();
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        LoggerHelper.LogEvenIfCantBeResolved(ex);
                    }
                });

                var finishedLaunching = base.FinishedLaunching(app, options);

                ThemeManager.SetTheme(Microsoft.Maui.Controls.Application.Current.Resources);
                iOSCoreHelpers.AppearanceAdjustments();

                return finishedLaunching;
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw;
            }
        }

        public override void OnResignActivation(UIApplication uiApplication)
        {
            try
            {
                if (UIApplication.SharedApplication.KeyWindow != null)
                {
                    var view = new UIView(UIApplication.SharedApplication.KeyWindow.Frame)
                    {
                        Tag = SPLASH_VIEW_TAG
                    };
                    var backgroundView = new UIView(UIApplication.SharedApplication.KeyWindow.Frame)
                    {
                        BackgroundColor = ThemeManager.GetResourceColor("SplashBackgroundColor").ToPlatform()
                    };
                    var logo = new UIImage(!ThemeManager.UsingLightTheme ? "logo_white.png" : "logo.png");
                    var frame = new CGRect(0, 0, 280, 100); //Setting image width to avoid it being larger and getting cropped on smaller devices. This harcoded size should be good even for very small devices.
                    var imageView = new UIImageView(frame)
                    {
                        Image = logo,
                        Center = new CGPoint(view.Center.X, view.Center.Y - 30),
                        ContentMode = UIViewContentMode.ScaleAspectFit
                    };
                    view.AddSubview(backgroundView);
                    view.AddSubview(imageView);
                    UIApplication.SharedApplication.KeyWindow.AddSubview(view);
                    UIApplication.SharedApplication.KeyWindow.BringSubviewToFront(view);
                    UIApplication.SharedApplication.KeyWindow.EndEditing(true);
                }
                base.OnResignActivation(uiApplication);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw;
            }
        }

        public override void DidEnterBackground(UIApplication uiApplication)
        {
            try
            {
                if (_stateService != null && _deviceActionService != null)
                {
                    _stateService.SetLastActiveTimeAsync(_deviceActionService.GetActiveTime());
                }

                _messagingService?.Send("slept");
                base.DidEnterBackground(uiApplication);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw;
            }
        }

        public override async void OnActivated(UIApplication uiApplication)
        {
            try
            {
                base.OnActivated(uiApplication);

                if (UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
                {
                    await UNUserNotificationCenter.Current.SetBadgeCountAsync(0);
                }
                else
                {
                    UIApplication.SharedApplication.ApplicationIconBadgeNumber = 0;
                }

                UIApplication.SharedApplication.KeyWindow?
                    .ViewWithTag(SPLASH_VIEW_TAG)?
                    .RemoveFromSuperview();

                ThemeManager.UpdateThemeOnPagesAsync();
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        public override void WillEnterForeground(UIApplication uiApplication)
        {
            try
            {
                _messagingService?.Send(AppHelpers.RESUMED_MESSAGE_COMMAND);
                base.WillEnterForeground(uiApplication);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        [Export("application:openURL:sourceApplication:annotation:")]
        public bool OpenUrl(UIApplication application, NSUrl url, string sourceApplication, NSObject annotation)
        {
            return true;
        }

        public override bool OpenUrl(UIApplication app, NSUrl url, NSDictionary options)
        {
            try
            {
                return _deepLinkContext.Value.OnNewUri(url) || base.OpenUrl(app, url, options);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                return false;
            }
        }

        public override bool ContinueUserActivity(UIApplication application, NSUserActivity userActivity,
            UIApplicationRestorationHandler completionHandler)
        {
            try
            {
                if (Microsoft.Maui.ApplicationModel.Platform.ContinueUserActivity(application, userActivity, completionHandler))
                {
                    return true;
                }
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
            return base.ContinueUserActivity(application, userActivity, completionHandler);
        }

        [Export("application:didFailToRegisterForRemoteNotificationsWithError:")]
        public void FailedToRegisterForRemoteNotifications(UIApplication application, NSError error)
        {
            try
            {
                _pushHandler?.OnErrorReceived(error);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        [Export("application:didRegisterForRemoteNotificationsWithDeviceToken:")]
        public void RegisteredForRemoteNotifications(UIApplication application, NSData deviceToken)
        {
            try
            {
                _pushHandler?.OnRegisteredSuccess(deviceToken);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        [Export("application:didRegisterUserNotificationSettings:")]
        public void DidRegisterUserNotificationSettings(UIApplication application,
            UIUserNotificationSettings notificationSettings)
        {
            try
            {
                application.RegisterForRemoteNotifications();
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        [Export("application:didReceiveRemoteNotification:fetchCompletionHandler:")]
        public void DidReceiveRemoteNotification(UIApplication application, NSDictionary userInfo,
            Action<UIBackgroundFetchResult> completionHandler)
        {
            try
            {
                _pushHandler?.OnMessageReceived(userInfo);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        [Export("application:didReceiveRemoteNotification:")]
        public void ReceivedRemoteNotification(UIApplication application, NSDictionary userInfo)
        {
            try
            {
                _pushHandler?.OnMessageReceived(userInfo);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        public void InitApp()
        {
            if (ServiceContainer.RegisteredServices.Count > 0)
            {
                return;
            }

            // Migration services
            ServiceContainer.Register<INativeLogService>("nativeLogService", new ConsoleLogService());

            iOSCoreHelpers.RegisterLocalServices();
            RegisterPush();
            var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            ServiceContainer.Init(deviceActionService.DeviceUserAgent, Constants.ClearCiphersCacheKey, 
                Constants.iOSAllClearCipherCacheKeys);
            iOSCoreHelpers.InitLogger();
            iOSCoreHelpers.RegisterFinallyBeforeBootstrap();

            _pushHandler = new iOSPushNotificationHandler(
                ServiceContainer.Resolve<IPushNotificationListenerService>("pushNotificationListenerService"));
            _nfcDelegate = new Core.NFCReaderDelegate((success, message) =>
                _messagingService.Send("gotYubiKeyOTP", message));

            iOSCoreHelpers.Bootstrap(ApplyManagedSettingsAsync);
        }

        private void RegisterPush()
        {
            var notificationListenerService = new PushNotificationListenerService();
            ServiceContainer.Register<IPushNotificationListenerService>(
                "pushNotificationListenerService", notificationListenerService);
            var iosPushNotificationService = new iOSPushNotificationService();
            ServiceContainer.Register<IPushNotificationService>(
                "pushNotificationService", iosPushNotificationService);
        }

        private void ShowAppExtension(ExtensionPageViewModel extensionPageViewModel)
        {
            var itemProvider = new NSItemProvider(new NSDictionary(), Core.Constants.UTTypeAppExtensionSetup);
            var extensionItem = new NSExtensionItem
            {
                Attachments = new NSItemProvider[] { itemProvider }
            };
            var activityViewController = new UIActivityViewController(new NSExtensionItem[] { extensionItem }, null)
            {
                CompletionHandler = (activityType, completed) =>
                {
                    extensionPageViewModel.EnabledExtension(completed && activityType == iOSCoreHelpers.AppExtensionId);
                }
            };
            var modal = UIApplication.SharedApplication.KeyWindow.RootViewController.ModalViewController;
            if (activityViewController.PopoverPresentationController != null)
            {
                activityViewController.PopoverPresentationController.SourceView = modal.View;
                var frame = UIScreen.MainScreen.Bounds;
                frame.Height /= 2;
                activityViewController.PopoverPresentationController.SourceRect = frame;
            }
            modal.PresentViewController(activityViewController, true, null);
        }

        private void StartEventTimer()
        {
            _eventTimer?.Invalidate();
            _eventTimer?.Dispose();
            _eventTimer = null;
            MainThread.BeginInvokeOnMainThread(() =>
            {
                try
                {
                    _eventTimer = NSTimer.CreateScheduledTimer(60, true, timer =>
                    {
                        _eventService?.UploadEventsAsync().FireAndForget();
                    });
                }
                catch (Exception ex)
                {
                    LoggerHelper.LogEvenIfCantBeResolved(ex);
                }
            });
        }

        private async Task StopEventTimerAsync()
        {
            try
            {
                _eventTimer?.Invalidate();
                _eventTimer?.Dispose();
                _eventTimer = null;
                if (_eventBackgroundTaskId > 0)
                {
                    UIApplication.SharedApplication.EndBackgroundTask(_eventBackgroundTaskId);
                    _eventBackgroundTaskId = 0;
                }
                _eventBackgroundTaskId = UIApplication.SharedApplication.BeginBackgroundTask(() =>
                {
                    UIApplication.SharedApplication.EndBackgroundTask(_eventBackgroundTaskId);
                    _eventBackgroundTaskId = 0;
                });
                await _eventService.UploadEventsAsync();
                UIApplication.SharedApplication.EndBackgroundTask(_eventBackgroundTaskId);
                _eventBackgroundTaskId = 0;
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        private async Task ApplyManagedSettingsAsync()
        {
            var userDefaults = NSUserDefaults.StandardUserDefaults;
            var managedSettings = userDefaults.DictionaryForKey("com.apple.configuration.managed");
            if (managedSettings != null && managedSettings.Count > 0)
            {
                var dict = new Dictionary<string, string>();
                foreach (var setting in managedSettings)
                {
                    dict.Add(setting.Key.ToString(), setting.Value?.ToString());
                }
                await AppHelpers.SetPreconfiguredSettingsAsync(dict);
            }
        }

        private async Task ConnectToWatchIfNeededAsync()
        {
            if (_stateService != null && await _stateService.GetShouldConnectToWatchAsync())
            {
                WCSessionManager.SharedManager.StartSession();
            }
        }
    }
}
