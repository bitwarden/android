using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Android.AccessibilityServices;
using Android.App;
using Android.Content;
using Android.Graphics;
using Android.OS;
using Android.Runtime;
using Android.Views;
using Android.Views.Accessibility;
using Android.Widget;

namespace Bit.Android
{
    //[Service(Permission = "android.permission.BIND_ACCESSIBILITY_SERVICE", Label = "bitwarden")]
    //[IntentFilter(new string[] { "android.accessibilityservice.AccessibilityService" })]
    //[MetaData("android.accessibilityservice", Resource = "@xml/accessibilityservice")]
    public class AutofillService : AccessibilityService
    {
        private const int autoFillNotificationId = 0;
        private const string androidAppPrefix = "androidapp://";

        public override void OnAccessibilityEvent(AccessibilityEvent e)
        {
            var eventType = e.EventType;
            var package = e.PackageName;
            switch(eventType)
            {
                case EventTypes.ViewTextSelectionChanged:
                    //if(e.Source.Password && string.IsNullOrWhiteSpace(e.Source.Text))
                    //{
                    //    var bundle = new Bundle();
                    //    bundle.PutCharSequence(AccessibilityNodeInfo.ActionArgumentSetTextCharsequence, "mypassword");
                    //    e.Source.PerformAction(global::Android.Views.Accessibility.Action.SetText, bundle);
                    //}
                    break;
                case EventTypes.WindowContentChanged:
                case EventTypes.WindowStateChanged:
                    if(e.PackageName == "com.android.systemui")
                    {
                        break;
                    }
                    var root = RootInActiveWindow;
                    if((ExistsNodeOrChildren(root, n => n.WindowId == e.WindowId) && !ExistsNodeOrChildren(root, n => (n.ViewIdResourceName != null) && (n.ViewIdResourceName.StartsWith("com.android.systemui")))))
                    {
                        bool cancelNotification = true;

                        var allEditTexts = GetNodeOrChildren(root, n => { return IsEditText(n); });

                        var usernameEdit = allEditTexts.TakeWhile(edit => (edit.Password == false)).LastOrDefault();

                        string searchString = androidAppPrefix + root.PackageName;

                        string url = androidAppPrefix + root.PackageName;

                        if(root.PackageName == "com.android.chrome")
                        {
                            var addressField = root.FindAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar").FirstOrDefault();
                            UrlFromAddressField(ref url, addressField);

                        }
                        else if(root.PackageName == "com.android.browser")
                        {
                            var addressField = root.FindAccessibilityNodeInfosByViewId("com.android.browser:id/url").FirstOrDefault();
                            UrlFromAddressField(ref url, addressField);
                        }

                        var emptyPasswordFields = GetNodeOrChildren(root, n => { return IsPasswordField(n); }).ToList();
                        if(emptyPasswordFields.Any())
                        {
                            if((AutofillActivity.LastReceivedCredentials != null) && IsSame(AutofillActivity.LastReceivedCredentials.Url, url))
                            {
                                //Android.Util.Log.Debug("KP2AAS", "Filling credentials for " + url);

                                FillPassword(url, usernameEdit, emptyPasswordFields);
                            }
                            else
                            {
                                //Android.Util.Log.Debug("KP2AAS", "Notif for " + url);
                                if(AutofillActivity.LastReceivedCredentials != null)
                                {
                                    //Android.Util.Log.Debug("KP2AAS", LookupCredentialsActivity.LastReceivedCredentials.Url);
                                    //Android.Util.Log.Debug("KP2AAS", url);
                                }

                                AskFillPassword(url, usernameEdit, emptyPasswordFields);
                                cancelNotification = false;
                            }

                        }
                        if(cancelNotification)
                        {
                            ((NotificationManager)GetSystemService(NotificationService)).Cancel(autoFillNotificationId);
                            //Android.Util.Log.Debug("KP2AAS", "Cancel notif");
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        public override void OnInterrupt()
        {

        }

        private static void UrlFromAddressField(ref string url, AccessibilityNodeInfo addressField)
        {
            if(addressField != null)
            {
                url = addressField.Text;
                if(!url.Contains("://"))
                    url = "http://" + url;
            }

        }

        private bool IsSame(string url1, string url2)
        {
            if(url1.StartsWith("androidapp://"))
                return url1 == url2;
            //return KeePassLib.Utility.UrlUtil.GetHost(url1) == KeePassLib.Utility.UrlUtil.GetHost(url2);
            return false;
        }

        private static bool IsPasswordField(AccessibilityNodeInfo n)
        {
            //if (n.Password) Android.Util.Log.Debug(_logTag, "pwdx with " + (n.Text == null ? "null" : n.Text));
            var res = n.Password && string.IsNullOrEmpty(n.Text);
            // if (n.Password) Android.Util.Log.Debug(_logTag, "pwd with " + n.Text + res);
            return res;
        }

        private static bool IsEditText(AccessibilityNodeInfo n)
        {
            //it seems like n.Editable is not a good check as this is false for some fields which are actually editable, at least in tests with Chrome.
            return (n.ClassName != null) && (n.ClassName.Contains("EditText"));
        }

        private void AskFillPassword(string url, AccessibilityNodeInfo usernameEdit, IEnumerable<AccessibilityNodeInfo> passwordFields)
        {
            var runSearchIntent = new Intent(this, typeof(AutofillActivity));
            runSearchIntent.PutExtra("url", url);
            runSearchIntent.SetFlags(ActivityFlags.NewTask | ActivityFlags.SingleTop | ActivityFlags.ClearTop);
            var pending = PendingIntent.GetActivity(this, 0, runSearchIntent, PendingIntentFlags.UpdateCurrent);

            var targetName = url;

            if(url.StartsWith(androidAppPrefix))
            {
                var packageName = url.Substring(androidAppPrefix.Length);
                try
                {
                    var appInfo = PackageManager.GetApplicationInfo(packageName, 0);
                    targetName = (string)(appInfo != null ? PackageManager.GetApplicationLabel(appInfo) : packageName);
                }
                catch(Exception e)
                {
                    //Android.Util.Log.Debug(_logTag, e.ToString());
                    targetName = packageName;
                }
            }
            else
            {
                //targetName = KeePassLib.Utility.UrlUtil.GetHost(url);
            }


            var builder = new Notification.Builder(this);
            //TODO icon
            //TODO plugin icon
            builder.SetSmallIcon(Resource.Drawable.icon)
                   .SetContentText("Content Text")
                   .SetContentTitle("Content Title")
                   .SetWhen(Java.Lang.JavaSystem.CurrentTimeMillis())
                   .SetTicker("Ticker Text")
                   .SetVisibility(NotificationVisibility.Secret)
                   .SetContentIntent(pending);
            var notificationManager = (NotificationManager)GetSystemService(NotificationService);
            notificationManager.Notify(autoFillNotificationId, builder.Build());

        }

        private void FillPassword(string url, AccessibilityNodeInfo usernameEdit, IEnumerable<AccessibilityNodeInfo> passwordFields)
        {

            FillDataInTextField(usernameEdit, AutofillActivity.LastReceivedCredentials.User);
            foreach(var pwd in passwordFields)
                FillDataInTextField(pwd, AutofillActivity.LastReceivedCredentials.Password);

            AutofillActivity.LastReceivedCredentials = null;
        }

        private static void FillDataInTextField(AccessibilityNodeInfo edit, string newValue)
        {
            Bundle b = new Bundle();
            b.PutString(AccessibilityNodeInfo.ActionArgumentSetTextCharsequence, newValue);
            edit.PerformAction(global::Android.Views.Accessibility.Action.SetText, b);
        }

        private bool ExistsNodeOrChildren(AccessibilityNodeInfo n, Func<AccessibilityNodeInfo, bool> p)
        {
            return GetNodeOrChildren(n, p).Any();
        }

        private IEnumerable<AccessibilityNodeInfo> GetNodeOrChildren(AccessibilityNodeInfo n, Func<AccessibilityNodeInfo, bool> p)
        {
            if(n != null)
            {

                if(p(n))
                {
                    yield return n;
                }

                for(int i = 0; i < n.ChildCount; i++)
                {
                    foreach(var x in GetNodeOrChildren(n.GetChild(i), p))
                    {
                        yield return x;
                    }
                }
            }
        }
    }
}