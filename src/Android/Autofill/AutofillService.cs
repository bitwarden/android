using Android;
using Android.App;
using Android.OS;
using Android.Runtime;
using Android.Service.Autofill;
using Android.Views;
using Bit.App.Abstractions;
using System.Collections.Generic;
using System.Linq;
using XLabs.Ioc;

namespace Bit.Android.Autofill
{
    [Service(Permission = Manifest.Permission.BindAutofillService, Label = "bitwarden")]
    [IntentFilter(new string[] { "android.service.autofill.AutofillService" })]
    [MetaData("android.autofill", Resource = "@xml/autofillservice")]
    [Register("com.x8bit.bitwarden.Autofill.AutofillService")]
    public class AutofillService : global::Android.Service.Autofill.AutofillService
    {
        private ICipherService _cipherService;

        public async override void OnFillRequest(FillRequest request, CancellationSignal cancellationSignal, FillCallback callback)
        {
            var structure = request.FillContexts?.LastOrDefault()?.Structure;
            if(structure == null)
            {
                return;
            }

            var clientState = request.ClientState;

            var parser = new Parser(structure);
            parser.ParseForFill();

            if(!parser.FieldCollection.Fields.Any() || string.IsNullOrWhiteSpace(parser.Uri))
            {
                return;
            }

            if(_cipherService == null)
            {
                _cipherService = Resolver.Resolve<ICipherService>();
            }

            // build response
            var items = new Dictionary<string, IFilledItem>();
            var ciphers = await _cipherService.GetAllAsync(parser.Uri);
            if(ciphers.Item1.Any() || ciphers.Item2.Any())
            {
                var allCiphers = ciphers.Item1.ToList();
                allCiphers.AddRange(ciphers.Item2.ToList());
                foreach(var cipher in allCiphers)
                {
                    items.Add(cipher.Id, new CipherFilledItem(cipher));
                }
            }

            if(!items.Any())
            {
                return;
            }

            var response = AutofillHelpers.NewResponse(this, false, parser.FieldCollection, items);
            callback.OnSuccess(response);
        }

        public override void OnSaveRequest(SaveRequest request, SaveCallback callback)
        {
            var structure = request.FillContexts?.LastOrDefault()?.Structure;
            if(structure == null)
            {
                return;
            }

            var clientState = request.ClientState;

            var parser = new Parser(structure);
            parser.ParseForSave();
            var filledAutofillFieldCollection = parser.GetClientFormData();
            //SaveFilledAutofillFieldCollection(filledAutofillFieldCollection);
        }
    }
}
