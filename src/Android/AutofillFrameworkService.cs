using Android;
using Android.App;
using Android.OS;
using Android.Runtime;
using Android.Service.Autofill;
using Android.Util;

namespace Bit.Android
{
    [Service(Permission = Manifest.Permission.BindAutofillService, Label = "bitwarden")]
    [IntentFilter(new string[] { "android.service.autofill.AutofillService" })]
    [MetaData("android.autofill", Resource = "@xml/autofillservice")]
    [Register("com.x8bit.bitwarden.AutofillFrameworkService")]
    public class AutofillFrameworkService : global::Android.Service.Autofill.AutofillService
    {
        public override void OnFillRequest(FillRequest request, CancellationSignal cancellationSignal, FillCallback callback)
        {
            var structure = request.FillContexts[request.FillContexts.Count - 1].Structure;
            var data = request.ClientState;
        }

        public override void OnSaveRequest(SaveRequest request, SaveCallback callback)
        {
            var context = request.FillContexts;
            var structure = context[context.Count - 1].Structure;
            var data = request.ClientState;
            
        }
    }
}
