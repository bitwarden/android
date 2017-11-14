using Android;
using Android.App;
using Android.OS;
using Android.Runtime;
using Android.Service.Autofill;
using Android.Views;
using System.Collections.Generic;
using System.Linq;

namespace Bit.Android.Autofill
{
    [Service(Permission = Manifest.Permission.BindAutofillService, Label = "bitwarden")]
    [IntentFilter(new string[] { "android.service.autofill.AutofillService" })]
    [MetaData("android.autofill", Resource = "@xml/autofillservice")]
    [Register("com.x8bit.bitwarden.Autofill.AutofillFrameworkService")]
    public class AutofillFrameworkService : global::Android.Service.Autofill.AutofillService
    {
        public override void OnFillRequest(FillRequest request, CancellationSignal cancellationSignal, FillCallback callback)
        {
            var structure = request.FillContexts?.LastOrDefault()?.Structure;
            if(structure == null)
            {
                return;
            }

            var clientState = request.ClientState;

            var parser = new StructureParser(structure);
            parser.ParseForFill();

            // build response
            var responseBuilder = new FillResponse.Builder();

            var username1 = new FilledAutofillField { TextValue = "username1" };
            var password1 = new FilledAutofillField { TextValue = "pass1" };
            var login1 = new Dictionary<string, FilledAutofillField>
            {
                { View.AutofillHintUsername, username1 },
                { View.AutofillHintPassword, password1 }
            };
            var coll = new FilledAutofillFieldCollection("Login 1 Name", login1);

            var username2 = new FilledAutofillField { TextValue = "username2" };
            var password2 = new FilledAutofillField { TextValue = "pass2" };
            var login2 = new Dictionary<string, FilledAutofillField>
            {
                { View.AutofillHintUsername, username2 },
                { View.AutofillHintPassword, password2 }
            };
            var col2 = new FilledAutofillFieldCollection("Login 2 Name", login2);

            var clientFormDataMap = new Dictionary<string, FilledAutofillFieldCollection>
            {
                { "login-1-guid", coll },
                { "login-2-guid", col2 }
            };

            var response = AutofillHelper.NewResponse(this, false, parser.AutofillFields, clientFormDataMap);
            // end build response

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

            var parser = new StructureParser(structure);
            parser.ParseForSave();
            var filledAutofillFieldCollection = parser.GetClientFormData();
            //SaveFilledAutofillFieldCollection(filledAutofillFieldCollection);
        }
    }
}
