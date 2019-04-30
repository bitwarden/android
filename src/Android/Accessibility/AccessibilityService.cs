using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Views.Accessibility;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;

namespace Bit.Droid.Accessibility
{
    [Service(Permission = Android.Manifest.Permission.BindAccessibilityService, Label = "Bitwarden")]
    [IntentFilter(new string[] { "android.accessibilityservice.AccessibilityService" })]
    [MetaData("android.accessibilityservice", Resource = "@xml/accessibilityservice")]
    [Register("com.x8bit.bitwarden.Accessibility.AccessibilityService")]
    public class AccessibilityService : Android.AccessibilityServices.AccessibilityService
    {
        private NotificationChannel _notificationChannel;

        private const int AutoFillNotificationId = 34573;
        private const string BitwardenPackage = "com.x8bit.bitwarden";
        private const string BitwardenWebsite = "vault.bitwarden.com";

        private IStorageService _storageService;
        private bool _settingAutofillPasswordField;
        private bool _settingAutofillPersistNotification;
        private DateTime? _lastSettingsReload = null;
        private TimeSpan _settingsReloadSpan = TimeSpan.FromMinutes(1);
        private long _lastNotificationTime = 0;
        private string _lastNotificationUri = null;
        private HashSet<string> _launcherPackageNames = null;
        private DateTime? _lastLauncherSetBuilt = null;
        private TimeSpan _rebuildLauncherSpan = TimeSpan.FromHours(1);

        public override void OnAccessibilityEvent(AccessibilityEvent e)
        {
            try
            {
                var powerManager = GetSystemService(PowerService) as PowerManager;
                if(Build.VERSION.SdkInt > BuildVersionCodes.KitkatWatch && !powerManager.IsInteractive)
                {
                    return;
                }
                else if(Build.VERSION.SdkInt < BuildVersionCodes.Lollipop && !powerManager.IsScreenOn)
                {
                    return;
                }

                if(SkipPackage(e?.PackageName))
                {
                    return;
                }

                var root = RootInActiveWindow;
                if(root == null || root.PackageName != e.PackageName)
                {
                    return;
                }

                // AccessibilityHelpers.PrintTestData(root, e);
                LoadServices();
                var settingsTask = LoadSettingsAsync();

                var notificationManager = GetSystemService(NotificationService) as NotificationManager;
                var cancelNotification = true;

                switch(e.EventType)
                {
                    case EventTypes.ViewFocused:
                        if(e.Source == null || !e.Source.Password || !_settingAutofillPasswordField)
                        {
                            break;
                        }
                        if(e.PackageName == BitwardenPackage)
                        {
                            CancelNotification(notificationManager);
                            break;
                        }
                        if(ScanAndAutofill(root, e, notificationManager, cancelNotification))
                        {
                            CancelNotification(notificationManager);
                        }
                        break;
                    case EventTypes.WindowContentChanged:
                    case EventTypes.WindowStateChanged:
                        if(_settingAutofillPasswordField && e.Source.Password)
                        {
                            break;
                        }
                        else if(_settingAutofillPasswordField && AccessibilityHelpers.LastCredentials == null)
                        {
                            if(string.IsNullOrWhiteSpace(_lastNotificationUri))
                            {
                                CancelNotification(notificationManager);
                                break;
                            }
                            var uri = AccessibilityHelpers.GetUri(root);
                            if(uri != _lastNotificationUri)
                            {
                                CancelNotification(notificationManager);
                            }
                            else if(uri.StartsWith(Constants.AndroidAppProtocol))
                            {
                                CancelNotification(notificationManager, 30000);
                            }
                            break;
                        }

                        if(e.PackageName == BitwardenPackage)
                        {
                            CancelNotification(notificationManager);
                            break;
                        }

                        if(_settingAutofillPersistNotification)
                        {
                            var uri = AccessibilityHelpers.GetUri(root);
                            if(uri != null && !uri.Contains(BitwardenWebsite))
                            {
                                var needToFill = AccessibilityHelpers.NeedToAutofill(
                                    AccessibilityHelpers.LastCredentials, uri);
                                if(needToFill)
                                {
                                    var passwordNodes = AccessibilityHelpers.GetWindowNodes(root, e,
                                        n => n.Password, false);
                                    needToFill = passwordNodes.Any();
                                    if(needToFill)
                                    {
                                        AccessibilityHelpers.GetNodesAndFill(root, e, passwordNodes);
                                    }
                                    passwordNodes.Dispose();
                                }
                                if(!needToFill)
                                {
                                    NotifyToAutofill(uri, notificationManager);
                                    cancelNotification = false;
                                }
                            }
                            AccessibilityHelpers.LastCredentials = null;
                        }
                        else
                        {
                            cancelNotification = ScanAndAutofill(root, e, notificationManager, cancelNotification);
                        }

                        if(cancelNotification)
                        {
                            CancelNotification(notificationManager);
                        }
                        break;
                    default:
                        break;
                }

                notificationManager?.Dispose();
                root.Dispose();
                e.Dispose();
            }
            // Suppress exceptions so that service doesn't crash.
            catch { }
        }

        public override void OnInterrupt()
        {
            // Do nothing.
        }

        public bool ScanAndAutofill(AccessibilityNodeInfo root, AccessibilityEvent e,
            NotificationManager notificationManager, bool cancelNotification)
        {
            var passwordNodes = AccessibilityHelpers.GetWindowNodes(root, e, n => n.Password, false);
            if(passwordNodes.Count > 0)
            {
                var uri = AccessibilityHelpers.GetUri(root);
                if(uri != null && !uri.Contains(BitwardenWebsite))
                {
                    if(AccessibilityHelpers.NeedToAutofill(AccessibilityHelpers.LastCredentials, uri))
                    {
                        AccessibilityHelpers.GetNodesAndFill(root, e, passwordNodes);
                    }
                    else
                    {
                        NotifyToAutofill(uri, notificationManager);
                        cancelNotification = false;
                    }
                }
                AccessibilityHelpers.LastCredentials = null;
            }
            else if(AccessibilityHelpers.LastCredentials != null)
            {
                Task.Run(async () =>
                {
                    await Task.Delay(1000);
                    AccessibilityHelpers.LastCredentials = null;
                });
            }
            passwordNodes.Dispose();
            return cancelNotification;
        }

        public void CancelNotification(NotificationManager notificationManager, long limit = 250)
        {
            if(Java.Lang.JavaSystem.CurrentTimeMillis() - _lastNotificationTime < limit)
            {
                return;
            }
            _lastNotificationUri = null;
            notificationManager?.Cancel(AutoFillNotificationId);
        }

        private void NotifyToAutofill(string uri, NotificationManager notificationManager)
        {
            if(notificationManager == null || string.IsNullOrWhiteSpace(uri))
            {
                return;
            }

            var now = Java.Lang.JavaSystem.CurrentTimeMillis();
            var intent = new Intent(this, typeof(AccessibilityActivity));
            intent.PutExtra("uri", uri);
            intent.SetFlags(ActivityFlags.NewTask | ActivityFlags.SingleTop | ActivityFlags.ClearTop);
            var pendingIntent = PendingIntent.GetActivity(this, 0, intent, PendingIntentFlags.UpdateCurrent);

            var notificationContent = Build.VERSION.SdkInt > BuildVersionCodes.KitkatWatch ?
                AppResources.BitwardenAutofillServiceNotificationContent :
                AppResources.BitwardenAutofillServiceNotificationContentOld;

            var builder = new Notification.Builder(this);
            builder.SetSmallIcon(Resource.Drawable.notification_sm)
                   .SetContentTitle(AppResources.BitwardenAutofillService)
                   .SetContentText(notificationContent)
                   .SetTicker(notificationContent)
                   .SetWhen(now)
                   .SetContentIntent(pendingIntent);

            if(Build.VERSION.SdkInt > BuildVersionCodes.KitkatWatch)
            {
                builder.SetVisibility(NotificationVisibility.Secret)
                    .SetColor(Android.Support.V4.Content.ContextCompat.GetColor(ApplicationContext,
                        Resource.Color.primary));
            }
            if(Build.VERSION.SdkInt >= BuildVersionCodes.O)
            {
                if(_notificationChannel == null)
                {
                    _notificationChannel = new NotificationChannel("bitwarden_autofill_service",
                        AppResources.AutofillService, NotificationImportance.Low);
                    notificationManager.CreateNotificationChannel(_notificationChannel);
                }
                builder.SetChannelId(_notificationChannel.Id);
            }
            if(/*Build.VERSION.SdkInt <= BuildVersionCodes.N && */_settingAutofillPersistNotification)
            {
                builder.SetPriority(-2);
            }

            _lastNotificationTime = now;
            _lastNotificationUri = uri;
            notificationManager.Notify(AutoFillNotificationId, builder.Build());
            builder.Dispose();
        }

        private bool SkipPackage(string eventPackageName)
        {
            if(string.IsNullOrWhiteSpace(eventPackageName) ||
                AccessibilityHelpers.FilteredPackageNames.Contains(eventPackageName) ||
                eventPackageName.Contains("launcher"))
            {
                return true;
            }
            if(_launcherPackageNames == null || _lastLauncherSetBuilt == null ||
                (DateTime.Now - _lastLauncherSetBuilt.Value) > _rebuildLauncherSpan)
            {
                // refresh launcher list every now and then
                _lastLauncherSetBuilt = DateTime.Now;
                var intent = new Intent(Intent.ActionMain);
                intent.AddCategory(Intent.CategoryHome);
                var resolveInfo = PackageManager.QueryIntentActivities(intent, 0);
                _launcherPackageNames = resolveInfo.Select(ri => ri.ActivityInfo.PackageName).ToHashSet();
            }
            return _launcherPackageNames.Contains(eventPackageName);
        }

        private void LoadServices()
        {
            if(_storageService == null)
            {
                _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            }
        }

        private async Task LoadSettingsAsync()
        {
            var now = DateTime.UtcNow;
            if(_lastSettingsReload == null || (now - _lastSettingsReload.Value) > _settingsReloadSpan)
            {
                _lastSettingsReload = now;
                _settingAutofillPasswordField = await _storageService.GetAsync<bool>(
                    Constants.AccessibilityAutofillPasswordFieldKey);
                _settingAutofillPersistNotification = await _storageService.GetAsync<bool>(
                    Constants.AccessibilityAutofillPersistNotificationKey);
            }
        }
    }
}
