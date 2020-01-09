using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Android.App;
using Android.Content;
using Android.Graphics;
using Android.OS;
using Android.Provider;
using Android.Runtime;
using Android.Views;
using Android.Views.Accessibility;
using Android.Widget;
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
        private const string BitwardenPackage = "com.x8bit.bitwarden";
        private const string BitwardenWebsite = "vault.bitwarden.com";

        private string _lastNotificationUri = null;

        private HashSet<string> _launcherPackageNames = null;
        private DateTime? _lastLauncherSetBuilt = null;
        private TimeSpan _rebuildLauncherSpan = TimeSpan.FromHours(1);

        private IWindowManager _windowManager = null;
        private LinearLayout _overlayView = null;

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
                    CancelOverlayPrompt();
                    return;
                }

                var root = RootInActiveWindow;
                if(root == null || root.PackageName != e.PackageName)
                {
                    return;
                }

                // AccessibilityHelpers.PrintTestData(root, e);

                switch(e.EventType)
                {
                    case EventTypes.ViewFocused:
                    case EventTypes.ViewClicked:
                        var isKnownBroswer = AccessibilityHelpers.SupportedBrowsers.ContainsKey(root.PackageName);
                        if(e.EventType == EventTypes.ViewClicked && isKnownBroswer)
                        {
                            break;
                        }
                        if(e.Source == null || !e.Source.Password)
                        {
                            CancelOverlayPrompt();
                            break;
                        }
                        if(e.PackageName == BitwardenPackage)
                        {
                            CancelOverlayPrompt();
                            break;
                        }
                        if(ScanAndAutofill(root, e))
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
                        if(e.Source == null || e.Source.Password)
                        {
                            break;
                        }
                        else if(AccessibilityHelpers.LastCredentials == null)
                        {
                            if(string.IsNullOrWhiteSpace(_lastNotificationUri))
                            {
                                CancelOverlayPrompt();
                                break;
                            }
                            var uri = AccessibilityHelpers.GetUri(root);
                            if(uri != null && uri != _lastNotificationUri)
                            {
                                CancelOverlayPrompt();
                            }
                            else if(uri != null && uri.StartsWith(Constants.AndroidAppProtocol))
                            {
                                CancelOverlayPrompt();
                            }
                            break;
                        }

                        if(e.PackageName == BitwardenPackage)
                        {
                            CancelOverlayPrompt();
                            break;
                        }

                        if(ScanAndAutofill(root, e))
                        {
                            CancelOverlayPrompt();
                        }
                        break;
                    default:
                        break;
                }

                root.Dispose();
                e.Dispose();
            }
            // Suppress exceptions so that service doesn't crash.
            catch(Exception ex)
            {
                System.Diagnostics.Debug.WriteLine(">>> Exception: " + ex.StackTrace);
            }
        }

        public override void OnInterrupt()
        {
            // Do nothing.
        }

        public bool ScanAndAutofill(AccessibilityNodeInfo root, AccessibilityEvent e)
        {
            var filled = false;
            var passwordNodes = AccessibilityHelpers.GetWindowNodes(root, e, n => n.Password, false);
            if(passwordNodes.Count > 0)
            {
                var uri = AccessibilityHelpers.GetUri(root);
                if(uri != null && !uri.Contains(BitwardenWebsite))
                {
                    if(AccessibilityHelpers.NeedToAutofill(AccessibilityHelpers.LastCredentials, uri))
                    {
                        AccessibilityHelpers.GetNodesAndFill(root, e, passwordNodes);
                        filled = true;
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
            return filled;
        }

        private void CancelOverlayPrompt()
        {
            if(_windowManager == null || _overlayView == null)
            {
                return;
            }

            _windowManager.RemoveViewImmediate(_overlayView);
            System.Diagnostics.Debug.WriteLine(">>> Accessibility Overlay View Removed");

            _overlayView = null;
            _lastNotificationUri = null;
        }

        private void OverlayPromptToAutofill(AccessibilityNodeInfo root, AccessibilityEvent e)
        {
            if(!AccessibilityHelpers.OverlayPermitted(this))
            {
                System.Diagnostics.Debug.WriteLine(">>> Overlay Permission not granted");
                Toast.MakeText(this, AppResources.AccessibilityOverlayPermissionAlert, ToastLength.Long).Show();
                return;
            }

            var uri = AccessibilityHelpers.GetUri(root);
            if(string.IsNullOrWhiteSpace(uri))
            {
                return;
            }

            WindowManagerTypes windowManagerType;
            if(Build.VERSION.SdkInt >= BuildVersionCodes.O)
            {
                windowManagerType = WindowManagerTypes.ApplicationOverlay;
            }
            else
            {
                windowManagerType = WindowManagerTypes.Phone;
            }

            var layoutParams = new WindowManagerLayoutParams(
                ViewGroup.LayoutParams.WrapContent,
                ViewGroup.LayoutParams.WrapContent,
                windowManagerType,
                WindowManagerFlags.NotFocusable | WindowManagerFlags.NotTouchModal,
                Format.Transparent);

            var anchorPosition = AccessibilityHelpers.GetOverlayAnchorPosition(root, e);

            layoutParams.Gravity = GravityFlags.Bottom | GravityFlags.Left;
            layoutParams.X = anchorPosition.X;
            layoutParams.Y = anchorPosition.Y;

            var intent = new Intent(this, typeof(AccessibilityActivity));
            intent.PutExtra("uri", uri);
            intent.SetFlags(ActivityFlags.NewTask | ActivityFlags.SingleTop | ActivityFlags.ClearTop);

            if(_windowManager == null)
            {
                _windowManager = GetSystemService(WindowService).JavaCast<IWindowManager>();
            }

            var updateView = false;
            if(_overlayView != null)
            {
                updateView = true;
            }

            _overlayView = AccessibilityHelpers.GetOverlayView(this);
            _overlayView.Click += (sender, eventArgs) =>
            {
                CancelOverlayPrompt();
                StartActivity(intent);
            };

            _lastNotificationUri = uri;

            if(updateView)
            {
                _windowManager.UpdateViewLayout(_overlayView, layoutParams);
            }
            else
            {
                _windowManager.AddView(_overlayView, layoutParams);
            }

            System.Diagnostics.Debug.WriteLine(">>> Accessibility Overlay View {0} X:{1} Y:{2}",
                updateView ? "Updated to" : "Added at", layoutParams.X, layoutParams.Y);
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
    }
}
