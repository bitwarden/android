using System;
using System.Collections.Generic;
using System.Linq;
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

namespace Bit.Droid.Accessibility
{
    public static class AccessibilityHelpers
    {
        public static Credentials LastCredentials = null;
        public static string SystemUiPackage = "com.android.systemui";
        public static string BitwardenTag = "bw_access";
        public static bool IsAutofillTileAdded = false;
        public static bool IsAccessibilityBroadcastReady = false;

        // Be sure to keep these two sections sorted alphabetically
        public static Dictionary<string, Browser> SupportedBrowsers => new List<Browser>
        {
            // [Section A] Entries also present in the list of Autofill Framework
            //
            // So keep them in sync with:
            //   - AutofillHelpers.{TrustedBrowsers,CompatBrowsers}
            //   - Resources/xml/autofillservice.xml
            new Browser("alook.browser", "search_fragment_input_view"),
            new Browser("com.amazon.cloud9", "url"),
            new Browser("com.android.browser", "url"),
            new Browser("com.android.chrome", "url_bar"),
            // Rem. for "com.android.htmlviewer": doesn't have a URL bar, therefore not present here.
            new Browser("com.avast.android.secure.browser", "editor"),
            new Browser("com.avg.android.secure.browser", "editor"),
            new Browser("com.brave.browser", "url_bar"),
            new Browser("com.brave.browser_beta", "url_bar"),
            new Browser("com.brave.browser_default", "url_bar"),
            new Browser("com.brave.browser_dev", "url_bar"),
            new Browser("com.brave.browser_nightly", "url_bar"),
            new Browser("com.chrome.beta", "url_bar"),
            new Browser("com.chrome.canary", "url_bar"),
            new Browser("com.chrome.dev", "url_bar"),
            new Browser("com.cookiegames.smartcookie", "search"),
            new Browser("com.cookiejarapps.android.smartcookieweb", "mozac_browser_toolbar_url_view"),
            new Browser("com.duckduckgo.mobile.android", "omnibarTextInput"),
            new Browser("com.ecosia.android", "url_bar"),
            new Browser("com.google.android.apps.chrome", "url_bar"),
            new Browser("com.google.android.apps.chrome_dev", "url_bar"),
            new Browser("com.jamal2367.styx", "search"),
            new Browser("com.kiwibrowser.browser", "url_bar"),
            new Browser("com.microsoft.emmx", "url_bar"),
            new Browser("com.microsoft.emmx.beta", "url_bar"),
            new Browser("com.microsoft.emmx.canary", "url_bar"),
            new Browser("com.microsoft.emmx.dev", "url_bar"),
            new Browser("com.mmbox.browser", "search_box"),
            new Browser("com.mmbox.xbrowser", "search_box"),
            new Browser("com.mycompany.app.soulbrowser", "edit_text"),
            new Browser("com.naver.whale", "url_bar"),
            new Browser("com.opera.browser", "url_field"),
            new Browser("com.opera.browser.beta", "url_field"),
            new Browser("com.opera.mini.native", "url_field"),
            new Browser("com.opera.mini.native.beta", "url_field"),
            new Browser("com.opera.touch", "addressbarEdit"),
            new Browser("com.qwant.liberty", "mozac_browser_toolbar_url_view,url_bar_title"), // 2nd = Legacy (before v4)
            new Browser("com.sec.android.app.sbrowser", "location_bar_edit_text"),
            new Browser("com.sec.android.app.sbrowser.beta", "location_bar_edit_text"),
            new Browser("com.stoutner.privacybrowser.free", "url_edittext"),
            new Browser("com.stoutner.privacybrowser.standard", "url_edittext"),
            new Browser("com.vivaldi.browser", "url_bar"),
            new Browser("com.vivaldi.browser.snapshot", "url_bar"),
            new Browser("com.vivaldi.browser.sopranos", "url_bar"),
            new Browser("com.yandex.browser", "bro_omnibar_address_title_text,bro_omnibox_collapsed_title",
                (s) => s.Split(new char[]{' ', ' '}).FirstOrDefault()), // 0 = Regular Space, 1 = No-break space (00A0)
            new Browser("com.z28j.feel", "g2"),
            new Browser("idm.internet.download.manager", "search"),
            new Browser("idm.internet.download.manager.adm.lite", "search"),
            new Browser("idm.internet.download.manager.plus", "search"),
            new Browser("io.github.forkmaintainers.iceraven", "mozac_browser_toolbar_url_view"),
            new Browser("mark.via", "am,an"),
            new Browser("mark.via.gp", "as"),
            new Browser("net.slions.fulguris.full.download", "search"),
            new Browser("net.slions.fulguris.full.download.debug", "search"),
            new Browser("net.slions.fulguris.full.playstore", "search"),
            new Browser("net.slions.fulguris.full.playstore.debug", "search"),
            new Browser("org.adblockplus.browser", "url_bar,url_bar_title"), // 2nd = Legacy (before v2)
            new Browser("org.adblockplus.browser.beta", "url_bar,url_bar_title"), // 2nd = Legacy (before v2)
            new Browser("org.bromite.bromite", "url_bar"),
            new Browser("org.bromite.chromium", "url_bar"),
            new Browser("org.chromium.chrome", "url_bar"),
            new Browser("org.codeaurora.swe.browser", "url_bar"),
            new Browser("org.gnu.icecat", "url_bar_title,mozac_browser_toolbar_url_view"), // 2nd = Anticipation
            new Browser("org.mozilla.fenix", "mozac_browser_toolbar_url_view"),
            new Browser("org.mozilla.fenix.nightly", "mozac_browser_toolbar_url_view"), // [DEPRECATED ENTRY]
            new Browser("org.mozilla.fennec_aurora", "mozac_browser_toolbar_url_view,url_bar_title"), // [DEPRECATED ENTRY]
            new Browser("org.mozilla.fennec_fdroid", "mozac_browser_toolbar_url_view,url_bar_title"), // 2nd = Legacy
            new Browser("org.mozilla.firefox", "mozac_browser_toolbar_url_view,url_bar_title"), // 2nd = Legacy
            new Browser("org.mozilla.firefox_beta", "mozac_browser_toolbar_url_view,url_bar_title"), // 2nd = Legacy
            new Browser("org.mozilla.focus", "display_url"),
            new Browser("org.mozilla.klar", "display_url"),
            new Browser("org.mozilla.reference.browser", "mozac_browser_toolbar_url_view"),
            new Browser("org.mozilla.rocket", "display_url"),
            new Browser("org.torproject.torbrowser", "mozac_browser_toolbar_url_view,url_bar_title"), // 2nd = Legacy (before v10.0.3)
            new Browser("org.torproject.torbrowser_alpha", "mozac_browser_toolbar_url_view,url_bar_title"), // 2nd = Legacy (before v10.0a8)
            new Browser("org.ungoogled.chromium.extensions.stable", "url_bar"),
            new Browser("org.ungoogled.chromium.stable", "url_bar"),

            // [Section B] Entries only present here
            //
            // FIXME: Test the compatibility of these with Autofill Framework
            new Browser("acr.browser.barebones", "search"),
            new Browser("acr.browser.lightning", "search"),
            new Browser("com.feedback.browser.wjbrowser", "addressbar_url"),
            new Browser("com.ghostery.android.ghostery", "search_field"),
            new Browser("com.htc.sense.browser", "title"),
            new Browser("com.jerky.browser2", "enterUrl"),
            new Browser("com.ksmobile.cb", "address_bar_edit_text"),
            new Browser("com.linkbubble.playstore", "url_text"),
            new Browser("com.mx.browser", "address_editor_with_progress"),
            new Browser("com.mx.browser.tablet", "address_editor_with_progress"),
            new Browser("com.nubelacorp.javelin", "enterUrl"),
            new Browser("jp.co.fenrir.android.sleipnir", "url_text"),
            new Browser("jp.co.fenrir.android.sleipnir_black", "url_text"),
            new Browser("jp.co.fenrir.android.sleipnir_test", "url_text"),
            new Browser("mobi.mgeek.TunnyBrowser", "title"),
            new Browser("org.iron.srware", "url_bar"),
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

        // Be sure to keep these sections sorted alphabetically
        public static Dictionary<string, KnownUsernameField> KnownUsernameFields => new List<KnownUsernameField>
        {
            /**************************************************************************************
             * SECTION A ——— World-renowned web sites/applications
             *************************************************************************************/

            // REM.: For this type of web sites/applications, the Top 100 (SimilarWeb, 2019)
            //       and the Top 50 (Alexa Internet, 2020) are covered. National variants
            //       have been added when available. Mobile and desktop versions supported.
            //
            //       A few other popular web sites/applications have also been added.
            //
            //       Could not be added, however:
            //       web sites/applications that don't use an "id" attribute for their login field.

            // NOTE: The case of OAuth compatible web sites/applications that also provide
            //       a "user ID only" login page in this situation
            //       was taken into account in the tests as well.

            /*
             * A
             */

            // Amazon ——— ap_email_login = mobile / ap_email = desktop (amazon.co.jp currently uses ap_email in both cases, as of July 2020).
            new KnownUsernameField("amazon.ae",              new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.ca",              new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.cn",              new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.co.jp",           new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.co.uk",           new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.com",             new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.com.au",          new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.com.br",          new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.com.mx",          new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.com.tr",          new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.de",              new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.es",              new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.fr",              new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.in",              new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.it",              new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.nl",              new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.pl",              new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.sa",              new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.se",              new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),
            new KnownUsernameField("amazon.sg",              new (string, string)[] { ("contains:/ap/signin", "ap_email_login,ap_email") }),

            // Amazon Web Services
            new KnownUsernameField("signin.aws.amazon.com",  new (string, string)[] { ("signin", "resolving_input") }),

            // Atlassian
            new KnownUsernameField("id.atlassian.com",       new (string, string)[] { ("login", "username") }),

            /*
             * B
             */

            // Bitly ——— enterprise users.
            new KnownUsernameField("bitly.com",              new (string, string)[] { ("/sso/url_slug", "url_slug") }),

            /*
             * E
             */

            // eBay ——— 1st = traditional access / 2nd = direct access (i.e. https://signin.ebay.tld/).
            new KnownUsernameField("signin.befr.ebay.be",    new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.benl.ebay.be",    new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.cafr.ebay.ca",    new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.at",         new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.be",         new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.ca",         new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.ch",         new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.co.uk",      new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.com",        new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.com.au",     new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.com.hk",     new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.com.my",     new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.com.sg",     new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.de",         new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.es",         new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.fr",         new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.ie",         new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.in",         new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.it",         new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.nl",         new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.ph",         new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),
            new KnownUsernameField("signin.ebay.pl",         new (string, string)[] { ("iendswith:eBayISAPI.dll", "userid"), ("icontains:/signin/", "userid") }),

            /*
             * G
             */

            // Google ——— 1st = used in most cases (v2) / 2nd = used in some cases (v1).
            new KnownUsernameField("accounts.google.com",    new (string, string)[] { ("identifier", "identifierId"), ("ServiceLogin", "Email") }),

            /*
             * P
             */

            // PayPal ——— 1st = traditional access / 2nd = access using OAuth.
            new KnownUsernameField("paypal.com",             new (string, string)[] { ("signin", "email"), ("contains:/connect/", "email") }),

            /*
             * T
             */

            // Tumblr ——— despite "signup" in its ID, it's the login field (the website offers registration if the account doesn't exist).
            new KnownUsernameField("tumblr.com",             new (string, string)[] { ("login", "signup_determine_email") }),

            /*
             * Y
             */

            // Yandex
            new KnownUsernameField("passport.yandex.az",     new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.by",     new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.co.il",  new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.com",    new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.com.am", new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.com.ge", new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.com.tr", new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.ee",     new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.fi",     new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.fr",     new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.kg",     new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.kz",     new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.lt",     new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.lv",     new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.md",     new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.pl",     new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.ru",     new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.tj",     new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.tm",     new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.ua",     new (string, string)[] { ("auth", "passp-field-login") }),
            new KnownUsernameField("passport.yandex.uz",     new (string, string)[] { ("auth", "passp-field-login") }),

            /**************************************************************************************
             * SECTION B ——— Top 100 worldwide
             *************************************************************************************/

            // As of July 2020, all entries that needed to be added from
            // Top 100 (SimilarWeb, 2019) and Top 50 (Alexa Internet, 2020)
            // matched section A.
            //
            // Therefore, no entry currently.

            /**************************************************************************************
             * SECTION C ——— Top 20 for selected countries
             *************************************************************************************/

            // REM.: For these selected countries, the Top 20 (SimilarWeb, 2020)
            //       and the Top 20 (Alexa Internet, 2020) are covered.
            //       Mobile and desktop versions supported.
            //
            //       Could not be added, however:
            //       web sites/applications that don't use an "id" attribute for their login field.

            /*
             * Japan
             */

            // NTT DOCOMO ——— mainly used for "My docomo".
            new KnownUsernameField("cfg.smt.docomo.ne.jp",   new (string, string)[] { ("contains:/auth/", "Di_Uid") }),
            new KnownUsernameField("id.smt.docomo.ne.jp",    new (string, string)[] { ("contains:/cgi7/", "Di_Uid") }),

            /**************************************************************************************
             * SECTION D ——— Miscellaneous
             *************************************************************************************/

            /*
             * Various entries ——— Following user requests, etc.
             */

            // No entry, currently.

            /**************************************************************************************
             * SECTION Z ——— Special forms
             *
             * Despite "user ID + password" fields both visible, detection rules required.
             *************************************************************************************/

            /*
             * Main
             */

            // No entry, currently.

            /*
             * Test/example purposes only
             */

            // GitHub ——— VERY special case (signup form, just to test the proper functioning of special forms).
            new KnownUsernameField("github.com",             new (string, string)[] { ("", "user[login]-footer") }),
        }.ToDictionary(n => n.UriAuthority);

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

        private static string ExtractUri(string uri, AccessibilityNodeInfo addressNode, Browser browser)
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
                var hasHttpProtocol = uri.StartsWith("http://") || uri.StartsWith("https://");
                if (!hasHttpProtocol && uri.Contains("."))
                {
                    if (Uri.TryCreate("https://" + uri, UriKind.Absolute, out var _))
                    {
                        return string.Concat("https://", uri);
                    }
                }
                if (Uri.TryCreate(uri, UriKind.Absolute, out var _))
                {
                    return uri;
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

        public static AccessibilityNodeInfo GetUsernameEditText(string uriString, 
            IEnumerable<AccessibilityNodeInfo> allEditTexts)
        {
            string uriAuthority = null;
            string uriKey = null;
            string uriLocalPath = null;
            if (Uri.TryCreate(uriString, UriKind.Absolute, out var uri))
            {
                uriAuthority = uri.Authority;
                uriKey = uriAuthority.StartsWith("www.", StringComparison.Ordinal) ? uriAuthority.Substring(4) : uriAuthority;
                uriLocalPath = uri.LocalPath;
            }

            if (!string.IsNullOrEmpty(uriKey))
            {
                // Uncomment this to log values necessary for username field discovery
                // foreach (var editText in allEditTexts)
                // {
                //     System.Diagnostics.Debug.WriteLine(">>> uriKey: {0}, uriLocalPath: {1}, viewId: {2}", uriKey,
                //         uriLocalPath, editText.ViewIdResourceName);
                // }

                if (KnownUsernameFields.ContainsKey(uriKey))
                {
                    var usernameField = KnownUsernameFields[uriKey];
                    (string UriPathWanted, string UsernameViewId)[] accessOptions = usernameField.AccessOptions;

                    for (int i = 0; i < accessOptions.Length; i++)
                    {
                        string curUriPathWanted = accessOptions[i].UriPathWanted;
                        string curUsernameViewId = accessOptions[i].UsernameViewId;
                        bool uriLocalPathMatches = false;

                        // Case-sensitive comparison
                        if (curUriPathWanted.StartsWith("startswith:", StringComparison.Ordinal))
                        {
                            curUriPathWanted = curUriPathWanted.Substring(11);
                            uriLocalPathMatches = uriLocalPath.StartsWith(curUriPathWanted, StringComparison.Ordinal);
                        }
                        else if (curUriPathWanted.StartsWith("contains:", StringComparison.Ordinal))
                        {
                            curUriPathWanted = curUriPathWanted.Substring(9);
                            uriLocalPathMatches = uriLocalPath.Contains(curUriPathWanted, StringComparison.Ordinal);
                        }
                        else if (curUriPathWanted.StartsWith("endswith:", StringComparison.Ordinal))
                        {
                            curUriPathWanted = curUriPathWanted.Substring(9);
                            uriLocalPathMatches = uriLocalPath.EndsWith(curUriPathWanted, StringComparison.Ordinal);
                        }

                        // Case-insensitive comparison
                        else if (curUriPathWanted.StartsWith("istartswith:", StringComparison.Ordinal))
                        {
                            curUriPathWanted = curUriPathWanted.Substring(12);
                            uriLocalPathMatches = uriLocalPath.StartsWith(curUriPathWanted, StringComparison.OrdinalIgnoreCase);
                        }
                        else if (curUriPathWanted.StartsWith("icontains:", StringComparison.Ordinal))
                        {
                            curUriPathWanted = curUriPathWanted.Substring(10);
                            uriLocalPathMatches = uriLocalPath.Contains(curUriPathWanted, StringComparison.OrdinalIgnoreCase);
                        }
                        else if (curUriPathWanted.StartsWith("iendswith:", StringComparison.Ordinal))
                        {
                            curUriPathWanted = curUriPathWanted.Substring(10);
                            uriLocalPathMatches = uriLocalPath.EndsWith(curUriPathWanted, StringComparison.OrdinalIgnoreCase);
                        }

                        // Default type of comparison
                        else
                        {
                            uriLocalPathMatches = uriLocalPath.EndsWith(curUriPathWanted, StringComparison.Ordinal);
                        }

                        if (uriLocalPathMatches)
                        {
                            foreach (var editText in allEditTexts)
                            {
                                foreach (var usernameViewId in curUsernameViewId.Split(","))
                                {
                                    if (usernameViewId == editText.ViewIdResourceName)
                                    {
                                        return editText;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // no match found, attempt to establish username field based on password field
            return GetUsernameEditTextIfPasswordExists(allEditTexts);
        }
        
        private static AccessibilityNodeInfo GetUsernameEditTextIfPasswordExists(
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
            var uriString = GetUri(root);
            var usernameEditText = GetUsernameEditText(uriString, allEditTexts);

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
                if (Settings.CanDrawOverlays(Application.Context))
                {
                    return true;
                }
                
                var appOpsMgr = (AppOpsManager)Application.Context.GetSystemService(Context.AppOpsService);
                var mode = appOpsMgr.CheckOpNoThrow("android:system_alert_window", Process.MyUid(),
                    Application.Context.PackageName);
                if (mode == AppOpsManagerMode.Allowed || mode == AppOpsManagerMode.Ignored)
                {
                    return true;
                }
                
                try
                {
                    var wm = Application.Context.GetSystemService(Context.WindowService)
                        .JavaCast<IWindowManager>();
                    if (wm == null)
                    {
                        return false;
                    }
                    var testView = new View(Application.Context);
                    var layoutParams = GetOverlayLayoutParams();
                    wm.AddView(testView, layoutParams);
                    wm.RemoveView(testView);
                    return true;
                }
                catch { }
                
                return false;
            }
            
            // older android versions are always true
            return true;
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
            icon.SetImageResource(Resource.Drawable.shield);
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
            // Autofill framework not available until API 26
            if (Build.VERSION.SdkInt >= BuildVersionCodes.O) 
            {
                return windows?.Any(w => w.Title?.ToLower().Contains("autofill") ?? false) ?? false;
            }
            return false;
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
