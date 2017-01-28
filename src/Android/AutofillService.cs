using System;
using System.Collections.Generic;
using System.Linq;
using Android.AccessibilityServices;
using Android.App;
using Android.Content;
using Android.OS;
using Android.Views.Accessibility;

namespace Bit.Android
{
    [Service(Permission = "android.permission.BIND_ACCESSIBILITY_SERVICE", Label = "bitwarden")]
    [IntentFilter(new string[] { "android.accessibilityservice.AccessibilityService" })]
    [MetaData("android.accessibilityservice", Resource = "@xml/accessibilityservice")]
    public class AutofillService : AccessibilityService
    {
        private const int AutoFillNotificationId = 34573;
        private const string AndroidAppProtocol = "androidapp://";
        private const string SystemUiPackage = "com.android.systemui";
        private const string ChromePackage = "com.android.chrome";
        private const string BrowserPackage = "com.android.browser";

        public override void OnAccessibilityEvent(AccessibilityEvent e)
        {
            var eventType = e.EventType;
            var packageName = e.PackageName;

            if(packageName == SystemUiPackage)
            {
                return;
            }

            switch(eventType)
            {
                case EventTypes.WindowContentChanged:
                case EventTypes.WindowStateChanged:
                    var root = RootInActiveWindow;
                    var isChrome = root == null ? false : root.PackageName == ChromePackage;
                    var cancelNotification = true;
                    var avialablePasswordNodes = GetNodeOrChildren(root, n => AvailablePasswordField(n, isChrome));

                    if(avialablePasswordNodes.Any() && AnyNodeOrChildren(root, n => n.WindowId == e.WindowId &&
                        !(n.ViewIdResourceName != null && n.ViewIdResourceName.StartsWith(SystemUiPackage))))
                    {
                        var uri = string.Concat(AndroidAppProtocol, root.PackageName);
                        if(isChrome)
                        {
                            var addressNode = root.FindAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar")
                                .FirstOrDefault();
                            uri = ExtractUriFromAddressField(uri, addressNode);

                        }
                        else if(root.PackageName == BrowserPackage)
                        {
                            var addressNode = root.FindAccessibilityNodeInfosByViewId("com.android.browser:id/url")
                                .FirstOrDefault();
                            uri = ExtractUriFromAddressField(uri, addressNode);
                        }

                        var allEditTexts = GetNodeOrChildren(root, n => EditText(n));
                        var usernameEditText = allEditTexts.TakeWhile(n => !n.Password).LastOrDefault();

                        if(AutofillActivity.LastCredentials != null && SameUri(AutofillActivity.LastCredentials.LastUri, uri))
                        {
                            FillCredentials(usernameEditText, avialablePasswordNodes);
                        }
                        else
                        {
                            AskFillPassword(uri, usernameEditText, avialablePasswordNodes);
                            cancelNotification = false;
                        }
                    }

                    if(cancelNotification)
                    {
                        ((NotificationManager)GetSystemService(NotificationService)).Cancel(AutoFillNotificationId);
                    }
                    break;
                default:
                    break;
            }
        }

        public override void OnInterrupt()
        {

        }

        private string ExtractUriFromAddressField(string uri, AccessibilityNodeInfo addressNode)
        {
            if(addressNode != null)
            {
                uri = addressNode.Text;
                if(!uri.Contains("://"))
                {
                    uri = string.Concat("http://", uri);
                }
            }

            return uri;
        }

        private bool SameUri(string uriString1, string uriString2)
        {
            Uri uri1, uri2;
            if(Uri.TryCreate(uriString1, UriKind.RelativeOrAbsolute, out uri1) &&
                Uri.TryCreate(uriString2, UriKind.RelativeOrAbsolute, out uri2) && uri1.Host == uri2.Host)
            {
                return true;
            }

            return false;
        }

        private static bool AvailablePasswordField(AccessibilityNodeInfo n, bool isChrome)
        {
            // chrome sends password field values in many conditions when the field is still actually empty
            // ex. placeholders, nearby label, etc
            return n.Password && (isChrome || string.IsNullOrWhiteSpace(n.Text));
        }

        private static bool EditText(AccessibilityNodeInfo n)
        {
            return n.ClassName != null && n.ClassName.Contains("EditText");
        }

        private void AskFillPassword(string uri, AccessibilityNodeInfo usernameNode,
            IEnumerable<AccessibilityNodeInfo> passwordNodes)
        {
            var intent = new Intent(this, typeof(AutofillActivity));
            intent.PutExtra("uri", uri);
            intent.SetFlags(ActivityFlags.NewTask | ActivityFlags.SingleTop | ActivityFlags.ClearTop);
            var pendingIntent = PendingIntent.GetActivity(this, 0, intent, PendingIntentFlags.UpdateCurrent);

            var targetName = uri;
            if(uri.StartsWith(AndroidAppProtocol))
            {
                var packageName = uri.Substring(AndroidAppProtocol.Length);
                try
                {
                    var appInfo = PackageManager.GetApplicationInfo(packageName, 0);
                    targetName = appInfo != null ? PackageManager.GetApplicationLabel(appInfo) : packageName;
                }
                catch
                {
                    targetName = packageName;
                }
            }
            else
            {
                //targetName = KeePassLib.Utility.UrlUtil.GetHost(uri);
            }


            var builder = new Notification.Builder(this);
            //TODO icon
            //TODO plugin icon
            builder.SetSmallIcon(Resource.Drawable.icon)
                   .SetContentText("Tap this notification to autofill a login from your bitwarden vault.")
                   .SetContentTitle("bitwarden Autofill Service")
                   .SetWhen(Java.Lang.JavaSystem.CurrentTimeMillis())
                   .SetTicker("Tap this notification to autofill a login from your bitwarden vault.")
                   .SetVisibility(NotificationVisibility.Secret)
                   .SetContentIntent(pendingIntent);
            var notificationManager = (NotificationManager)GetSystemService(NotificationService);
            notificationManager.Notify(AutoFillNotificationId, builder.Build());
        }

        private void FillCredentials(AccessibilityNodeInfo usernameNode, IEnumerable<AccessibilityNodeInfo> passwordNodes)
        {
            FillEditText(usernameNode, AutofillActivity.LastCredentials.Username);
            foreach(var pNode in passwordNodes)
            {
                FillEditText(pNode, AutofillActivity.LastCredentials.Password);
            }

            AutofillActivity.LastCredentials = null;
        }

        private static void FillEditText(AccessibilityNodeInfo editTextNode, string value)
        {
            var bundle = new Bundle();
            bundle.PutString(AccessibilityNodeInfo.ActionArgumentSetTextCharsequence, value);
            editTextNode.PerformAction(global::Android.Views.Accessibility.Action.SetText, bundle);
        }

        private bool AnyNodeOrChildren(AccessibilityNodeInfo n, Func<AccessibilityNodeInfo, bool> p)
        {
            return GetNodeOrChildren(n, p).Any();
        }

        private IEnumerable<AccessibilityNodeInfo> GetNodeOrChildren(AccessibilityNodeInfo n,
            Func<AccessibilityNodeInfo, bool> p)
        {
            if(n != null)
            {
                if(p(n))
                {
                    yield return n;
                }

                for(int i = 0; i < n.ChildCount; i++)
                {
                    foreach(var node in GetNodeOrChildren(n.GetChild(i), p))
                    {
                        yield return node;
                    }
                }
            }
        }
    }
}
