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
        private const string SystemUiPackage = "com.android.systemui";
        private const string ChromePackage = "com.android.chrome";
        private const string BrowserPackage = "com.android.browser";
        private const string BitwardenPackage = "com.x8bit.bitwarden";

        public override void OnAccessibilityEvent(AccessibilityEvent e)
        {
            var eventType = e.EventType;
            var packageName = e.PackageName;

            if(packageName == SystemUiPackage || packageName == BitwardenPackage)
            {
                return;
            }

            switch(eventType)
            {
                case EventTypes.WindowContentChanged:
                case EventTypes.WindowStateChanged:
                    var cancelNotification = true;
                    var root = RootInActiveWindow;
                    var isChrome = root == null ? false : root.PackageName == ChromePackage;
                    var avialablePasswordNodes = GetWindowNodes(root, e, n => AvailablePasswordField(n, isChrome));

                    if(avialablePasswordNodes.Any())
                    {
                        var uri = string.Concat(App.Constants.AndroidAppProtocol, root.PackageName);
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

                        if(NeedToAutofill(AutofillActivity.LastCredentials, uri))
                        {
                            var allEditTexts = GetWindowNodes(root, e, n => EditText(n));
                            var usernameEditText = allEditTexts.TakeWhile(n => !n.Password).LastOrDefault();
                            FillCredentials(usernameEditText, avialablePasswordNodes);
                        }
                        else
                        {
                            NotifyToAutofill(uri);
                            cancelNotification = false;
                        }

                        AutofillActivity.LastCredentials = null;
                    }

                    if(cancelNotification)
                    {
                        var notificationManager = ((NotificationManager)GetSystemService(NotificationService));
                        notificationManager.Cancel(AutoFillNotificationId);
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

        /// <summary>
        /// Check to make sure it is ok to autofill still on the current screen
        /// </summary>
        private bool NeedToAutofill(AutofillCredentials creds, string currentUriString)
        {
            if(creds == null)
            {
                return false;
            }

            Uri credsUri, lastUri, currentUri;
            if(Uri.TryCreate(creds.Uri, UriKind.Absolute, out credsUri) &&
                Uri.TryCreate(creds.LastUri, UriKind.Absolute, out lastUri) &&
                Uri.TryCreate(currentUriString, UriKind.Absolute, out currentUri) &&
                credsUri.Host == currentUri.Host && lastUri.Host == currentUri.Host)
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

        private void NotifyToAutofill(string uri)
        {
            var intent = new Intent(this, typeof(AutofillActivity));
            intent.PutExtra("uri", uri);
            intent.SetFlags(ActivityFlags.NewTask | ActivityFlags.SingleTop | ActivityFlags.ClearTop);
            var pendingIntent = PendingIntent.GetActivity(this, 0, intent, PendingIntentFlags.UpdateCurrent);

            var builder = new Notification.Builder(this);
            builder.SetSmallIcon(Resource.Drawable.notification_sm)
                   .SetContentTitle("bitwarden Autofill Service")
                   .SetContentText("Tap this notification to autofill a login from your bitwarden vault.")
                   .SetTicker("Tap this notification to autofill a login from your bitwarden vault.")
                   .SetWhen(Java.Lang.JavaSystem.CurrentTimeMillis())
                   .SetVisibility(NotificationVisibility.Secret)
                   .SetColor(global::Android.Support.V4.Content.ContextCompat.GetColor(ApplicationContext,
                        Resource.Color.primary))
                   .SetContentIntent(pendingIntent);

            var notificationManager = (NotificationManager)GetSystemService(NotificationService);
            notificationManager.Notify(AutoFillNotificationId, builder.Build());
        }

        private void FillCredentials(AccessibilityNodeInfo usernameNode, IEnumerable<AccessibilityNodeInfo> passwordNodes)
        {
            FillEditText(usernameNode, AutofillActivity.LastCredentials.Username);
            foreach(var n in passwordNodes)
            {
                FillEditText(n, AutofillActivity.LastCredentials.Password);
            }
        }

        private static void FillEditText(AccessibilityNodeInfo editTextNode, string value)
        {
            if(editTextNode == null || value == null)
            {
                return;
            }

            var bundle = new Bundle();
            bundle.PutString(AccessibilityNodeInfo.ActionArgumentSetTextCharsequence, value);
            editTextNode.PerformAction(global::Android.Views.Accessibility.Action.SetText, bundle);
        }

        private IEnumerable<AccessibilityNodeInfo> GetWindowNodes(AccessibilityNodeInfo n,
            AccessibilityEvent e, Func<AccessibilityNodeInfo, bool> p)
        {
            if(n != null)
            {
                if(n.WindowId == e.WindowId && !(n.ViewIdResourceName?.StartsWith(SystemUiPackage) ?? false) && p(n))
                {
                    yield return n;
                }

                for(int i = 0; i < n.ChildCount; i++)
                {
                    foreach(var node in GetWindowNodes(n.GetChild(i), e, p))
                    {
                        yield return node;
                    }
                }
            }
        }
    }
}
