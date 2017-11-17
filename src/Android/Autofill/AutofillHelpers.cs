using System;
using System.Collections.Generic;
using Android.Content;
using Android.Service.Autofill;
using Android.Views;
using Android.Widget;
using System.Linq;

namespace Bit.Android.Autofill
{
    public static class AutofillHelpers
    {
        public static FillResponse BuildFillResponse(Context context, bool auth, FieldCollection fields,
            IDictionary<string, IFilledItem> items)
        {
            var responseBuilder = new FillResponse.Builder();
            if(items != null)
            {
                foreach(var datasetName in items.Keys)
                {
                    var dataset = BuildDataset(context, fields, items[datasetName], auth);
                    if(dataset != null)
                    {
                        responseBuilder.AddDataset(dataset);
                    }
                }
            }

            var info = new SaveInfo.Builder(fields.SaveType, fields.AutofillIds.ToArray()).Build();
            responseBuilder.SetSaveInfo(info);
            return responseBuilder.Build();
        }

        public static Dataset BuildDataset(Context context, FieldCollection fields, IFilledItem filledItem, bool auth)
        {
            Dataset.Builder datasetBuilder;
            if(auth)
            {
                datasetBuilder = new Dataset.Builder(
                    BuildListView(context.PackageName, filledItem.Name, filledItem.Subtitle, Resource.Drawable.fa_lock));
                //IntentSender sender = AuthActivity.getAuthIntentSenderForDataset(context, datasetName);
                //datasetBuilder.SetAuthentication(sender);
            }
            else
            {
                datasetBuilder = new Dataset.Builder(
                    BuildListView(context.PackageName, filledItem.Name, filledItem.Subtitle, filledItem.Icon));
            }
            
            if(filledItem.ApplyToFields(fields, datasetBuilder))
            {
                return datasetBuilder.Build();
            }

            return null;
        }

        public static RemoteViews BuildListView(string packageName, string text, string subtext, int iconId)
        {
            var view = new RemoteViews(packageName, Resource.Layout.autofill_listitem);
            view.SetTextViewText(Resource.Id.text, text);
            view.SetTextViewText(Resource.Id.text2, subtext);
            view.SetImageViewResource(Resource.Id.icon, iconId);
            return view;
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