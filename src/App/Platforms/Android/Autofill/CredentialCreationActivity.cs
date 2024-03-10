using Android.App;
using Android.Content.PM;
using Android.OS;
using AndroidX.Credentials.Provider;
using Bit.App.Droid.Utilities;

namespace Bit.Droid.Autofill
{
    [Activity(
        NoHistory = true,
        LaunchMode = LaunchMode.SingleTop)]
    public class CredentialCreationActivity : MauiAppCompatActivity
    {
        protected override async void OnCreate(Bundle bundle)
        {
            Intent?.Validate();
            base.OnCreate(bundle);

            //TODO: Check if we have a Request, CallingRequest and origin and cancel if otherwise

            var getRequest = PendingIntentHandler.RetrieveProviderCreateCredentialRequest(Intent);

            await Bit.App.Platforms.Android.Autofill.CredentialHelpers.CreateCipherPasskeyAsync(getRequest, this);
        }
    }
}
