using System;
using System.Collections.Generic;
using System.Linq;
using Android.Content;
using Android.Graphics;
using Android.OS;
using Android.Provider;
using Android.Views;
using Android.Views.Accessibility;
using Android.Widget;
using Bit.App.Resources;
using Bit.Core;

namespace Bit.Droid.Accessibility
{
    public static class AccessibilityHelpers
    {
        public static Credentials LastCredentials = null;
        public static string SystemUiPackage = "com.android.systemui";
        public static string BitwardenTag = "bw_access";
        public static bool IsAutofillTileAdded = false;
        public static bool IsAccessibilityBroadcastReady = false;

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
            new Browser("com.yandex.browser", "bro_omnibar_address_title_text,bro_omnibox_collapsed_title",
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
            new Browser("com.vivaldi.browser.snapshot", "url_bar"),
            new Browser("com.feedback.browser.wjbrowser", "addressbar_url"),
            new Browser("com.naver.whale", "url_bar"),
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
            foreach (var node in testNodesData)
            {
                System.Diagnostics.Debug.WriteLine("Node: {0} = {1}", node.id, node.text);
            }
        }

        public static string GetUri(AccessibilityNodeInfo root)
        {
            var uri = string.Concat(Constants.AndroidAppProtocol, root.PackageName);
            if (SupportedBrowsers.ContainsKey(root.PackageName))
            {
                var browser = SupportedBrowsers[root.PackageName];
                AccessibilityNodeInfo addressNode = null;
                foreach (var uriViewId in browser.UriViewId.Split(","))
                {
                    addressNode = root.FindAccessibilityNodeInfosByViewId(
                        $"{root.PackageName}:id/{uriViewId}").FirstOrDefault();
                    if (addressNode != null)
                    {
                        break;
                    }
                }

                if (addressNode != null)
                {
                    uri = ExtractUri(uri, addressNode, browser);
                    addressNode.Recycle();
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
            if (addressNode?.Text == null)
            {
                return uri;
            }
            if (addressNode.Text == null)
            {
                return uri;
            }
            uri = browser.GetUriFunction(addressNode.Text)?.Trim();
            if (uri != null && uri.Contains("."))
            {
                if (!uri.Contains("://") && !uri.Contains(" "))
                {
                    uri = string.Concat("http://", uri);
                }
                else if (Build.VERSION.SdkInt <= BuildVersionCodes.KitkatWatch)
                {
                    var parts = uri.Split(new string[] { ". " }, StringSplitOptions.None);
                    if (parts.Length > 1)
                    {
                        var urlPart = parts.FirstOrDefault(p => p.StartsWith("http"));
                        if (urlPart != null)
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
            if (credentials == null)
            {
                return false;
            }
            if (Uri.TryCreate(credentials.LastUri, UriKind.Absolute, out Uri lastUri) &&
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
            foreach (var n in passwordNodes)
            {
                FillEditText(n, LastCredentials?.Password);
            }
        }

        public static void FillEditText(AccessibilityNodeInfo editTextNode, string value)
        {
            if (editTextNode == null || value == null)
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
            if (nodes == null)
            {
                nodes = new NodeList();
            }
            var dispose = disposeIfUnused;
            if (n != null && recursionDepth < 100)
            {
                var add = n.WindowId == e.WindowId &&
                    !(n.ViewIdResourceName?.StartsWith(SystemUiPackage) ?? false) &&
                    condition(n);
                if (add)
                {
                    dispose = false;
                    nodes.Add(n);
                }

                for (var i = 0; i < n.ChildCount; i++)
                {
                    var childNode = n.GetChild(i);
                    if (childNode == null)
                    {
                        continue;
                    }
                    else if (i > 100)
                    {
                        Android.Util.Log.Info(BitwardenTag, "Too many child iterations.");
                        break;
                    }
                    else if (childNode.GetHashCode() == n.GetHashCode())
                    {
                        Android.Util.Log.Info(BitwardenTag, "Child node is the same as parent for some reason.");
                    }
                    else
                    {
                        GetWindowNodes(childNode, e, condition, true, nodes, recursionDepth++);
                    }
                }
            }
            if (dispose)
            {
                n?.Recycle();
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
            foreach (var editText in allEditTexts)
            {
                if (editText.Password)
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

            var isUsernameEditText = false;
            if (usernameEditText != null)
            {
                isUsernameEditText = IsSameNode(usernameEditText, e.Source);
            }
            allEditTexts.Dispose();

            return isUsernameEditText;
        }

        public static bool IsSameNode(AccessibilityNodeInfo node1, AccessibilityNodeInfo node2)
        {
            if (node1 != null && node2 != null)
            {
                return node1.Equals(node2) || node1.GetHashCode() == node2.GetHashCode();
            }
            return false;
        }

        public static bool OverlayPermitted()
        {
            if (Build.VERSION.SdkInt >= BuildVersionCodes.M)
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
            if (Build.VERSION.SdkInt >= BuildVersionCodes.O)
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
            layoutParams.Gravity = GravityFlags.Top | GravityFlags.Left;

            return layoutParams;
        }

        public static Point GetOverlayAnchorPosition(AccessibilityService service, AccessibilityNodeInfo anchorView, 
            int overlayViewHeight, bool isOverlayAboveAnchor)
        {
            var anchorViewRect = new Rect();
            anchorView.GetBoundsInScreen(anchorViewRect);
            var anchorViewX = anchorViewRect.Left;
            var anchorViewY = isOverlayAboveAnchor ? anchorViewRect.Top : anchorViewRect.Bottom;
            anchorViewRect.Dispose();
            
            if (isOverlayAboveAnchor)
            {
                anchorViewY -= overlayViewHeight;
            }
            anchorViewY -= GetStatusBarHeight(service);

            return new Point(anchorViewX, anchorViewY);
        }

        public static Point GetOverlayAnchorPosition(AccessibilityService service, AccessibilityNodeInfo anchorNode, 
            AccessibilityNodeInfo root, IEnumerable<AccessibilityWindowInfo> windows, int overlayViewHeight, 
            bool isOverlayAboveAnchor)
        {
            Point point = null;
            if (anchorNode != null)
            {
                // Update node's info since this is still a reference from an older event
                anchorNode.Refresh();
                if (!anchorNode.VisibleToUser)
                {
                    return new Point(-1, -1);
                }
                if (!anchorNode.Focused)
                {
                    return null;
                }

                // node.VisibleToUser doesn't always give us exactly what we want, so attempt to tighten up the range
                // of visibility
                var inputMethodHeight = 0;
                if (windows != null)
                {
                    if (IsStatusBarExpanded(windows))
                    {
                        return new Point(-1, -1);
                    }
                    inputMethodHeight = GetInputMethodHeight(windows);
                }
                var minY = 0;
                var rootNodeHeight = GetNodeHeight(root);
                if (rootNodeHeight == -1)
                {
                    return null;
                }
                var maxY = rootNodeHeight - GetNavigationBarHeight(service) - GetStatusBarHeight(service) -
                           inputMethodHeight;

                point = GetOverlayAnchorPosition(service, anchorNode, overlayViewHeight, isOverlayAboveAnchor);
                if (point.Y < minY)
                {
                    if (isOverlayAboveAnchor)
                    {
                        // view nearing bounds, anchor to bottom
                        point.X = -1;
                        point.Y = 0;
                    }
                    else
                    {
                        // view out of bounds, hide overlay
                        point.X = -1;
                        point.Y = -1;
                    }
                } 
                else if (point.Y > (maxY - overlayViewHeight))
                {
                    if (isOverlayAboveAnchor)
                    {
                        // view out of bounds, hide overlay
                        point.X = -1;
                        point.Y = -1;
                    }
                    else
                    {
                        // view nearing bounds, anchor to top
                        point.X = 0;
                        point.Y = -1;
                    }
                } 
                else if (isOverlayAboveAnchor && point.Y < (maxY - (overlayViewHeight * 2) - GetNodeHeight(anchorNode)))
                {
                    // This else block forces the overlay to return to bottom alignment as soon as space is available
                    // below the anchor view. Removing this will change the behavior to wait until there isn't enough
                    // space above the anchor view before returning to bottom alignment.
                    point.X = -1;
                    point.Y = 0;
                }
            }
            return point;
        }

        public static bool IsStatusBarExpanded(IEnumerable<AccessibilityWindowInfo> windows)
        {
            if (windows != null && windows.Any())
            {
                var isSystemWindowsOnly = true;
                foreach (var window in windows)
                {
                    if (window.Type != AccessibilityWindowType.System)
                    {
                        isSystemWindowsOnly = false;
                        break;
                    }
                }
                return isSystemWindowsOnly;
            }
            return false;
        }

        public static int GetInputMethodHeight(IEnumerable<AccessibilityWindowInfo> windows)
        {
            var inputMethodWindowHeight = 0;
            if (windows != null)
            {
                foreach (var window in windows)
                {
                    if (window.Type == AccessibilityWindowType.InputMethod)
                    {
                        var windowRect = new Rect();
                        window.GetBoundsInScreen(windowRect);
                        inputMethodWindowHeight = windowRect.Height();
                        break;
                    }
                }
            }
            return inputMethodWindowHeight;
        }
        
        public static bool IsAutofillServicePromptVisible(IEnumerable<AccessibilityWindowInfo> windows)
        {
            return windows?.Any(w => w.Title?.ToLower().Contains("autofill") ?? false) ?? false;
        }

        public static int GetNodeHeight(AccessibilityNodeInfo node)
        {
            if (node == null)
            {
                return -1;
            }
            var nodeRect = new Rect();
            node.GetBoundsInScreen(nodeRect);
            var nodeRectHeight = nodeRect.Height();
            nodeRect.Dispose();
            return nodeRectHeight;
        }

        private static int GetStatusBarHeight(AccessibilityService service)
        {
            return GetSystemResourceDimenPx(service, "status_bar_height");
        }

        private static int GetNavigationBarHeight(AccessibilityService service)
        {
            return GetSystemResourceDimenPx(service, "navigation_bar_height");
        }

        private static int GetSystemResourceDimenPx(AccessibilityService service, string resName)
        {
            var resourceId = service.Resources.GetIdentifier(resName, "dimen", "android");
            if (resourceId > 0)
            {
                return service.Resources.GetDimensionPixelSize(resourceId);
            }
            return 0;
        }
    }
}
