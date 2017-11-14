using System;
using System.Collections.Generic;
using Android.Content;
using Android.Service.Autofill;
using Android.Views;
using Android.Widget;

namespace Bit.Android.Autofill
{
    public static class AutofillHelper
    {
        /**
         * Wraps autofill data in a LoginCredential  Dataset object which can then be sent back to the
         * client View.
         */
        public static Dataset NewDataset(Context context, AutofillFieldMetadataCollection autofillFields,
            FilledAutofillFieldCollection filledAutofillFieldCollection, bool datasetAuth)
        {
            var datasetName = filledAutofillFieldCollection.DatasetName;
            if(datasetName != null)
            {
                Dataset.Builder datasetBuilder;
                if(datasetAuth)
                {
                    datasetBuilder = new Dataset.Builder(
                        NewRemoteViews(context.PackageName, datasetName, "username", Resource.Drawable.fa_lock));
                    //IntentSender sender = AuthActivity.getAuthIntentSenderForDataset(context, datasetName);
                    //datasetBuilder.SetAuthentication(sender);
                }
                else
                {
                    datasetBuilder = new Dataset.Builder(
                        NewRemoteViews(context.PackageName, datasetName, "username", Resource.Drawable.user));
                }

                var setValueAtLeastOnce = filledAutofillFieldCollection.ApplyToFields(autofillFields, datasetBuilder);
                if(setValueAtLeastOnce)
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
        public static FillResponse NewResponse(Context context, bool datasetAuth,
            AutofillFieldMetadataCollection autofillFields,
            IDictionary<string, FilledAutofillFieldCollection> clientFormDataMap)
        {
            var responseBuilder = new FillResponse.Builder();
            if(clientFormDataMap != null)
            {
                foreach(var datasetName in clientFormDataMap.Keys)
                {
                    if(clientFormDataMap.ContainsKey(datasetName))
                    {
                        var dataset = NewDataset(context, autofillFields, clientFormDataMap[datasetName], datasetAuth);
                        if(dataset != null)
                        {
                            responseBuilder.AddDataset(dataset);
                        }
                    }
                }
            }

            if(autofillFields.SaveType != SaveDataType.Generic)
            {
                responseBuilder.SetSaveInfo(
                    new SaveInfo.Builder(autofillFields.SaveType, autofillFields.AutofillIds.ToArray()).Build());
                return responseBuilder.Build();
            }
            else
            {
                //Log.d(TAG, "These fields are not meant to be saved by autofill.");
                return null;
            }
        }

        public static string[] FilterForSupportedHints(string[] hints)
        {
            if((hints?.Length ?? 0) == 0)
            {
                return new string[0];
            }

            var filteredHints = new string[hints.Length];
            var i = 0;
            foreach(var hint in hints)
            {
                if(IsValidHint(hint))
                {
                    filteredHints[i++] = hint;
                }
            }

            var finalFilteredHints = new string[i];
            Array.Copy(filteredHints, 0, finalFilteredHints, 0, i);
            return finalFilteredHints;
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