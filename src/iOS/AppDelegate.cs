using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using AuthenticationServices;
using Bit.App.Abstractions;
using Bit.App.Pages;
using Bit.App.Resources;
using Bit.App.Services;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Services;
using CoreNFC;
using Foundation;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

namespace Bit.iOS
{
    [Register("AppDelegate")]
    public partial class AppDelegate : FormsApplicationDelegate
    {
        private NFCNdefReaderSession _nfcSession = null;
        private iOSPushNotificationHandler _pushHandler = null;
        private NFCReaderDelegate _nfcDelegate = null;
        private NSTimer _clipboardTimer = null;
        private nint _clipboardBackgroundTaskId;
        private NSTimer _lockTimer = null;
        private nint _lockBackgroundTaskId;
        private NSTimer _eventTimer = null;
        private nint _eventBackgroundTaskId;

        private IDeviceActionService _deviceActionService;
        private IMessagingService _messagingService;
        private IBroadcasterService _broadcasterService;
        private IStorageService _storageService;
        private ILockService _lockService;
        private IEventService _eventService;

        public override bool FinishedLaunching(UIApplication app, NSDictionary options)
        {
            Forms.Init();
            InitApp();
            if(App.Migration.MigrationHelpers.NeedsMigration())
            {
                var task = App.Migration.MigrationHelpers.PerformMigrationAsync();
                Task.Delay(5000).Wait();
            }

            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _lockService = ServiceContainer.Resolve<ILockService>("lockService");
            _eventService = ServiceContainer.Resolve<IEventService>("eventService");

            LoadApplication(new App.App(null));
            iOSCoreHelpers.AppearanceAdjustments();
            ZXing.Net.Mobile.Forms.iOS.Platform.Init();

            _broadcasterService.Subscribe(nameof(AppDelegate), async (message) =>
            {
                if(message.Command == "scheduleLockTimer")
                {
                    LockTimer((int)message.Data);
                }
                else if(message.Command == "cancelLockTimer")
                {
                    CancelLockTimer();
                }
                else if(message.Command == "startEventTimer")
                {
                    StartEventTimer();
                }
                else if(message.Command == "stopEventTimer")
                {
                    var task = StopEventTimerAsync();
                }
                else if(message.Command == "updatedTheme")
                {
                    // ThemeManager.SetThemeStyle(message.Data as string);
                }
                else if(message.Command == "copiedToClipboard")
                {

                    Device.BeginInvokeOnMainThread(() =>
                    {
                        var task = ClearClipboardTimerAsync(message.Data as Tuple<string, int?, bool>);
                    });
                }
                else if(message.Command == "listenYubiKeyOTP")
                {
                    ListenYubiKey((bool)message.Data);
                }
                else if(message.Command == "showAppExtension")
                {
                    Device.BeginInvokeOnMainThread(() => ShowAppExtension((ExtensionPageViewModel)message.Data));
                }
                else if(message.Command == "showStatusBar")
                {
                    Device.BeginInvokeOnMainThread(() =>
                        UIApplication.SharedApplication.SetStatusBarHidden(!(bool)message.Data, false));
                }
                else if(message.Command == "syncCompleted")
                {
                    if(message.Data is Dictionary<string, object> data && data.ContainsKey("successfully"))
                    {
                        var success = data["successfully"] as bool?;
                        if(success.GetValueOrDefault() && _deviceActionService.SystemMajorVersion() >= 12)
                        {
                            await ASHelpers.ReplaceAllIdentities();
                        }
                    }
                }
                else if(message.Command == "addedCipher" || message.Command == "editedCipher")
                {
                    if(_deviceActionService.SystemMajorVersion() >= 12)
                    {
                        if(await ASHelpers.IdentitiesCanIncremental())
                        {
                            var cipherId = message.Data as string;
                            if(message.Command == "addedCipher" && !string.IsNullOrWhiteSpace(cipherId))
                            {
                                var identity = await ASHelpers.GetCipherIdentityAsync(cipherId);
                                if(identity == null)
                                {
                                    return;
                                }
                                await ASCredentialIdentityStore.SharedStore?.SaveCredentialIdentitiesAsync(
                                    new ASPasswordCredentialIdentity[] { identity });
                                return;
                            }
                        }
                        await ASHelpers.ReplaceAllIdentities();
                    }
                }
                else if(message.Command == "deletedCipher")
                {
                    if(_deviceActionService.SystemMajorVersion() >= 12)
                    {
                        if(await ASHelpers.IdentitiesCanIncremental())
                        {
                            var identity = ASHelpers.ToCredentialIdentity(
                                message.Data as Bit.Core.Models.View.CipherView);
                            if(identity == null)
                            {
                                return;
                            }
                            await ASCredentialIdentityStore.SharedStore?.RemoveCredentialIdentitiesAsync(
                                new ASPasswordCredentialIdentity[] { identity });
                            return;
                        }
                        await ASHelpers.ReplaceAllIdentities();
                    }
                }
                else if(message.Command == "loggedOut")
                {
                    if(_deviceActionService.SystemMajorVersion() >= 12)
                    {
                        await ASCredentialIdentityStore.SharedStore?.RemoveAllCredentialIdentitiesAsync();
                    }
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
            _storageService.SaveAsync(Constants.LastActiveKey, DateTime.UtcNow);
            _messagingService.Send("slept");
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

        public void InitApp()
        {
            if(ServiceContainer.RegisteredServices.Count > 0)
            {
                return;
            }

            // Migration services
            ServiceContainer.Register<ILogService>("logService", new ConsoleLogService());
            ServiceContainer.Register("settingsShim", new App.Migration.SettingsShim(iOSCoreHelpers.AppGroupId));
            if(App.Migration.MigrationHelpers.NeedsMigration())
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

            iOSCoreHelpers.RegisterLocalServices();
            RegisterPush();
            ServiceContainer.Init();
            iOSCoreHelpers.RegisterHockeyApp();
            _pushHandler = new iOSPushNotificationHandler(
                ServiceContainer.Resolve<IPushNotificationListenerService>("pushNotificationListenerService"));
            _nfcDelegate = new NFCReaderDelegate((success, message) =>
                _messagingService.Send("gotYubiKeyOTP", message));

            iOSCoreHelpers.Bootstrap();
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

        private void LockTimer(int lockOptionMinutes)
        {
            if(_lockBackgroundTaskId > 0)
            {
                UIApplication.SharedApplication.EndBackgroundTask(_lockBackgroundTaskId);
                _lockBackgroundTaskId = 0;
            }
            _lockBackgroundTaskId = UIApplication.SharedApplication.BeginBackgroundTask(() =>
            {
                UIApplication.SharedApplication.EndBackgroundTask(_lockBackgroundTaskId);
                _lockBackgroundTaskId = 0;
            });
            var lockOptionMs = lockOptionMinutes * 60000;
            _lockTimer?.Invalidate();
            _lockTimer?.Dispose();
            _lockTimer = null;
            var lockMsSpan = TimeSpan.FromMilliseconds(lockOptionMs + 10);
            Device.BeginInvokeOnMainThread(() =>
            {
                _lockTimer = NSTimer.CreateScheduledTimer(lockMsSpan, timer =>
                {
                    Device.BeginInvokeOnMainThread(() =>
                    {
                        _lockService.CheckLockAsync();
                        _lockTimer?.Invalidate();
                        _lockTimer?.Dispose();
                        _lockTimer = null;
                        if(_lockBackgroundTaskId > 0)
                        {
                            UIApplication.SharedApplication.EndBackgroundTask(_lockBackgroundTaskId);
                            _lockBackgroundTaskId = 0;
                        }
                    });
                });
            });
        }

        private void CancelLockTimer()
        {
            _lockTimer?.Invalidate();
            _lockTimer?.Dispose();
            _lockTimer = null;
            if(_lockBackgroundTaskId > 0)
            {
                UIApplication.SharedApplication.EndBackgroundTask(_lockBackgroundTaskId);
                _lockBackgroundTaskId = 0;
            }
        }

        private async Task ClearClipboardTimerAsync(Tuple<string, int?, bool> data)
        {
            if(data.Item3)
            {
                return;
            }
            var clearMs = data.Item2;
            if(clearMs == null)
            {
                var clearSeconds = await _storageService.GetAsync<int?>(Constants.ClearClipboardKey);
                if(clearSeconds != null)
                {
                    clearMs = clearSeconds.Value * 1000;
                }
            }
            if(clearMs == null)
            {
                return;
            }
            if(_clipboardBackgroundTaskId > 0)
            {
                UIApplication.SharedApplication.EndBackgroundTask(_clipboardBackgroundTaskId);
                _clipboardBackgroundTaskId = 0;
            }
            _clipboardBackgroundTaskId = UIApplication.SharedApplication.BeginBackgroundTask(() =>
            {
                UIApplication.SharedApplication.EndBackgroundTask(_clipboardBackgroundTaskId);
                _clipboardBackgroundTaskId = 0;
            });
            _clipboardTimer?.Invalidate();
            _clipboardTimer?.Dispose();
            _clipboardTimer = null;
            var lastClipboardChangeCount = UIPasteboard.General.ChangeCount;
            var clearMsSpan = TimeSpan.FromMilliseconds(clearMs.Value);
            _clipboardTimer = NSTimer.CreateScheduledTimer(clearMsSpan, timer =>
            {
                Device.BeginInvokeOnMainThread(() =>
                {
                    var changeNow = UIPasteboard.General.ChangeCount;
                    if(changeNow == 0 || lastClipboardChangeCount == changeNow)
                    {
                        UIPasteboard.General.String = string.Empty;
                    }
                    _clipboardTimer?.Invalidate();
                    _clipboardTimer?.Dispose();
                    _clipboardTimer = null;
                    if(_clipboardBackgroundTaskId > 0)
                    {
                        UIApplication.SharedApplication.EndBackgroundTask(_clipboardBackgroundTaskId);
                        _clipboardBackgroundTaskId = 0;
                    }
                });
            });
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
            if(activityViewController.PopoverPresentationController != null)
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
            Device.BeginInvokeOnMainThread(() =>
            {
                _eventTimer = NSTimer.CreateScheduledTimer(60, true, timer =>
                {
                    var task = Task.Run(() => _eventService.UploadEventsAsync());
                });
            });
        }

        private async Task StopEventTimerAsync()
        {
            _eventTimer?.Invalidate();
            _eventTimer?.Dispose();
            _eventTimer = null;
            if(_eventBackgroundTaskId > 0)
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
    }
}
