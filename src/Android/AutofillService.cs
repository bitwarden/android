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

        private static Dictionary<string, Browser> SupportedBrowsers => new List<Browser>
        {
            new Browser("com.android.chrome", "url_bar"),
            new Browser("com.chrome.beta", "url_bar"),
            new Browser("com.android.browser", "url"),
            new Browser("com.brave.browser", "url_bar"),
            new Browser("com.opera.browser", "url_field"),
            new Browser("com.opera.browser.beta", "url_field"),
            new Browser("com.opera.mini.native", "url_field"),
            new Browser("com.chrome.dev", "url_bar"),
            new Browser("com.chrome.canary", "url_bar"),
            new Browser("com.google.android.apps.chrome", "url_bar"),
            new Browser("com.google.android.apps.chrome_dev", "url_bar"),
            new Browser("org.iron.srware", "url_bar"),
            new Browser("com.sec.android.app.sbrowser", "sbrowser_url_bar"),
            new Browser("com.sec.android.app.sbrowser.beta", "location_bar_edit_text"),
            new Browser("com.yandex.browser", "bro_omnibar_address_title_text",
                (s) => s.Split(' ').FirstOrDefault()),
            new Browser("org.mozilla.firefox", "url_bar_title"),
            new Browser("org.mozilla.firefox_beta", "url_bar_title"),
            new Browser("com.ghostery.android.ghostery", "search_field"),
            new Browser("org.adblockplus.browser", "url_bar_title"),
            new Browser("com.htc.sense.browser", "title"),
            new Browser("com.amazon.cloud9", "url"),
            new Browser("mobi.mgeek.TunnyBrowser", "title"),
            new Browser("com.nubelacorp.javelin", "enterUrl"),
            new Browser("com.jerky.browser2", "enterUrl"),
            new Browser("com.mx.browser", "address_editor_with_progress"),
            new Browser("com.mx.browser.tablet", "address_editor_with_progress"),
            new Browser("com.linkbubble.playstore", "url_text"),
            new Browser("com.ksmobile.cb", "address_bar_edit_text")
        }.ToDictionary(n => n.PackageName);

        public override void OnAccessibilityEvent(AccessibilityEvent e)
        {
            try
            {
                var root = RootInActiveWindow;
                if(e == null || root == null || string.IsNullOrWhiteSpace(e.PackageName) ||
                    e.PackageName == SystemUiPackage || root.PackageName != e.PackageName)
                {
                    return;
                }

                /*
                var testNodes = GetWindowNodes(root, e, n => n.ViewIdResourceName != null && n.Text != null, false);
                var testNodesData = testNodes.Select(n => new { id = n.ViewIdResourceName, text = n.Text });
                testNodes.Dispose();
                */

                var notificationManager = (NotificationManager)GetSystemService(NotificationService);
                switch(e.EventType)
                {
                    case EventTypes.WindowContentChanged:
                    case EventTypes.WindowStateChanged:
                        var cancelNotification = true;

                        if(e.PackageName == BitwardenPackage)
                        {
                            notificationManager?.Cancel(AutoFillNotificationId);
                            break;
                        }

                        var passwordNodes = GetWindowNodes(root, e, n => n.Password, false);
                        if(passwordNodes.Count > 0)
                        {
                            var uri = GetUri(root);
                            if(uri != null && !uri.Contains(BitwardenWebsite))
                            {
                                if(NeedToAutofill(AutofillActivity.LastCredentials, uri))
                                {
                                    var allEditTexts = GetWindowNodes(root, e, n => EditText(n), false);
                                    var usernameEditText = allEditTexts.TakeWhile(n => !n.Password).LastOrDefault();
                                    FillCredentials(usernameEditText, passwordNodes);

                                    allEditTexts.Dispose();
                                    usernameEditText.Dispose();
                                }
                                else
                                {
                                    NotifyToAutofill(uri, notificationManager);
                                    cancelNotification = false;
                                }
                            }

                            AutofillActivity.LastCredentials = null;
                        }

                        passwordNodes.Dispose();

                        if(cancelNotification)
                        {
                            notificationManager?.Cancel(AutoFillNotificationId);
                        }
                        break;
                    default:
                        break;
                }

                notificationManager?.Dispose();
                root.Dispose();
                e.Dispose();
            }
            // Suppress exceptions so that service doesn't crash
            catch { }
        }

        public override void OnInterrupt()
        {

        }

        private string GetUri(AccessibilityNodeInfo root)
        {
            var uri = string.Concat(App.Constants.AndroidAppProtocol, root.PackageName);
            if(SupportedBrowsers.ContainsKey(root.PackageName))
            {
                var addressNode = root.FindAccessibilityNodeInfosByViewId(
                    $"{root.PackageName}:id/{SupportedBrowsers[root.PackageName].UriViewId}").FirstOrDefault();
                if(addressNode != null)
                {
                    uri = ExtractUri(uri, addressNode, SupportedBrowsers[root.PackageName]);
                    addressNode.Dispose();
                }
            }

            return uri;
        }

        private string ExtractUri(string uri, AccessibilityNodeInfo addressNode, Browser browser)
        {
            if(addressNode?.Text != null)
            {
                uri = browser.GetUriFunction(addressNode.Text).Trim();
                if(uri != null && uri.Contains("."))
                {
                    if(!uri.Contains("://") && !uri.Contains(" "))
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

            Uri lastUri, currentUri;
            if(Uri.TryCreate(creds.LastUri, UriKind.Absolute, out lastUri) &&
                Uri.TryCreate(currentUriString, UriKind.Absolute, out currentUri) &&
                lastUri.Host == currentUri.Host)
            {
                return true;
            }

            return false;
        }

        private static bool EditText(AccessibilityNodeInfo n)
        {
            return n?.ClassName?.Contains("EditText") ?? false;
        }

        private void NotifyToAutofill(string uri, NotificationManager notificationManager)
        {
            if(notificationManager == null || string.IsNullOrWhiteSpace(uri))
            {
                return;
            }

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

            notificationManager.Notify(AutoFillNotificationId, builder.Build());

            builder.Dispose();
        }

        private void FillCredentials(AccessibilityNodeInfo usernameNode, IEnumerable<AccessibilityNodeInfo> passwordNodes)
        {
            FillEditText(usernameNode, AutofillActivity.LastCredentials?.Username);
            foreach(var n in passwordNodes)
            {
                FillEditText(n, AutofillActivity.LastCredentials?.Password);
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

        private NodeList GetWindowNodes(AccessibilityNodeInfo n, AccessibilityEvent e,
            Func<AccessibilityNodeInfo, bool> condition, bool disposeIfUnused, NodeList nodes = null)
        {
            if(nodes == null)
            {
                nodes = new NodeList();
            }

            if(n != null)
            {
                var dispose = disposeIfUnused;
                if(n.WindowId == e.WindowId && !(n.ViewIdResourceName?.StartsWith(SystemUiPackage) ?? false) && condition(n))
                {
                    dispose = false;
                    nodes.Add(n);
                }

                for(var i = 0; i < n.ChildCount; i++)
                {
                    GetWindowNodes(n.GetChild(i), e, condition, true, nodes);
                }

                if(dispose)
                {
                    n.Dispose();
                }
            }

            return nodes;
        }

        public class Browser
        {
            public Browser(string packageName, string uriViewId)
            {
                PackageName = packageName;
                UriViewId = uriViewId;
            }

            public Browser(string packageName, string uriViewId, Func<string, string> getUriFunction)
                : this(packageName, uriViewId)
            {
                GetUriFunction = getUriFunction;
            }

            public string PackageName { get; set; }
            public string UriViewId { get; set; }
            public Func<string, string> GetUriFunction { get; set; } = (s) => s;
        }

        public class NodeList : List<AccessibilityNodeInfo>, IDisposable
        {
            public void Dispose()
            {
                foreach(var item in this)
                {
                    item.Dispose();
                }
            }
        }
    }
}
