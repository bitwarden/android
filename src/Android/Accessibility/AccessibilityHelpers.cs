using System;
using System.Collections.Generic;
using System.Linq;
using Android.Content;
using Android.Content.Res;
using Android.Graphics;
using Android.OS;
using Android.Provider;
using Android.Views;
using Android.Views.Accessibility;
using Android.Widget;
using Bit.App.Resources;
using Bit.Core;
using Plugin.CurrentActivity;

namespace Bit.Droid.Accessibility
{
    public static class AccessibilityHelpers
    {
        public static Credentials LastCredentials = null;
        public static string SystemUiPackage = "com.android.systemui";
        public static string BitwardenTag = "bw_access";

        public static Dictionary<string, Browser> SupportedBrowsers => new List<Browser>
        {
            new Browser("com.android.chrome", "url_bar"),
            new Browser("com.chrome.beta", "url_bar"),
            new Browser("org.chromium.chrome", "url_bar"),
            new Browser("com.android.browser", "url"),
            new Browser("com.brave.browser", "url_bar"),
            new Browser("com.opera.browser", "url_field"),
            new Browser("com.opera.browser.beta", "url_field"),
            new Browser("com.opera.mini.native", "url_field"),
            new Browser("com.opera.touch", "addressbarEdit"),
            new Browser("com.chrome.dev", "url_bar"),
            new Browser("com.chrome.canary", "url_bar"),
            new Browser("com.google.android.apps.chrome", "url_bar"),
            new Browser("com.google.android.apps.chrome_dev", "url_bar"),
            new Browser("org.codeaurora.swe.browser", "url_bar"),
            new Browser("org.iron.srware", "url_bar"),
            new Browser("com.sec.android.app.sbrowser", "location_bar_edit_text"),
            new Browser("com.sec.android.app.sbrowser.beta", "location_bar_edit_text"),
            new Browser("com.yandex.browser", "bro_omnibar_address_title_text",
                (s) => s.Split(new char[]{' ', ' '}).FirstOrDefault()), // 0 = Regular Space, 1 = No-break space (00A0)
            new Browser("org.mozilla.firefox", "url_bar_title"),
            new Browser("org.mozilla.firefox_beta", "url_bar_title"),
            new Browser("org.mozilla.fennec_aurora", "url_bar_title"),
            new Browser("org.mozilla.fennec_fdroid", "url_bar_title"),
            new Browser("org.mozilla.focus", "display_url"),
            new Browser("org.mozilla.klar", "display_url"),
            new Browser("org.mozilla.fenix", "mozac_browser_toolbar_url_view"),
            new Browser("org.mozilla.fenix.nightly", "mozac_browser_toolbar_url_view"),
            new Browser("org.mozilla.reference.browser", "mozac_browser_toolbar_url_view"),
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
            new Browser("com.ksmobile.cb", "address_bar_edit_text"),
            new Browser("acr.browser.lightning", "search"),
            new Browser("acr.browser.barebones", "search"),
            new Browser("com.microsoft.emmx", "url_bar"),
            new Browser("com.duckduckgo.mobile.android", "omnibarTextInput"),
            new Browser("mark.via.gp", "aw"),
            new Browser("org.bromite.bromite", "url_bar"),
            new Browser("com.kiwibrowser.browser", "url_bar"),
            new Browser("com.ecosia.android", "url_bar"),
            new Browser("com.qwant.liberty", "url_bar_title"),
            new Browser("jp.co.fenrir.android.sleipnir", "url_text"),
            new Browser("jp.co.fenrir.android.sleipnir_black", "url_text"),
            new Browser("jp.co.fenrir.android.sleipnir_test", "url_text"),
            new Browser("com.vivaldi.browser", "url_bar"),
            new Browser("com.feedback.browser.wjbrowser", "addressbar_url"),
        }.ToDictionary(n => n.PackageName);

        // Known packages to skip
        public static HashSet<string> FilteredPackageNames => new HashSet<string>
        {
            SystemUiPackage,
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.nexuslauncher",
            "com.google.android.launcher",
            "com.computer.desktop.ui.launcher",
            "com.launcher.notelauncher",
            "com.anddoes.launcher",
            "com.actionlauncher.playstore",
            "ch.deletescape.lawnchair.plah",
            "com.microsoft.launcher",
            "com.teslacoilsw.launcher",
            "com.teslacoilsw.launcher.prime",
            "is.shortcut",
            "me.craftsapp.nlauncher",
            "com.ss.squarehome2",
            "com.treydev.pns"
        };

        public static void PrintTestData(AccessibilityNodeInfo root, AccessibilityEvent e)
        {
            var testNodes = GetWindowNodes(root, e, n => n.ViewIdResourceName != null && n.Text != null, false);
            var testNodesData = testNodes.Select(n => new { id = n.ViewIdResourceName, text = n.Text });
            foreach(var node in testNodesData)
            {
                System.Diagnostics.Debug.WriteLine("Node: {0} = {1}", node.id, node.text);
            }
        }

        public static string GetUri(AccessibilityNodeInfo root)
        {
            var uri = string.Concat(Constants.AndroidAppProtocol, root.PackageName);
            if(SupportedBrowsers.ContainsKey(root.PackageName))
            {
                var browser = SupportedBrowsers[root.PackageName];
                var addressNode = root.FindAccessibilityNodeInfosByViewId(
                    $"{root.PackageName}:id/{browser.UriViewId}").FirstOrDefault();
                if(addressNode != null)
                {
                    uri = ExtractUri(uri, addressNode, browser);
                    addressNode.Dispose();
                }
                else
                {
                    // Return null to prevent overwriting notification pendingIntent uri with browser packageName
                    // (we login to pages, not browsers)
                    return null;
                }
            }
            return uri;
        }

        public static string ExtractUri(string uri, AccessibilityNodeInfo addressNode, Browser browser)
        {
            if(addressNode?.Text == null)
            {
                return uri;
            }
            if(addressNode.Text == null)
            {
                return uri;
            }
            uri = browser.GetUriFunction(addressNode.Text)?.Trim();
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
            return uri;
        }

        /// <summary>
        /// Check to make sure it is ok to autofill still on the current screen
        /// </summary>
        public static bool NeedToAutofill(Credentials credentials, string currentUriString)
        {
            if(credentials == null)
            {
                return false;
            }
            if(Uri.TryCreate(credentials.LastUri, UriKind.Absolute, out Uri lastUri) &&
                Uri.TryCreate(currentUriString, UriKind.Absolute, out Uri currentUri))
            {
                return lastUri.Host == currentUri.Host;
            }
            return false;
        }

        public static bool EditText(AccessibilityNodeInfo n)
        {
            return n?.ClassName?.Contains("EditText") ?? false;
        }

        public static void FillCredentials(AccessibilityNodeInfo usernameNode,
            IEnumerable<AccessibilityNodeInfo> passwordNodes)
        {
            FillEditText(usernameNode, LastCredentials?.Username);
            foreach(var n in passwordNodes)
            {
                FillEditText(n, LastCredentials?.Password);
            }
        }

        public static void FillEditText(AccessibilityNodeInfo editTextNode, string value)
        {
            if(editTextNode == null || value == null)
            {
                return;
            }
            var bundle = new Bundle();
            bundle.PutString(AccessibilityNodeInfo.ActionArgumentSetTextCharsequence, value);
            editTextNode.PerformAction(Android.Views.Accessibility.Action.SetText, bundle);
        }

        public static NodeList GetWindowNodes(AccessibilityNodeInfo n, AccessibilityEvent e,
            Func<AccessibilityNodeInfo, bool> condition, bool disposeIfUnused, NodeList nodes = null,
            int recursionDepth = 0)
        {
            if(nodes == null)
            {
                nodes = new NodeList();
            }
            var dispose = disposeIfUnused;
            if(n != null && recursionDepth < 50)
            {
                var add = n.WindowId == e.WindowId &&
                    !(n.ViewIdResourceName?.StartsWith(SystemUiPackage) ?? false) &&
                    condition(n);
                if(add)
                {
                    dispose = false;
                    nodes.Add(n);
                }

                for(var i = 0; i < n.ChildCount; i++)
                {
                    var childNode = n.GetChild(i);
                    if(i > 100)
                    {
                        Android.Util.Log.Info(BitwardenTag, "Too many child iterations.");
                        break;
                    }
                    else if(childNode.GetHashCode() == n.GetHashCode())
                    {
                        Android.Util.Log.Info(BitwardenTag, "Child node is the same as parent for some reason.");
                    }
                    else
                    {
                        GetWindowNodes(childNode, e, condition, true, nodes, recursionDepth++);
                    }
                }
            }
            if(dispose)
            {
                n?.Dispose();
            }
            return nodes;
        }

        public static void GetNodesAndFill(AccessibilityNodeInfo root, AccessibilityEvent e,
            IEnumerable<AccessibilityNodeInfo> passwordNodes)
        {
            var allEditTexts = GetWindowNodes(root, e, n => EditText(n), false);
            var usernameEditText = GetUsernameEditTextIfPasswordExists(allEditTexts);
            FillCredentials(usernameEditText, passwordNodes);
            allEditTexts.Dispose();
            usernameEditText = null;
        }

        public static AccessibilityNodeInfo GetUsernameEditTextIfPasswordExists(
            IEnumerable<AccessibilityNodeInfo> allEditTexts)
        {
            AccessibilityNodeInfo previousEditText = null;
            foreach(var editText in allEditTexts)
            {
                if(editText.Password)
                {
                    return previousEditText;
                }
                previousEditText = editText;
            }
            return null;
        }

        public static bool IsUsernameEditText(AccessibilityNodeInfo root, AccessibilityEvent e)
        {
            var allEditTexts = GetWindowNodes(root, e, n => EditText(n), false);
            var usernameEditText = GetUsernameEditTextIfPasswordExists(allEditTexts);
            if(usernameEditText != null)
            { 
                var isUsernameEditText = IsSameNode(usernameEditText, e.Source);
                allEditTexts.Dispose();
                usernameEditText = null;
                return isUsernameEditText;
            }
            return false;
        }

        public static bool IsSameNode(AccessibilityNodeInfo info1, AccessibilityNodeInfo info2)
        {
            if(info1 != null && info2 != null) 
            {
                return info1.Equals(info2) || info1.GetHashCode() == info2.GetHashCode();
            }
            return false;
        }

        public static bool OverlayPermitted()
        {
            if(Build.VERSION.SdkInt >= BuildVersionCodes.M)
            {
                return Settings.CanDrawOverlays(Android.App.Application.Context);
            }
            else
            {
                // TODO do older android versions require a check?
                return true;
            }
        }

        public static LinearLayout GetOverlayView(Context context)
        {
            var inflater = (LayoutInflater)context.GetSystemService(Context.LayoutInflaterService);
            var view = (LinearLayout)inflater.Inflate(Resource.Layout.autofill_listitem, null);
            var text1 = (TextView)view.FindViewById(Resource.Id.text1);
            var text2 = (TextView)view.FindViewById(Resource.Id.text2);
            var icon = (ImageView)view.FindViewById(Resource.Id.icon);
            text1.Text = AppResources.AutofillWithBitwarden;
            text2.Text = AppResources.GoToMyVault;
            icon.SetImageResource(Resource.Drawable.icon);
            return view;
        }

        public static WindowManagerLayoutParams GetOverlayLayoutParams()
        {
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
            layoutParams.Gravity = GravityFlags.Bottom | GravityFlags.Left;

            return layoutParams;
        }

        public static Point GetOverlayAnchorPosition(AccessibilityNodeInfo root, AccessibilityNodeInfo anchorView)
        {
            var rootRect = new Rect();
            root.GetBoundsInScreen(rootRect);
            var rootRectHeight = rootRect.Height();

            var anchorViewRect = new Rect();
            anchorView.GetBoundsInScreen(anchorViewRect);
            var anchorViewRectLeft = anchorViewRect.Left;
            var anchorViewRectTop = anchorViewRect.Top;

            var navBarHeight = GetNavigationBarHeight();
            var calculatedTop = rootRectHeight - anchorViewRectTop - navBarHeight;
            return new Point(anchorViewRectLeft, calculatedTop);
        }

        public static Point GetOverlayAnchorPosition(int nodeHash, AccessibilityNodeInfo root, AccessibilityEvent e)
        {
            Point point = null;
            var allEditTexts = GetWindowNodes(root, e, n => EditText(n), false);
            foreach(var node in allEditTexts)
            {
                if(node.GetHashCode() == nodeHash)
                {
                    point = GetOverlayAnchorPosition(root, node);
                    break;
                }
            }
            allEditTexts.Dispose();
            return point;
        }

        private static int GetStatusBarHeight()
        {
            return GetSystemResourceDimenPx("status_bar_height");
        }

        private static int GetNavigationBarHeight()
        {
            return GetSystemResourceDimenPx("navigation_bar_height");
        }

        private static int GetSystemResourceDimenPx(string resName)
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            var barHeight = 0;
            var resourceId = activity.Resources.GetIdentifier(resName, "dimen", "android");
            if(resourceId > 0)
            {
                barHeight = activity.Resources.GetDimensionPixelSize(resourceId);
            }
            return barHeight;
        }
    }
}
