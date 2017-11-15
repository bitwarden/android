using System;
using System.Collections.Generic;
using Android.Content;
using Android.Service.Autofill;
using Android.Views;
using Android.Widget;
using System.Diagnostics;
using System.Linq;

namespace Bit.Android.Autofill
{
    public static class AutofillHelpers
    {
        /**
         * Wraps autofill data in a LoginCredential  Dataset object which can then be sent back to the
         * client View.
         */
        public static Dataset NewDataset(Context context, FieldCollection fields, IFilledItem filledItem, bool auth)
        {
            var itemName = filledItem.Name;
            if(itemName != null)
            {
                Dataset.Builder datasetBuilder;
                if(auth)
                {
                    datasetBuilder = new Dataset.Builder(
                        NewRemoteViews(context.PackageName, itemName, filledItem.Subtitle, Resource.Drawable.fa_lock));
                    //IntentSender sender = AuthActivity.getAuthIntentSenderForDataset(context, datasetName);
                    //datasetBuilder.SetAuthentication(sender);
                }
                else
                {
                    datasetBuilder = new Dataset.Builder(
                        NewRemoteViews(context.PackageName, itemName, filledItem.Subtitle, Resource.Drawable.user));
                }

                var setValue = filledItem.ApplyToFields(fields, datasetBuilder);
                if(setValue)
                {
                    return datasetBuilder.Build();
                }
            }

            return null;
        }

        public static RemoteViews NewRemoteViews(string packageName, string text, string subtext, int iconId)
        {
            var views = new RemoteViews(packageName, Resource.Layout.autofill_listitem);
            views.SetTextViewText(Resource.Id.text, text);
            views.SetTextViewText(Resource.Id.text2, subtext);
            views.SetImageViewResource(Resource.Id.icon, iconId);
            return views;
        }

        /**
         * Wraps autofill data in a Response object (essentially a series of Datasets) which can then
         * be sent back to the client View.
         */
        public static FillResponse NewResponse(Context context, bool auth, FieldCollection fields,
            IDictionary<string, IFilledItem> items)
        {
            var responseBuilder = new FillResponse.Builder();
            if(items != null)
            {
                foreach(var datasetName in items.Keys)
                {
                    var dataset = NewDataset(context, fields, items[datasetName], auth);
                    if(dataset != null)
                    {
                        responseBuilder.AddDataset(dataset);
                    }
                }
            }

            if(true || fields.SaveType != SaveDataType.Generic)
            {
                var info = new SaveInfo.Builder(fields.SaveType, fields.AutofillIds.ToArray()).Build();
                responseBuilder.SetSaveInfo(info);
                return responseBuilder.Build();
            }
            else
            {
                Debug.WriteLine("These fields are not meant to be saved by autofill.");
                return null;
            }
        }

        public static List<string> FilterForSupportedHints(string[] hints)
        {
            if(hints == null)
            {
                return new List<string>();
            }

            return hints.Where(h => IsValidHint(h)).ToList();
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