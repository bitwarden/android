using System.Threading.Tasks;
using Android.App;
using Android.Content.PM;
using Android.OS;
using AndroidX.Credentials.Provider;
using AndroidX.Credentials.WebAuthn;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Bit.App.Droid.Utilities;

namespace Bit.Droid.Autofill
{
    [Activity(
        NoHistory = true,
        LaunchMode = LaunchMode.SingleTop)]
    public class CredentialProviderSelectionActivity : MauiAppCompatActivity
    {
        protected override void OnCreate(Bundle bundle)
        {
            Intent?.Validate();
            base.OnCreate(bundle);

            var cipherId = Intent?.GetStringExtra(CredentialProviderConstants.CredentialProviderCipherId);
            if (string.IsNullOrEmpty(cipherId))
            {
                SetResult(Result.Canceled);
                Finish();
                return;
            }

            GetCipherAndPerformPasskeyAuthAsync(cipherId).FireAndForget();
        }

        private async Task GetCipherAndPerformPasskeyAuthAsync(string cipherId)
        {
            // TODO this is a work in progress
            // https://developer.android.com/training/sign-in/credential-provider#passkeys-implement

            var getRequest = PendingIntentHandler.RetrieveProviderGetCredentialRequest(Intent);
            // var publicKeyRequest = getRequest?.CredentialOptions as PublicKeyCredentialRequestOptions;

            var requestInfo = Intent.GetBundleExtra(CredentialProviderConstants.CredentialDataIntentExtra);
            var credIdEnc = requestInfo?.GetString(CredentialProviderConstants.CredentialIdIntentExtra);

            var cipherService = ServiceContainer.Resolve<ICipherService>();
            var cipher = await cipherService.GetAsync(cipherId);
            var decCipher = await cipher.DecryptAsync();

            var passkey = decCipher.Login.Fido2Credentials.Find(f => f.CredentialId == credIdEnc);

            var credId = Convert.FromBase64String(credIdEnc);
            // var privateKey = Convert.FromBase64String(passkey.PrivateKey);
            // var uid = Convert.FromBase64String(passkey.uid);

            var origin = getRequest?.CallingAppInfo.Origin;
            var packageName = getRequest?.CallingAppInfo.PackageName;

            // --- continue WIP here (save TOTP copy as last step) ---

            // Copy TOTP if needed
            var autofillHandler = ServiceContainer.Resolve<IAutofillHandler>();
            autofillHandler.Autofill(decCipher);
        }
    }
}
