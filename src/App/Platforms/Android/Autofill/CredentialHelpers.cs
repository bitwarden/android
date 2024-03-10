using Android.App;
using Android.Content;
using Android.OS;
using Bit.Core.Abstractions;
using Bit.Droid;
using Bit.Droid.Autofill;
using Drawables = Android.Graphics.Drawables;

namespace Bit.App.Platforms.Android.Autofill
{
    public static class CredentialHelpers
    {
        public static async Task<List<AndroidX.Credentials.Provider.CredentialEntry>> PopulatePasskeyDataAsync(AndroidX.Credentials.Provider.CallingAppInfo callingAppInfo,
            AndroidX.Credentials.Provider.BeginGetPublicKeyCredentialOption option, Context context)
        {
            var origin = callingAppInfo.Origin;
            var passkeyEntries = new List<AndroidX.Credentials.Provider.CredentialEntry>();

            var cipherService = Bit.Core.Utilities.ServiceContainer.Resolve<ICipherService>();
            var ciphers = await cipherService.GetAllDecryptedForUrlAsync(origin);
            if (ciphers == null)
            {
                return passkeyEntries;
            }

            var passkeyCiphers = ciphers.Where(cipher => cipher.HasFido2Credential).ToList();
            if (!passkeyCiphers.Any())
            {
                return passkeyEntries;
            }

            foreach (var cipher in passkeyCiphers)
            {
                var passkeyEntry = GetPasskey(cipher, option, context);
                passkeyEntries.Add(passkeyEntry);
            }

            return passkeyEntries;
        }

        private static AndroidX.Credentials.Provider.PublicKeyCredentialEntry GetPasskey(Bit.Core.Models.View.CipherView cipher, AndroidX.Credentials.Provider.BeginGetPublicKeyCredentialOption option, Context context)
        {
            var credDataBundle = new Bundle();
            credDataBundle.PutString(Bit.Droid.Autofill.CredentialProviderConstants.CredentialIdIntentExtra,
                cipher.Login.MainFido2Credential.CredentialId);

            var intent = new Intent(context, typeof(Bit.Droid.Autofill.CredentialProviderSelectionActivity))
                .SetAction(CredentialProviderService.GetPasskeyIntentAction).SetPackage(Constants.PACKAGE_NAME);
            intent.PutExtra(Bit.Droid.Autofill.CredentialProviderConstants.CredentialDataIntentExtra, credDataBundle);
            intent.PutExtra(Bit.Droid.Autofill.CredentialProviderConstants.CredentialProviderCipherId, cipher.Id);
            var pendingIntent = PendingIntent.GetActivity(context, CredentialProviderService.UniqueGetRequestCode, intent,
                PendingIntentFlags.Mutable | PendingIntentFlags.UpdateCurrent);

            return new AndroidX.Credentials.Provider.PublicKeyCredentialEntry.Builder(
                    context,
                    cipher.Login.Username ?? "No username",
                    pendingIntent,
                    option)
                .SetDisplayName(cipher.Name)
                .SetIcon(Drawables.Icon.CreateWithResource(context, Microsoft.Maui.Resource.Drawable.icon))
                .Build();
        }
    }
}
