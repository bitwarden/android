using System;
using System.Collections.Generic;
using Android.Content;
using Android.Service.Autofill;
using Android.Widget;
using System.Linq;
using Android.App;
using System.Threading.Tasks;
using Android.App.Slices;
using Android.Graphics;
using Android.Graphics.Drawables;
using Android.OS;
using Android.Runtime;
using Android.Widget.Inline;
using Bit.App.Resources;
using Bit.Core.Enums;
using Android.Views.Autofill;
using AndroidX.AutoFill.Inline;
using AndroidX.AutoFill.Inline.V1;
using Bit.Core.Abstractions;
using SaveFlags = Android.Service.Autofill.SaveFlags;

namespace Bit.Droid.Autofill
{
    public static class AutofillHelpers
    {
        private static int _pendingIntentId = 0;

        // These browsers work natively with the Autofill Framework
        //
        // Be sure:
        //   - to keep these entries sorted alphabetically and
        //
        //   - ... to keep this list in sync with values in AccessibilityHelpers.SupportedBrowsers [Section A], too.
        public static HashSet<string> TrustedBrowsers = new HashSet<string>
        {
            "com.duckduckgo.mobile.android",
            "org.mozilla.focus",
            "org.mozilla.klar",
        };

        // These browsers work using the compatibility shim for the Autofill Framework
        //
        // Be sure:
        //   - to keep these entries sorted alphabetically,
        //   - to keep this list in sync with values in Resources/xml/autofillservice.xml, and
        //
        //   - ... to keep this list in sync with values in AccessibilityHelpers.SupportedBrowsers [Section A], too.
        public static HashSet<string> CompatBrowsers = new HashSet<string>
        {
            "alook.browser",
            "com.amazon.cloud9",
            "com.android.browser",
            "com.android.chrome",
            "com.android.htmlviewer",
            "com.avast.android.secure.browser",
            "com.avg.android.secure.browser",
            "com.brave.browser",
            "com.brave.browser_beta",
            "com.brave.browser_default",
            "com.brave.browser_dev",
            "com.brave.browser_nightly",
            "com.chrome.beta",
            "com.chrome.canary",
            "com.chrome.dev",
            "com.cookiegames.smartcookie",
            "com.cookiejarapps.android.smartcookieweb",
            "com.ecosia.android",
            "com.google.android.apps.chrome",
            "com.google.android.apps.chrome_dev",
            "com.jamal2367.styx",
            "com.kiwibrowser.browser",
            "com.microsoft.emmx",
            "com.microsoft.emmx.beta",
            "com.microsoft.emmx.canary",
            "com.microsoft.emmx.dev",
            "com.mmbox.browser",
            "com.mmbox.xbrowser",
            "com.mycompany.app.soulbrowser",
            "com.naver.whale",
            "com.opera.browser",
            "com.opera.browser.beta",
            "com.opera.mini.native",
            "com.opera.mini.native.beta",
            "com.opera.touch",
            "com.qwant.liberty",
            "com.sec.android.app.sbrowser",
            "com.sec.android.app.sbrowser.beta",
            "com.stoutner.privacybrowser.free",
            "com.stoutner.privacybrowser.standard",
            "com.vivaldi.browser",
            "com.vivaldi.browser.snapshot",
            "com.vivaldi.browser.sopranos",
            "com.yandex.browser",
            "com.z28j.feel",
            "idm.internet.download.manager",
            "idm.internet.download.manager.adm.lite",
            "idm.internet.download.manager.plus",
            "io.github.forkmaintainers.iceraven",
            "mark.via",
            "mark.via.gp",
            "net.slions.fulguris.full.download",
            "net.slions.fulguris.full.download.debug",            
            "net.slions.fulguris.full.playstore",
            "net.slions.fulguris.full.playstore.debug",
            "org.adblockplus.browser",
            "org.adblockplus.browser.beta",
            "org.bromite.bromite",
            "org.bromite.chromium",
            "org.chromium.chrome",
            "org.codeaurora.swe.browser",
            "org.gnu.icecat",
            "org.mozilla.fenix",
            "org.mozilla.fenix.nightly",
            "org.mozilla.fennec_aurora",
            "org.mozilla.fennec_fdroid",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.reference.browser",
            "org.mozilla.rocket",
            "org.torproject.torbrowser",
            "org.torproject.torbrowser_alpha",
            "org.ungoogled.chromium.extensions.stable",
            "org.ungoogled.chromium.stable",
        };

        // The URLs are blacklisted from autofilling
        public static HashSet<string> BlacklistedUris = new HashSet<string>
        {
            "androidapp://android",
            "androidapp://com.android.settings",
            "androidapp://com.x8bit.bitwarden",
            "androidapp://com.oneplus.applocker",
        };

        public static async Task<List<FilledItem>> GetFillItemsAsync(Parser parser, ICipherService cipherService)
        {
            if (parser.FieldCollection.FillableForLogin)
            {
                var ciphers = await cipherService.GetAllDecryptedByUrlAsync(parser.Uri);
                if (ciphers.Item1.Any() || ciphers.Item2.Any())
                {
                    var allCiphers = ciphers.Item1.ToList();
                    allCiphers.AddRange(ciphers.Item2.ToList());
                    var nonPromptCiphers = allCiphers.Where(cipher => cipher.Reprompt == CipherRepromptType.None);
                    return nonPromptCiphers.Select(c => new FilledItem(c)).ToList();
                }
            }
            else if (parser.FieldCollection.FillableForCard)
            {
                var ciphers = await cipherService.GetAllDecryptedAsync();
                return ciphers.Where(c => c.Type == CipherType.Card && c.Reprompt == CipherRepromptType.None).Select(c => new FilledItem(c)).ToList();
            }
            return new List<FilledItem>();
        }

        public static FillResponse BuildFillResponse(Parser parser, List<FilledItem> items, bool locked,
            bool inlineAutofillEnabled, FillRequest fillRequest = null)
        {
            // Acquire inline presentation specs on Android 11+
            IList<InlinePresentationSpec> inlinePresentationSpecs = null;
            var inlinePresentationSpecsCount = 0;
            var inlineMaxSuggestedCount = 0;
            if (inlineAutofillEnabled && fillRequest != null && (int)Build.VERSION.SdkInt >= 30)
            {
                var inlineSuggestionsRequest = fillRequest.InlineSuggestionsRequest;
                inlineMaxSuggestedCount = inlineSuggestionsRequest?.MaxSuggestionCount ?? 0;
                inlinePresentationSpecs = inlineSuggestionsRequest?.InlinePresentationSpecs;
                inlinePresentationSpecsCount = inlinePresentationSpecs?.Count ?? 0;
            }
            
            // Build response
            var responseBuilder = new FillResponse.Builder();
            if (items != null && items.Count > 0)
            {
                var maxItems = items.Count;
                if (inlineMaxSuggestedCount > 0)
                {
                    // -1 to adjust for 'open vault' option
                    maxItems = Math.Min(maxItems, inlineMaxSuggestedCount - 1);
                }
                for (int i = 0; i < maxItems; i++)
                {
                    InlinePresentationSpec inlinePresentationSpec = null;
                    if (inlinePresentationSpecs != null)
                    {
                        if (i < inlinePresentationSpecsCount)
                        {
                            inlinePresentationSpec = inlinePresentationSpecs[i];
                        }
                        else
                        {
                            // If the max suggestion count is larger than the number of specs in the list, then
                            // the last spec is used for the remainder of the suggestions
                            inlinePresentationSpec = inlinePresentationSpecs[inlinePresentationSpecsCount - 1];
                        }
                    }
                    var dataset = BuildDataset(parser.ApplicationContext, parser.FieldCollection, items[i], 
                        inlinePresentationSpec);
                    if (dataset != null)
                    {
                        responseBuilder.AddDataset(dataset);
                    }
                }
            }
            responseBuilder.AddDataset(BuildVaultDataset(parser.ApplicationContext, parser.FieldCollection,
                parser.Uri, locked, inlinePresentationSpecs));
            AddSaveInfo(parser, fillRequest, responseBuilder, parser.FieldCollection);
            responseBuilder.SetIgnoredIds(parser.FieldCollection.IgnoreAutofillIds.ToArray());
            return responseBuilder.Build();
        }

        public static Dataset BuildDataset(Context context, FieldCollection fields, FilledItem filledItem,
            InlinePresentationSpec inlinePresentationSpec = null)
        {
            var overlayPresentation = BuildOverlayPresentation(
                filledItem.Name,
                filledItem.Subtitle,
                filledItem.Icon,
                context);
            
            var inlinePresentation = BuildInlinePresentation(
                inlinePresentationSpec, 
                filledItem.Name, 
                filledItem.Subtitle, 
                filledItem.Icon, 
                null, 
                context);

            var datasetBuilder = new Dataset.Builder(overlayPresentation);
            if (inlinePresentation != null)
            {
                datasetBuilder.SetInlinePresentation(inlinePresentation);
            }
            if (filledItem.ApplyToFields(fields, datasetBuilder))
            {
                return datasetBuilder.Build();
            }
            return null;
        }

        public static Dataset BuildVaultDataset(Context context, FieldCollection fields, string uri, bool locked,
            IList<InlinePresentationSpec> inlinePresentationSpecs = null)
        {
            var intent = new Intent(context, typeof(MainActivity));
            intent.PutExtra("autofillFramework", true);
            if (fields.FillableForLogin)
            {
                intent.PutExtra("autofillFrameworkFillType", (int)CipherType.Login);
            }
            else if (fields.FillableForCard)
            {
                intent.PutExtra("autofillFrameworkFillType", (int)CipherType.Card);
            }
            else if (fields.FillableForIdentity)
            {
                intent.PutExtra("autofillFrameworkFillType", (int)CipherType.Identity);
            }
            else
            {
                return null;
            }
            intent.PutExtra("autofillFrameworkUri", uri);
            var pendingIntent = PendingIntent.GetActivity(context, ++_pendingIntentId, intent,
                PendingIntentFlags.CancelCurrent);

            var overlayPresentation = BuildOverlayPresentation(
                AppResources.AutofillWithBitwarden,
                locked ? AppResources.VaultIsLocked : AppResources.GoToMyVault,
                Resource.Drawable.icon,
                context);

            var inlinePresentation = BuildInlinePresentation(
                inlinePresentationSpecs?.Last(), 
                AppResources.Bitwarden, 
                locked ? AppResources.VaultIsLocked : AppResources.MyVault, 
                Resource.Drawable.icon, 
                pendingIntent, 
                context);

            var datasetBuilder = new Dataset.Builder(overlayPresentation);
            if (inlinePresentation != null)
            {
                datasetBuilder.SetInlinePresentation(inlinePresentation);
            }
            datasetBuilder.SetAuthentication(pendingIntent?.IntentSender);

            // Dataset must have a value set. We will reset this in the main activity when the real item is chosen.
            foreach (var autofillId in fields.AutofillIds)
            {
                datasetBuilder.SetValue(autofillId, AutofillValue.ForText("PLACEHOLDER"));
            }
            return datasetBuilder.Build();
        }

        public static RemoteViews BuildOverlayPresentation(string text, string subtext, int iconId, Context context)
        {
            var packageName = context.PackageName;
            var view = new RemoteViews(packageName, Resource.Layout.autofill_listitem);
            view.SetTextViewText(Resource.Id.text1, text);
            view.SetTextViewText(Resource.Id.text2, subtext);
            view.SetImageViewResource(Resource.Id.icon, iconId);
            return view;
        }

        public static InlinePresentation BuildInlinePresentation(InlinePresentationSpec inlinePresentationSpec,
            string text, string subtext, int iconId, PendingIntent pendingIntent, Context context)
        {
            if ((int)Build.VERSION.SdkInt < 30 || inlinePresentationSpec == null)
            {
                return null;
            }
            if (pendingIntent == null)
            {
                // InlinePresentation requires nonNull pending intent (even though we only utilize one for the
                // "my vault" presentation) so we're including an empty one here
                pendingIntent = PendingIntent.GetService(context, 0, new Intent(),
                    PendingIntentFlags.OneShot | PendingIntentFlags.UpdateCurrent);
            }
            var slice = CreateInlinePresentationSlice(
                inlinePresentationSpec,
                text,
                subtext,
                iconId,
                "Autofill option",
                pendingIntent,
                context);
            if (slice != null)
            {
                return new InlinePresentation(slice, inlinePresentationSpec, false);
            }
            return null;
        }

        private static Slice CreateInlinePresentationSlice(
            InlinePresentationSpec inlinePresentationSpec,
            string text,
            string subtext,
            int iconId,
            string contentDescription,
            PendingIntent pendingIntent,
            Context context)
        {
            var imeStyle = inlinePresentationSpec.Style;
            if (!UiVersions.GetVersions(imeStyle).Contains(UiVersions.InlineUiVersion1))
            {
                return null;
            }
            var contentBuilder = InlineSuggestionUi.NewContentBuilder(pendingIntent)
                .SetContentDescription(contentDescription);
            if (!string.IsNullOrWhiteSpace(text))
            {
                contentBuilder.SetTitle(text);
            }
            if (!string.IsNullOrWhiteSpace(subtext))
            {
                contentBuilder.SetSubtitle(subtext);
            }
            if (iconId > 0)
            {
                var icon = Icon.CreateWithResource(context, iconId);
                if (icon != null)
                {
                    if (iconId == Resource.Drawable.icon)
                    {
                        // Don't tint our logo
                        icon.SetTintBlendMode(BlendMode.Dst);
                    }
                    contentBuilder.SetStartIcon(icon);
                }
            }
            return contentBuilder.Build().JavaCast<InlineSuggestionUi.Content>()?.Slice;
        }

        public static void AddSaveInfo(Parser parser, FillRequest fillRequest, FillResponse.Builder responseBuilder, 
            FieldCollection fields)
        {
            // Docs state that password fields cannot be reliably saved in Compat mode since they will show as
            // masked values.
            bool? compatRequest = null;
            if (Build.VERSION.SdkInt >= BuildVersionCodes.Q && fillRequest != null)
            {
                // Attempt to automatically establish compat request mode on Android 10+
                compatRequest = (fillRequest.Flags | FillRequest.FlagCompatibilityModeRequest) == fillRequest.Flags;
            }
            var compatBrowser = compatRequest ?? CompatBrowsers.Contains(parser.PackageName);
            if (compatBrowser && fields.SaveType == SaveDataType.Password)
            {
                return;
            }

            var requiredIds = fields.GetRequiredSaveFields();
            if (fields.SaveType == SaveDataType.Generic || requiredIds.Length == 0)
            {
                return;
            }

            var saveBuilder = new SaveInfo.Builder(fields.SaveType, requiredIds);
            var optionalIds = fields.GetOptionalSaveIds();
            if (optionalIds.Length > 0)
            {
                saveBuilder.SetOptionalIds(optionalIds);
            }
            if (compatBrowser)
            {
                saveBuilder.SetFlags(SaveFlags.SaveOnAllViewsInvisible);
            }
            responseBuilder.SetSaveInfo(saveBuilder.Build());
        }
    }
}
