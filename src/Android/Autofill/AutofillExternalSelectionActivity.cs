using Android.App;
using Android.Content.PM;
using Android.OS;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Bit.Droid.Utilities;

namespace Bit.Droid.Autofill
{
    [Activity(
        NoHistory = true,
        LaunchMode = LaunchMode.SingleTop)]
    public class AutofillExternalSelectionActivity : Xamarin.Forms.Platform.Android.FormsAppCompatActivity
    {
        protected override void OnCreate(Bundle bundle)
        {
            Intent?.Validate();
            base.OnCreate(bundle);

            var cipherId = Intent?.GetStringExtra("autofillCipherId");
            if (string.IsNullOrEmpty(cipherId))
            {
                SetResult(Result.Canceled);
                Finish();
                return;
            }

            var cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            var cipher = cipherService.GetAsync(cipherId).GetAwaiter().GetResult();
            var decCipher = cipher?.DecryptAsync().GetAwaiter().GetResult();

            var autofillHandler = ServiceContainer.Resolve<IAutofillHandler>();
            autofillHandler.Autofill(decCipher, true);
        }
    }
}
