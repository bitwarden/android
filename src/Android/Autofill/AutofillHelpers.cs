using System;
using System.Collections.Generic;
using Android.Content;
using Android.Service.Autofill;
using Android.Views;
using Android.Widget;
using System.Linq;
using Android.App;
using Bit.App.Abstractions;
using System.Threading.Tasks;

namespace Bit.Android.Autofill
{
    public static class AutofillHelpers
    {
        public static async Task<List<IFilledItem>> GetFillItemsAsync(ICipherService service, string uri)
        {
            var items = new List<IFilledItem>();
            var ciphers = await service.GetAllAsync(uri);
            if(ciphers.Item1.Any() || ciphers.Item2.Any())
            {
                var allCiphers = ciphers.Item1.ToList();
                allCiphers.AddRange(ciphers.Item2.ToList());
                foreach(var cipher in allCiphers)
                {
                    items.Add(new CipherFilledItem(cipher));
                }
            }

            return items;
        }

        public static FillResponse BuildFillResponse(Context context, FieldCollection fields, List<IFilledItem> items)
        {
            var responseBuilder = new FillResponse.Builder();
            if(items != null && items.Count > 0)
            {
                foreach(var item in items)
                {
                    var dataset = BuildDataset(context, fields, item);
                    if(dataset != null)
                    {
                        responseBuilder.AddDataset(dataset);
                    }
                }
            }

            AddSaveInfo(responseBuilder, fields);
            responseBuilder.SetIgnoredIds(fields.IgnoreAutofillIds.ToArray());
            return responseBuilder.Build();
        }

        public static Dataset BuildDataset(Context context, FieldCollection fields, IFilledItem filledItem)
        {
            var datasetBuilder = new Dataset.Builder(
                BuildListView(context.PackageName, filledItem.Name, filledItem.Subtitle, filledItem.Icon));
            if(filledItem.ApplyToFields(fields, datasetBuilder))
            {
                return datasetBuilder.Build();
            }
            return null;
        }

        public static FillResponse BuildAuthResponse(Context context, FieldCollection fields, string uri)
        {
            var responseBuilder = new FillResponse.Builder();
            var view = BuildListView(context.PackageName, "Auto-fill with bitwarden",
                "Vault is locked", Resource.Drawable.icon);
            var intent = new Intent(context, typeof(MainActivity));
            intent.PutExtra("autofillFramework", true);
            intent.PutExtra("autofillFrameworkUri", uri);
            var pendingIntent = PendingIntent.GetActivity(context, 0, intent, PendingIntentFlags.CancelCurrent);
            responseBuilder.SetAuthentication(fields.AutofillIds.ToArray(), pendingIntent.IntentSender, view);
            AddSaveInfo(responseBuilder, fields);
            responseBuilder.SetIgnoredIds(fields.IgnoreAutofillIds.ToArray());
            return responseBuilder.Build();
        }

        public static RemoteViews BuildListView(string packageName, string text, string subtext, int iconId)
        {
            var view = new RemoteViews(packageName, Resource.Layout.autofill_listitem);
            view.SetTextViewText(Resource.Id.text, text);
            view.SetTextViewText(Resource.Id.text2, subtext);
            view.SetImageViewResource(Resource.Id.icon, iconId);
            return view;
        }

        public static void AddSaveInfo(FillResponse.Builder responseBuilder, FieldCollection fields)
        {
            var saveInfo = new SaveInfo.Builder(SaveDataType.Password, fields.AutofillIds.ToArray()).Build();
            responseBuilder.SetSaveInfo(saveInfo);
        }

        public static List<string> FilterForSupportedHints(string[] hints)
        {
            return hints?.Where(h => IsValidHint(h)).ToList() ?? new List<string>();
        }

        public static bool IsValidHint(string hint)
        {
            switch(hint)
            {
                case View.AutofillHintCreditCardExpirationDate:
                case View.AutofillHintCreditCardExpirationDay:
                case View.AutofillHintCreditCardExpirationMonth:
                case View.AutofillHintCreditCardExpirationYear:
                case View.AutofillHintCreditCardNumber:
                case View.AutofillHintCreditCardSecurityCode:
                case View.AutofillHintEmailAddress:
                case View.AutofillHintPhone:
                case View.AutofillHintName:
                case View.AutofillHintPassword:
                case View.AutofillHintPostalAddress:
                case View.AutofillHintPostalCode:
                case View.AutofillHintUsername:
                    return true;
                default:
                    return false;
            }
        }
    }
}