using System;
using System.Collections.Generic;
using Android.Content;
using Android.Service.Autofill;
using Android.Widget;
using System.Linq;
using Android.App;
using Bit.App.Abstractions;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.App.Enums;

namespace Bit.Android.Autofill
{
    public static class AutofillHelpers
    {
        public static async Task<List<FilledItem>> GetFillItemsAsync(Parser parser, ICipherService service)
        {
            var items = new List<FilledItem>();

            if(parser.FieldCollection.FillableForLogin)
            {
                var ciphers = await service.GetAllAsync(parser.Uri);
                if(ciphers.Item1.Any() || ciphers.Item2.Any())
                {
                    var allCiphers = ciphers.Item1.ToList();
                    allCiphers.AddRange(ciphers.Item2.ToList());
                    foreach(var cipher in allCiphers)
                    {
                        items.Add(new FilledItem(cipher));
                    }
                }
            }
            else if(parser.FieldCollection.FillableForCard)
            {
                var ciphers = await service.GetAllAsync();
                foreach(var cipher in ciphers.Where(c => c.Type == CipherType.Card))
                {
                    items.Add(new FilledItem(cipher));
                }
            }

            return items;
        }

        public static FillResponse BuildFillResponse(Context context, Parser parser, List<FilledItem> items)
        {
            var responseBuilder = new FillResponse.Builder();
            if(items != null && items.Count > 0)
            {
                foreach(var item in items)
                {
                    var dataset = BuildDataset(context, parser.FieldCollection, item);
                    if(dataset != null)
                    {
                        responseBuilder.AddDataset(dataset);
                    }
                }
            }

            AddSaveInfo(responseBuilder, parser.FieldCollection);
            responseBuilder.SetIgnoredIds(parser.FieldCollection.IgnoreAutofillIds.ToArray());
            return responseBuilder.Build();
        }

        public static Dataset BuildDataset(Context context, FieldCollection fields, FilledItem filledItem)
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
            var view = BuildListView(context.PackageName, AppResources.AutofillWithBitwarden,
                AppResources.VaultIsLocked, Resource.Drawable.icon);
            var intent = new Intent(context, typeof(MainActivity));
            intent.PutExtra("autofillFramework", true);
            if(fields.FillableForLogin)
            {
                intent.PutExtra("autofillFrameworkFillType", (int)CipherType.Login);
            }
            else if(fields.FillableForCard)
            {
                intent.PutExtra("autofillFrameworkFillType", (int)CipherType.Card);
            }
            else if(fields.FillableForIdentity)
            {
                intent.PutExtra("autofillFrameworkFillType", (int)CipherType.Identity);
            }
            else
            {
                return null;
            }
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
            var requiredIds = fields.GetRequiredSaveFields();
            if(fields.SaveType == SaveDataType.Generic || requiredIds.Length == 0)
            {
                return;
            }

            var saveBuilder = new SaveInfo.Builder(fields.SaveType, requiredIds);
            var optionalIds = fields.GetOptionalSaveIds();
            if(optionalIds.Length > 0)
            {
                saveBuilder.SetOptionalIds(optionalIds);
            }
            responseBuilder.SetSaveInfo(saveBuilder.Build());
        }
    }
}