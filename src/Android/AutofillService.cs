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
        private const string BitwardenPackage = "com.x8bit.bitwarden";
        private const string BitwardenWebsite = "bitwarden.com";

        public static bool Enabled { get; set; } = false;
        private static Dictionary<string, string[]> BrowserPackages => new Dictionary<string, string[]>
        {
            { "com.android.chrome", new string[] { "url_bar" } },
            { "com.chrome.beta", new string[] { "url_bar" } },
            { "com.android.browser", new string[] { "url" } },
            { "com.brave.browser", new string[] { "url_bar" } },
            { "com.opera.browser", new string[] { "url_field" } },
            { "com.opera.browser.beta", new string[] { "url_field" } },
            { "com.opera.mini.native", new string[] { "url_field" } },
            { "com.chrome.dev", new string[] { "url_bar" } },
            { "com.chrome.canary", new string[] { "url_bar" } },
            { "com.google.android.apps.chrome", new string[] { "url_bar" } },
            { "com.google.android.apps.chrome_dev", new string[] { "url_bar" } },
            { "org.iron.srware", new string[] { "url_bar" } },
            { "com.sec.android.app.sbrowser", new string[] { "sbrowser_url_bar" } },
            { "com.yandex.browser", new string[] { "bro_common_omnibox_host", "bro_common_omnibox_edit_text" } },
            { "org.mozilla.firefox", new string[] { "url_bar_title" } },
            { "org.mozilla.firefox_beta", new string[] { "url_bar_title" } },
            { "com.ghostery.android.ghostery",new string[] {  "search_field" } },
            { "org.adblockplus.browser", new string[] { "url_bar_title" } },
            { "com.htc.sense.browser", new string[] { "title" } },
            { "com.amazon.cloud9", new string[] { "url" } },
            { "mobi.mgeek.TunnyBrowser", new string[] { "title" } },
            { "com.nubelacorp.javelin", new string[] { "enterUrl" } },
            { "com.jerky.browser2", new string[] { "enterUrl" } },
            { "com.mx.browser", new string[] { "address_editor_with_progress" } },
            { "com.mx.browser.tablet", new string[] { "address_editor_with_progress"} },
            { "com.linkbubble.playstore", new string[] { "url_text" }}
        };

        public override void OnAccessibilityEvent(AccessibilityEvent e)
        {
            Enabled = true;
            var root = RootInActiveWindow;
            if(string.IsNullOrWhiteSpace(e.PackageName) || e.PackageName == SystemUiPackage ||
                root?.PackageName != e.PackageName)
            {
                return;
            }

            switch(e.EventType)
            {
                case EventTypes.WindowContentChanged:
                case EventTypes.WindowStateChanged:
                    var cancelNotification = true;

                    if(e.PackageName == BitwardenPackage)
                    {
                        CancelNotification();
                        break;
                    }

                    var passwordNodes = GetWindowNodes(root, e, n => n.Password);
                    if(passwordNodes.Any())
                    {
                        var uri = GetUri(root);
                        if(uri.Contains(BitwardenWebsite))
                        {
                            break;
                        }

                        if(NeedToAutofill(AutofillActivity.LastCredentials, uri))
                        {
                            var allEditTexts = GetWindowNodes(root, e, n => EditText(n));
                            var usernameEditText = allEditTexts.TakeWhile(n => !n.Password).LastOrDefault();
                            FillCredentials(usernameEditText, passwordNodes);
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
                        CancelNotification();
                    }
                    break;
                default:
                    break;
            }
        }

        public override void OnInterrupt()
        {

        }

        protected override void OnServiceConnected()
        {
            base.OnServiceConnected();
            Enabled = true;
        }

        public override void OnDestroy()
        {
            base.OnDestroy();
            Enabled = false;
        }

        private void CancelNotification()
        {
            var notificationManager = ((NotificationManager)GetSystemService(NotificationService));
            notificationManager.Cancel(AutoFillNotificationId);
        }

        private string GetUri(AccessibilityNodeInfo root)
        {
            var uri = string.Concat(App.Constants.AndroidAppProtocol, root.PackageName);
            if(BrowserPackages.ContainsKey(root.PackageName))
            {
                foreach(var addressViewId in BrowserPackages[root.PackageName])
                {
                    var addressNode = root.FindAccessibilityNodeInfosByViewId(
                        $"{root.PackageName}:id/{addressViewId}").FirstOrDefault();
                    if(addressNode == null)
                    {
                        continue;
                    }

                    uri = ExtractUri(uri, addressNode);
                    break;
                }
            }

            return uri;
        }

        private string ExtractUri(string uri, AccessibilityNodeInfo addressNode)
        {
            if(addressNode != null)
            {
                uri = addressNode.Text;
                if(!uri.Contains("://"))
                {
                    uri = string.Concat("http://", uri);
                }
                else if(Build.VERSION.SdkInt <= BuildVersionCodes.KitkatWatch)
                {
                    var parts = uri.Split(new string[] { ". " }, StringSplitOptions.None);
                    if(parts.Length > 1)
                    {
                        var urlPart = parts.FirstOrDefault(p => p.StartsWith("http"));
                        if(urlPart != null)
                        {
                            uri = urlPart.Trim();
                        }
                    }
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
                   .SetContentTitle(App.Resources.AppResources.BitwardenAutofillService)
                   .SetContentText(App.Resources.AppResources.BitwardenAutofillServiceNotificationContent)
                   .SetTicker(App.Resources.AppResources.BitwardenAutofillServiceNotificationContent)
                   .SetWhen(Java.Lang.JavaSystem.CurrentTimeMillis())
                   .SetContentIntent(pendingIntent);

            if(Build.VERSION.SdkInt > BuildVersionCodes.KitkatWatch)
            {
                builder.SetVisibility(NotificationVisibility.Secret)
                    .SetColor(global::Android.Support.V4.Content.ContextCompat.GetColor(ApplicationContext,
                        Resource.Color.primary));
            }

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
