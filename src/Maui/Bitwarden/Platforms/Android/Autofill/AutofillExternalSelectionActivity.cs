using Android.App;
using Android.Content.PM;
using Android.OS;
using Bit.App.Droid.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;

namespace Bit.App.Droid.Autofill
{
    [Activity(
        NoHistory = true,
        LaunchMode = LaunchMode.SingleTop)]
    public class AutofillExternalSelectionActivity : MauiAppCompatActivity
    {
        protected override void OnCreate(Bundle bundle)
        {
            Intent?.Validate();
            base.OnCreate(bundle);

            var cipherId = Intent?.GetStringExtra(AutofillConstants.AutofillFrameworkCipherId);
            if (string.IsNullOrEmpty(cipherId))
            {
                SetResult(Result.Canceled);
                Finish();
                return;
            }

            GetCipherAndPerformAutofillAsync(cipherId).FireAndForget();
        }

        private async Task GetCipherAndPerformAutofillAsync(string cipherId)
        {
            var cipherService = ServiceContainer.Resolve<ICipherService>();
            var cipher = await cipherService.GetAsync(cipherId);
            var decCipher = await cipher.DecryptAsync();

            var autofillHandler = ServiceContainer.Resolve<IAutofillHandler>();
            autofillHandler.Autofill(decCipher);
        }
    }
}
