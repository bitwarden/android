using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Views;
using Android.Views.Accessibility;
using Android.Widget;
using Bit.App.Resources;
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
        private const string BitwardenPackage = "com.x8bit.bitwarden";
        private const string BitwardenWebsite = "vault.bitwarden.com";

        private IStateService _stateService;
        private IBroadcasterService _broadcasterService;
        private DateTime? _lastSettingsReload = null;
        private TimeSpan _settingsReloadSpan = TimeSpan.FromMinutes(1);
        private HashSet<string> _blacklistedUris;
        private AccessibilityNodeInfo _anchorNode = null;
        private int _lastAnchorX = 0;
        private int _lastAnchorY = 0;
        private bool _isOverlayAboveAnchor = false;
        private static bool _overlayAnchorObserverRunning = false;
        private IWindowManager _windowManager = null;
        private LinearLayout _overlayView = null;
        private int _overlayViewHeight = 0;
        private long _lastAutoFillTime = 0;
        private Java.Lang.Runnable _overlayAnchorObserverRunnable = null;
        private Handler _handler = new Handler(Looper.MainLooper);

        private HashSet<string> _launcherPackageNames = null;
        private DateTime? _lastLauncherSetBuilt = null;
        private TimeSpan _rebuildLauncherSpan = TimeSpan.FromHours(1);

        public override void OnCreate()
        {
            base.OnCreate();
            LoadServices();
            var settingsTask = LoadSettingsAsync();
            _broadcasterService.Subscribe(nameof(AccessibilityService), (message) =>
            {
                if (message.Command == "OnAutofillTileClick")
                {
                    var runnable = new Java.Lang.Runnable(OnAutofillTileClick);
                    _handler.PostDelayed(runnable, 250);
                }
            });
            AccessibilityHelpers.IsAccessibilityBroadcastReady = true;
        }

        public override void OnDestroy()
        {
            AccessibilityHelpers.IsAccessibilityBroadcastReady = false;
            _broadcasterService.Unsubscribe(nameof(AccessibilityService));
        }

        public override void OnAccessibilityEvent(AccessibilityEvent e)
        {
            try
            {
                var powerManager = GetSystemService(PowerService) as PowerManager;
                if (Build.VERSION.SdkInt > BuildVersionCodes.KitkatWatch && !powerManager.IsInteractive)
                {
                    return;
                }
                else if (Build.VERSION.SdkInt < BuildVersionCodes.Lollipop && !powerManager.IsScreenOn)
                {
                    return;
                }

                if (SkipPackage(e?.PackageName))
                {
                    if (e?.PackageName != "com.android.systemui")
                    {
                        CancelOverlayPrompt();
                    }
                    return;
                }

                // AccessibilityHelpers.PrintTestData(RootInActiveWindow, e);

                LoadServices();
                var settingsTask = LoadSettingsAsync();
                AccessibilityNodeInfo root = null;

                switch (e.EventType)
                {
                    case EventTypes.ViewFocused:
                    case EventTypes.ViewClicked:
                        if (e.Source == null || e.PackageName == BitwardenPackage)
                        {
                            CancelOverlayPrompt();
                            break;
                        }

                        root = RootInActiveWindow;
                        if (root == null || root.PackageName != e.PackageName)
                        {
                            break;
                        }

                        if (!(e.Source?.Password ?? false) && !AccessibilityHelpers.IsUsernameEditText(root, e))
                        {
                            CancelOverlayPrompt();
                            break;
                        }
                        if (ScanAndAutofill(root, e))
                        {
                            CancelOverlayPrompt();
                        }
                        else
                        {
                            OverlayPromptToAutofill(root, e);
                        }
                        break;
                    case EventTypes.WindowContentChanged:
                    case EventTypes.WindowStateChanged:
                        if (AccessibilityHelpers.LastCredentials == null)
                        {
                            break;
                        }
                        if (e.PackageName == BitwardenPackage)
                        {
                            CancelOverlayPrompt();
                            break;
                        }

                        root = RootInActiveWindow;
                        if (root == null || root.PackageName != e.PackageName)
                        {
                            break;
                        }
                        if (ScanAndAutofill(root, e))
                        {
                            CancelOverlayPrompt();
                        }
                        break;
                    default:
                        break;
                }
            }
            // Suppress exceptions so that service doesn't crash.
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine(">>> {0}: {1}", ex.GetType(), ex.StackTrace);
            }
        }

        public override void OnInterrupt()
        {
            // Do nothing.
        }

        public bool ScanAndAutofill(AccessibilityNodeInfo root, AccessibilityEvent e)
        {
            var filled = false;
            var uri = AccessibilityHelpers.GetUri(root);
            if (uri != null && !uri.Contains(BitwardenWebsite) &&
                AccessibilityHelpers.NeedToAutofill(AccessibilityHelpers.LastCredentials, uri))
            {
                var allEditTexts = AccessibilityHelpers.GetWindowNodes(root, e, n => AccessibilityHelpers.EditText(n), false);
                var usernameEditText = AccessibilityHelpers.GetUsernameEditText(uri, allEditTexts);
                var passwordNodes = AccessibilityHelpers.GetWindowNodes(root, e, n => n.Password, false);
                if (usernameEditText != null || passwordNodes.Count > 0)
                {
                    AccessibilityHelpers.FillCredentials(usernameEditText, passwordNodes);
                    filled = true;
                    _lastAutoFillTime = Java.Lang.JavaSystem.CurrentTimeMillis();
                    AccessibilityHelpers.LastCredentials = null;
                }
                allEditTexts.Dispose();
                passwordNodes.Dispose();
            }
            if (AccessibilityHelpers.LastCredentials != null)
            {
                Task.Run(async () =>
                {
                    await Task.Delay(1000);
                    AccessibilityHelpers.LastCredentials = null;
                });
            }
            return filled;
        }
        
        private void OnAutofillTileClick()
        {
            CancelOverlayPrompt();
            
            var root = RootInActiveWindow;
            if (root != null && root.PackageName != BitwardenPackage &&
               root.PackageName != AccessibilityHelpers.SystemUiPackage &&
               !SkipPackage(root.PackageName))
            {
                var uri = AccessibilityHelpers.GetUri(root);
                if (!string.IsNullOrWhiteSpace(uri))
                {
                    var intent = new Intent(this, typeof(AccessibilityActivity));
                    intent.PutExtra("uri", uri);
                    intent.SetFlags(ActivityFlags.NewTask | ActivityFlags.SingleTop | ActivityFlags.ClearTop);
                    StartActivity(intent);
                    return;
                }
            }
            
            Toast.MakeText(this, AppResources.AutofillTileUriNotFound, ToastLength.Long).Show();
        }

        private void CancelOverlayPrompt()
        {
            _overlayAnchorObserverRunning = false;

            if (_windowManager != null && _overlayView != null)
            {
                try
                {
                    _windowManager.RemoveViewImmediate(_overlayView);
                    System.Diagnostics.Debug.WriteLine(">>> Accessibility Overlay View Removed");
                }
                catch { }
            }

            _overlayView = null;
            _lastAnchorX = 0;
            _lastAnchorY = 0;
            _isOverlayAboveAnchor = false;

            if (_anchorNode != null)
            {
                _anchorNode.Recycle();
                _anchorNode = null;
            }
        }

        private void OverlayPromptToAutofill(AccessibilityNodeInfo root, AccessibilityEvent e)
        {
            if (Java.Lang.JavaSystem.CurrentTimeMillis() - _lastAutoFillTime < 1000 ||
                AccessibilityHelpers.IsAutofillServicePromptVisible(Windows))
            {
                return;
            }
            
            if (!AccessibilityHelpers.OverlayPermitted())
            {
                if (Build.VERSION.SdkInt <= BuildVersionCodes.M)
                {
                    // The user has the option of only using the autofill tile and leaving the overlay permission
                    // disabled, so only show this toast if they're using accessibility without overlay permission on
                    // a version of Android without quick-action tile support
                    System.Diagnostics.Debug.WriteLine(">>> Overlay Permission not granted");
                    Toast.MakeText(this, AppResources.AccessibilityDrawOverPermissionAlert, ToastLength.Long).Show();   
                }
                return;
            }

            if (_overlayView != null || _anchorNode != null || _overlayAnchorObserverRunning)
            {
                CancelOverlayPrompt();
            }

            var uri = AccessibilityHelpers.GetUri(root);
            var fillable = !string.IsNullOrWhiteSpace(uri);
            if (fillable)
            {
                if (_blacklistedUris != null && _blacklistedUris.Any())
                {
                    if (Uri.TryCreate(uri, UriKind.Absolute, out var parsedUri) && parsedUri.Scheme.StartsWith("http"))
                    {
                        fillable = !_blacklistedUris.Contains(
                            string.Format("{0}://{1}", parsedUri.Scheme, parsedUri.Host));
                    }
                    else
                    {
                        fillable = !_blacklistedUris.Contains(uri);
                    }
                }
            }
            if (!fillable)
            {
                return;
            }

            var intent = new Intent(this, typeof(AccessibilityActivity));
            intent.PutExtra("uri", uri);
            intent.SetFlags(ActivityFlags.NewTask | ActivityFlags.SingleTop | ActivityFlags.ClearTop);

            _overlayView = AccessibilityHelpers.GetOverlayView(this);
            _overlayView.Measure(View.MeasureSpec.MakeMeasureSpec(0, 0),
                View.MeasureSpec.MakeMeasureSpec(0, 0));
            _overlayViewHeight = _overlayView.MeasuredHeight;
            _overlayView.Click += (sender, eventArgs) =>
            {
                CancelOverlayPrompt();
                StartActivity(intent);
            };

            var layoutParams = AccessibilityHelpers.GetOverlayLayoutParams();
            var anchorPosition = AccessibilityHelpers.GetOverlayAnchorPosition(this, e.Source, 
                _overlayViewHeight, _isOverlayAboveAnchor);
            layoutParams.X = anchorPosition.X;
            layoutParams.Y = anchorPosition.Y;

            if (_windowManager == null)
            {
                _windowManager = GetSystemService(WindowService).JavaCast<IWindowManager>();
            }

            _anchorNode = e.Source;
            _lastAnchorX = anchorPosition.X;
            _lastAnchorY = anchorPosition.Y;

            _windowManager.AddView(_overlayView, layoutParams);

            System.Diagnostics.Debug.WriteLine(">>> Accessibility Overlay View Added at X:{0} Y:{1}",
                layoutParams.X, layoutParams.Y);

            StartOverlayAnchorObserver();
        }

        private void StartOverlayAnchorObserver()
        {
            if (_overlayAnchorObserverRunning)
            {
                return;
            }

            _overlayAnchorObserverRunning = true;
            _overlayAnchorObserverRunnable = new Java.Lang.Runnable(() =>
            {
                if (_overlayAnchorObserverRunning)
                {
                    AdjustOverlayForScroll();
                    _handler.PostDelayed(_overlayAnchorObserverRunnable, 250);
                }
            });

            _handler.PostDelayed(_overlayAnchorObserverRunnable, 250);
        }

        private void AdjustOverlayForScroll()
        {
            if (_overlayView == null || _anchorNode == null || 
                AccessibilityHelpers.IsAutofillServicePromptVisible(Windows))
            {
                CancelOverlayPrompt();
                return;
            }

            var root = RootInActiveWindow;
            IEnumerable<AccessibilityWindowInfo> windows = null;
            if (Build.VERSION.SdkInt > BuildVersionCodes.Kitkat)
            {
                windows = Windows;
            }

            var anchorPosition = AccessibilityHelpers.GetOverlayAnchorPosition(this, _anchorNode, root, 
                windows, _overlayViewHeight, _isOverlayAboveAnchor);
            if (anchorPosition == null)
            {
                CancelOverlayPrompt();
                return;
            }
            else if (anchorPosition.X == -1 && anchorPosition.Y == -1)
            {
                if (_overlayView.Visibility != ViewStates.Gone)
                {
                    _overlayView.Visibility = ViewStates.Gone;
                    System.Diagnostics.Debug.WriteLine(">>> Accessibility Overlay View Hidden");
                }
                return;
            }
            else if (anchorPosition.X == -1)
            {
                _isOverlayAboveAnchor = false;
                System.Diagnostics.Debug.WriteLine(">>> Accessibility Overlay View Below Anchor");
                return;
            }
            else if (anchorPosition.Y == -1)
            {
                _isOverlayAboveAnchor = true;
                System.Diagnostics.Debug.WriteLine(">>> Accessibility Overlay View Above Anchor");
                return;
            }
            else if (anchorPosition.X == _lastAnchorX && anchorPosition.Y == _lastAnchorY)
            {
                if (_overlayView.Visibility != ViewStates.Visible)
                {
                    _overlayView.Visibility = ViewStates.Visible;
                }
                return;
            }

            var layoutParams = AccessibilityHelpers.GetOverlayLayoutParams();
            layoutParams.X = anchorPosition.X;
            layoutParams.Y = anchorPosition.Y;

            _lastAnchorX = anchorPosition.X;
            _lastAnchorY = anchorPosition.Y;

            _windowManager.UpdateViewLayout(_overlayView, layoutParams);

            if (_overlayView.Visibility != ViewStates.Visible)
            {
                _overlayView.Visibility = ViewStates.Visible;
            }

            System.Diagnostics.Debug.WriteLine(">>> Accessibility Overlay View Updated to X:{0} Y:{1}",
                    layoutParams.X, layoutParams.Y);
        }

        private bool SkipPackage(string eventPackageName)
        {
            if (string.IsNullOrWhiteSpace(eventPackageName) ||
                AccessibilityHelpers.FilteredPackageNames.Contains(eventPackageName) ||
                eventPackageName.Contains("launcher"))
            {
                return true;
            }
            if (_launcherPackageNames == null || _lastLauncherSetBuilt == null ||
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
            if (_stateService == null)
            {
                _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            }
            if (_broadcasterService == null)
            {
                _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            }
        }

        private async Task LoadSettingsAsync()
        {
            var now = DateTime.UtcNow;
            if (_lastSettingsReload == null || (now - _lastSettingsReload.Value) > _settingsReloadSpan)
            {
                _lastSettingsReload = now;
                var uris = await _stateService.GetAutofillBlacklistedUrisAsync();
                if (uris != null)
                {
                    _blacklistedUris = new HashSet<string>(uris);
                }
                var isAutoFillTileAdded = await _stateService.GetAutofillTileAddedAsync();
                AccessibilityHelpers.IsAutofillTileAdded = isAutoFillTileAdded.GetValueOrDefault();
            }
        }
    }
}
