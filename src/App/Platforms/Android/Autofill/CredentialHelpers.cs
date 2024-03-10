using Android.App;
using Android.Content;
using Android.OS;
using Android.Util;
using Bit.Core.Abstractions;
using Bit.Droid;
using Drawables = Android.Graphics.Drawables;
using AndroidX.Credentials.Provider;
using AndroidX.Credentials.WebAuthn;
using AndroidX.Credentials;
using Bit.Core.Utilities;
using Java.Security;
using Activity = Android.App.Activity;

namespace Bit.App.Platforms.Android.Autofill
{
    public static class CredentialHelpers
    {
        public static async Task<List<CredentialEntry>> PopulatePasskeyDataAsync(CallingAppInfo callingAppInfo,
            BeginGetPublicKeyCredentialOption option, Context context)
        {
            var origin = callingAppInfo.Origin;
            var passkeyEntries = new List<CredentialEntry>();

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

        private static PublicKeyCredentialEntry GetPasskey(Bit.Core.Models.View.CipherView cipher, BeginGetPublicKeyCredentialOption option, Context context)
        {
            var credDataBundle = new Bundle();
            credDataBundle.PutString(Bit.Droid.Autofill.CredentialProviderConstants.CredentialIdIntentExtra,
                cipher.Login.MainFido2Credential.CredentialId);

            var intent = new Intent(context, typeof(Bit.Droid.Autofill.CredentialProviderSelectionActivity))
                .SetAction(Bit.Droid.Autofill.CredentialProviderService.GetPasskeyIntentAction).SetPackage(Constants.PACKAGE_NAME);
            intent.PutExtra(Bit.Droid.Autofill.CredentialProviderConstants.CredentialDataIntentExtra, credDataBundle);
            intent.PutExtra(Bit.Droid.Autofill.CredentialProviderConstants.CredentialProviderCipherId, cipher.Id);
            var pendingIntent = PendingIntent.GetActivity(context, Bit.Droid.Autofill.CredentialProviderService.UniqueGetRequestCode, intent,
                PendingIntentFlags.Mutable | PendingIntentFlags.UpdateCurrent);

            return new PublicKeyCredentialEntry.Builder(
                    context,
                    cipher.Login.Username ?? "No username",
                    pendingIntent,
                    option)
                .SetDisplayName(cipher.Name)
                .SetIcon(Drawables.Icon.CreateWithResource(context, Microsoft.Maui.Resource.Drawable.icon))
                .Build();
        }

        public static async Task CreateCipherPasskeyAsync(ProviderCreateCredentialRequest getRequest, Activity activity)
        {
            var callingRequest = getRequest?.CallingRequest as CreatePublicKeyCredentialRequest;

            var origin = callingRequest.Origin;
            var androidOrigin = AppInfoToOrigin(getRequest?.CallingAppInfo);
            var packageName = getRequest?.CallingAppInfo.PackageName;

            var credentialCreationOptions = new PublicKeyCredentialCreationOptions(callingRequest.RequestJson);

            var rp = new Core.Utilities.Fido2.PublicKeyCredentialRpEntity()
            {
                Id = credentialCreationOptions.Rp.Id,
                Name = credentialCreationOptions.Rp.Name
            };

            var user = new Core.Utilities.Fido2.PublicKeyCredentialUserEntity()
            {
                Id = credentialCreationOptions.User.GetId(),
                Name = credentialCreationOptions.User.Name,
                DisplayName = credentialCreationOptions.User.DisplayName,
                //Icon = //TODO: Is Icon needed?
            };

            var pubKeyCredParams = new List<Core.Utilities.Fido2.PublicKeyCredentialParameters>();
            foreach (var pubKeyCredParam in credentialCreationOptions.PubKeyCredParams)
            {
                pubKeyCredParams.Add(new Core.Utilities.Fido2.PublicKeyCredentialParameters() { Alg = Convert.ToInt32(pubKeyCredParam.Alg), Type = pubKeyCredParam.Type }); //TODO: Can we assume Alg is never outside int range?
            }

            var excludeCredentials = new List<Core.Utilities.Fido2.PublicKeyCredentialDescriptor>();
            foreach (var excludeCred in credentialCreationOptions.ExcludeCredentials)
            {
                excludeCredentials.Add(new Core.Utilities.Fido2.PublicKeyCredentialDescriptor(){ Id = excludeCred.GetId(), Type = excludeCred.Type, Transports = excludeCred.Transports.ToArray() });
            }

            var authenticatorSelection = new Core.Utilities.Fido2.AuthenticatorSelectionCriteria()
            {
                UserVerification = credentialCreationOptions.AuthenticatorSelection.UserVerification,
                ResidentKey = credentialCreationOptions.AuthenticatorSelection.ResidentKey,
                RequireResidentKey = credentialCreationOptions.AuthenticatorSelection.RequireResidentKey
            };

            //TODO: Change to something else or handle overflow exception?
            var timeout = Convert.ToInt32(credentialCreationOptions.Timeout);
            
            var credentialCreateParams = new Bit.Core.Utilities.Fido2.Fido2ClientCreateCredentialParams()
            {
                Challenge = credentialCreationOptions.GetChallenge(),
                Origin = origin,
                PubKeyCredParams = pubKeyCredParams.ToArray(),
                Rp = rp,
                User = user,
                Timeout = timeout,
                Attestation = credentialCreationOptions.Attestation,
                AuthenticatorSelection = authenticatorSelection,
                ExcludeCredentials = excludeCredentials.ToArray(),
                //Extensions = //TODO: Do we need to handle Extensions?
                SameOriginWithAncestors = true //TODO: Where to get value? Or logic to set?
            };

            var fido2ClientService = ServiceContainer.Resolve<IFido2ClientService>();
            var clientCreateCredentialResult = await fido2ClientService.CreateCredentialAsync(credentialCreateParams);
            if (clientCreateCredentialResult == null)
            {
                //TODO: Cancel process?
            }

            var response = new AuthenticatorAttestationResponse
            (
                credentialCreationOptions,
                clientCreateCredentialResult.CredentialId,
                clientCreateCredentialResult.PublicKey,
                androidOrigin,
                true,
                true,
                true,
                true,
                packageName = packageName,
                clientCreateCredentialResult.ClientDataJSON
            );
            response.SetAttestationObject(clientCreateCredentialResult.AttestationObject);
            
            var credential = new FidoPublicKeyCredential
            (
                clientCreateCredentialResult.CredentialId,
                response,
                "platform"
            );

            var result = new Intent();
            var credentialJson = credential.Json();
            System.Diagnostics.Debug.WriteLine(credentialJson);

            var publicKeyResponse = new CreatePublicKeyCredentialResponse(credentialJson);
            PendingIntentHandler.SetCreateCredentialResponse(result, publicKeyResponse);

            activity.SetResult(Result.Ok, result);
            activity.Finish();
        }

        //TODO: To Delete if not needed
        private static string AppInfoToOrigin(CallingAppInfo info)
        {
            var cert = info.SigningInfo.GetApkContentsSigners()[0].ToByteArray();
            var md = MessageDigest.GetInstance("SHA-256");
            var certHash = md.Digest(cert);
            return $"android:apk-key-hash:${b64Encode(certHash)}";
        }

        //TODO: To Delete if not needed
        private static string b64Encode(byte[] data)
        {
            return Base64.EncodeToString(data, Base64Flags.NoPadding | Base64Flags.NoWrap | Base64Flags.UrlSafe);
        }
    }
}
