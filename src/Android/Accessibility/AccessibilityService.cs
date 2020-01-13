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
        private int _anchorViewHash = 0;
        private int _lastAnchorX, _lastAnchorY = 0;

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
                    case EventTypes.ViewScrolled:
                        var isKnownBroswer = AccessibilityHelpers.SupportedBrowsers.ContainsKey(root.PackageName);
                        if(e.EventType == EventTypes.ViewClicked && isKnownBroswer)
                        {
                            break;
                        }
                        if(e.Source == null || e.PackageName == BitwardenPackage)
                        {
                            CancelOverlayPrompt();
                            break;
                        }
                        if(e.EventType == EventTypes.ViewScrolled)
                        {
                            AdjustOverlayForScroll(root, e);
                            break;
                        }
                        else
                        {
                            var isUsernameEditText1 = AccessibilityHelpers.IsUsernameEditText(root, e);
                            var isPasswordEditText1 = e.Source?.Password ?? false;
                            if(!isUsernameEditText1 && !isPasswordEditText1)
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
                        }
                        break;
                    case EventTypes.WindowContentChanged:
                    case EventTypes.WindowStateChanged:
                        var isUsernameEditText2 = AccessibilityHelpers.IsUsernameEditText(root, e);
                        var isPasswordEditText2 = e.Source?.Password ?? false;
                        if(e.Source == null || isUsernameEditText2 || isPasswordEditText2)
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
            _anchorViewHash = 0;
            _lastNotificationUri = null;
            _lastAnchorX = 0;
            _lastAnchorY = 0;
        }

        private void OverlayPromptToAutofill(AccessibilityNodeInfo root, AccessibilityEvent e)
        {
            if(!AccessibilityHelpers.OverlayPermitted())
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

            var layoutParams = AccessibilityHelpers.GetOverlayLayoutParams();
            var anchorPosition = AccessibilityHelpers.GetOverlayAnchorPosition(root, e.Source);
            layoutParams.X = anchorPosition.X;
            layoutParams.Y = anchorPosition.Y;

            if(_windowManager == null)
            {
                _windowManager = GetSystemService(WindowService).JavaCast<IWindowManager>();
            }

            if(_overlayView == null)
            {
                var intent = new Intent(this, typeof(AccessibilityActivity));
                intent.PutExtra("uri", uri);
                intent.SetFlags(ActivityFlags.NewTask | ActivityFlags.SingleTop | ActivityFlags.ClearTop);

                _overlayView = AccessibilityHelpers.GetOverlayView(this);
                _overlayView.Click += (sender, eventArgs) =>
                {
                    CancelOverlayPrompt();
                    StartActivity(intent);
                };

                _lastNotificationUri = uri;

                _windowManager.AddView(_overlayView, layoutParams);

                System.Diagnostics.Debug.WriteLine(">>> Accessibility Overlay View Added at X:{0} Y:{1}",
                    layoutParams.X, layoutParams.Y);
            }
            else
            {
                _windowManager.UpdateViewLayout(_overlayView, layoutParams);

                System.Diagnostics.Debug.WriteLine(">>> Accessibility Overlay View Updated to X:{0} Y:{1}",
                    layoutParams.X, layoutParams.Y);
            }

            _anchorViewHash = e.Source.GetHashCode();
            _lastAnchorX = anchorPosition.X;
            _lastAnchorY = anchorPosition.Y;
        }

        private void AdjustOverlayForScroll(AccessibilityNodeInfo root, AccessibilityEvent e)
        {
            if(_overlayView == null || _anchorViewHash <= 0)
            {
                return;
            }

            var anchorPosition = AccessibilityHelpers.GetOverlayAnchorPosition(_anchorViewHash, root, e);
            if(anchorPosition == null)
            {
                return;
            }

            if(anchorPosition.X == _lastAnchorX && anchorPosition.Y == _lastAnchorY)
            {
                return;
            }

            var layoutParams = AccessibilityHelpers.GetOverlayLayoutParams();
            layoutParams.X = anchorPosition.X;
            layoutParams.Y = anchorPosition.Y;

            _windowManager.UpdateViewLayout(_overlayView, layoutParams);

            _lastAnchorX = anchorPosition.X;
            _lastAnchorY = anchorPosition.Y;

            System.Diagnostics.Debug.WriteLine(">>> Accessibility Overlay View Updated to X:{0} Y:{1}",
                    layoutParams.X, layoutParams.Y);
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
