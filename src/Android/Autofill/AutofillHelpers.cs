using System.Collections.Generic;
using Android.Content;
using Android.Service.Autofill;
using Android.Widget;
using System.Linq;
using Android.App;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.Core.Enums;
using Android.Views.Autofill;
using Bit.Core.Abstractions;

namespace Bit.Droid.Autofill
{
    public static class AutofillHelpers
    {
        private static int _pendingIntentId = 0;

        // These browser work natively with the autofill framework
        public static HashSet<string> TrustedBrowsers = new HashSet<string>
        {
            "org.mozilla.focus",
            "org.mozilla.klar",
            "com.duckduckgo.mobile.android",
        };

        // These browsers work using the compatibility shim for the autofill framework
        // Be sure to keep this in sync with values in Resources/xml/autofillservice.xml
        public static HashSet<string> CompatBrowsers = new HashSet<string>
        {
            "com.android.browser",
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.opera.browser.beta",
            "com.opera.mini.native",
            "com.opera.mini.native.beta",
            "com.opera.touch",
            "com.sec.android.app.sbrowser",
            "com.sec.android.app.sbrowser.beta",
            "org.mozilla.fennec_aurora",
            "org.mozilla.fennec_fdroid",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.fenix",
            "org.mozilla.fenix.nightly",
            "org.mozilla.reference.browser",
            "org.mozilla.rocket",
            "com.brave.browser",
            "com.google.android.apps.chrome",
            "com.google.android.apps.chrome_dev",
            "com.yandex.browser",
            "org.codeaurora.swe.browser",
            "com.amazon.cloud9",
            "mark.via.gp",
            "org.bromite.bromite",
            "org.chromium.chrome",
            "com.kiwibrowser.browser",
            "com.ecosia.android",
            "com.qwant.liberty",
            "org.torproject.torbrowser",
            "com.vivaldi.browser",
            "com.vivaldi.browser.snapshot",
            "com.naver.whale",
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
                    return allCiphers.Select(c => new FilledItem(c)).ToList();
                }
            }
            else if (parser.FieldCollection.FillableForCard)
            {
                var ciphers = await cipherService.GetAllDecryptedAsync();
                return ciphers.Where(c => c.Type == CipherType.Card).Select(c => new FilledItem(c)).ToList();
            }
            return new List<FilledItem>();
        }

        public static FillResponse BuildFillResponse(Parser parser, List<FilledItem> items, bool locked)
        {
            var responseBuilder = new FillResponse.Builder();
            if (items != null && items.Count > 0)
            {
                foreach (var item in items)
                {
                    var dataset = BuildDataset(parser.ApplicationContext, parser.FieldCollection, item);
                    if (dataset != null)
                    {
                        responseBuilder.AddDataset(dataset);
                    }
                }
            }
            responseBuilder.AddDataset(BuildVaultDataset(parser.ApplicationContext, parser.FieldCollection,
                parser.Uri, locked));
            AddSaveInfo(parser, responseBuilder, parser.FieldCollection);
            responseBuilder.SetIgnoredIds(parser.FieldCollection.IgnoreAutofillIds.ToArray());
            return responseBuilder.Build();
        }

        public static Dataset BuildDataset(Context context, FieldCollection fields, FilledItem filledItem)
        {
            var datasetBuilder = new Dataset.Builder(
                BuildListView(filledItem.Name, filledItem.Subtitle, filledItem.Icon, context));
            if (filledItem.ApplyToFields(fields, datasetBuilder))
            {
                return datasetBuilder.Build();
            }
            return null;
        }

        public static Dataset BuildVaultDataset(Context context, FieldCollection fields, string uri, bool locked)
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

            var view = BuildListView(
                AppResources.AutofillWithBitwarden,
                locked ? AppResources.VaultIsLocked : AppResources.GoToMyVault,
                Resource.Drawable.icon,
                context);

            var datasetBuilder = new Dataset.Builder(view);
            datasetBuilder.SetAuthentication(pendingIntent.IntentSender);

            // Dataset must have a value set. We will reset this in the main activity when the real item is chosen.
            foreach (var autofillId in fields.AutofillIds)
            {
                datasetBuilder.SetValue(autofillId, AutofillValue.ForText("PLACEHOLDER"));
            }
            return datasetBuilder.Build();
        }

        public static RemoteViews BuildListView(string text, string subtext, int iconId, Context context)
        {
            var packageName = context.PackageName;
            var view = new RemoteViews(packageName, Resource.Layout.autofill_listitem);
            view.SetTextViewText(Resource.Id.text1, text);
            view.SetTextViewText(Resource.Id.text2, subtext);
            view.SetImageViewResource(Resource.Id.icon, iconId);
            return view;
        }

        public static void AddSaveInfo(Parser parser, FillResponse.Builder responseBuilder, FieldCollection fields)
        {
            // Docs state that password fields cannot be reliably saved in Compat mode since they will show as
            // masked values.
            var compatBrowser = CompatBrowsers.Contains(parser.PackageName);
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
