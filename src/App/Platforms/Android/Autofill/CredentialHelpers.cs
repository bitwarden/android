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
using Org.Json;
using Activity = Android.App.Activity;
using System.Text.Json;
using System.Text.Json.Nodes;

namespace Bit.App.Platforms.Android.Autofill
{
    public static class CredentialHelpers
    {
        public static async Task<List<CredentialEntry>> PopulatePasskeyDataAsync(CallingAppInfo callingAppInfo,
            BeginGetPublicKeyCredentialOption option, Context context)
        {
            var passkeyEntries = new List<CredentialEntry>();

            var requestOptions = new PublicKeyCredentialRequestOptions(option.RequestJson);

            var authenticator = Bit.Core.Utilities.ServiceContainer.Resolve<IFido2AuthenticatorService>();
            var credentials = await authenticator.SilentCredentialDiscoveryAsync(requestOptions.RpId);

            passkeyEntries = credentials.Select(credential => MapCredential(credential, option, context) as CredentialEntry).ToList();

            return passkeyEntries;
        }

        private static PublicKeyCredentialEntry MapCredential(Fido2AuthenticatorDiscoverableCredentialMetadata credential, BeginGetPublicKeyCredentialOption option, Context context)
        {
            var credDataBundle = new Bundle();
            credDataBundle.PutByteArray(Bit.Droid.Autofill.CredentialProviderConstants.CredentialIdIntentExtra, credential.Id);

            var intent = new Intent(context, typeof(Bit.Droid.Autofill.CredentialProviderSelectionActivity))
                .SetAction(Bit.Droid.Autofill.CredentialProviderService.GetPasskeyIntentAction).SetPackage(Constants.PACKAGE_NAME);
            intent.PutExtra(Bit.Droid.Autofill.CredentialProviderConstants.CredentialDataIntentExtra, credDataBundle);
            intent.PutExtra(Bit.Droid.Autofill.CredentialProviderConstants.CredentialProviderCipherId, "unknown");
            var pendingIntent = PendingIntent.GetActivity(context, Bit.Droid.Autofill.CredentialProviderService.UniqueGetRequestCode, intent,
                PendingIntentFlags.Mutable | PendingIntentFlags.UpdateCurrent);

            return new PublicKeyCredentialEntry.Builder(
                        context,
                        credential.UserName ?? "No username",
                        pendingIntent,
                        option)
                    .SetDisplayName(credential.UserName ?? "No username")
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

            var transportsArray = new JSONArray();
            if (clientCreateCredentialResult.Transports != null)
            {
                foreach (var transport in clientCreateCredentialResult.Transports)
                {
                    transportsArray.Put(transport);
                }
            }
            
            var responseInnerAndroidJson = new JSONObject();
            responseInnerAndroidJson.Put("clientDataJSON", b64Encode(clientCreateCredentialResult.ClientDataJSON));
            responseInnerAndroidJson.Put("authenticatorData", b64Encode(clientCreateCredentialResult.AuthData));
            responseInnerAndroidJson.Put("attestationObject", b64Encode(clientCreateCredentialResult.AttestationObject));
            responseInnerAndroidJson.Put("transports", transportsArray);
            responseInnerAndroidJson.Put("publicKeyAlgorithm", clientCreateCredentialResult.PublicKeyAlgorithm);
            responseInnerAndroidJson.Put("publicKey", b64Encode(clientCreateCredentialResult.PublicKey));

            var rootAndroidJson = new JSONObject();
            rootAndroidJson.Put("id", b64Encode(clientCreateCredentialResult.CredentialId));
            rootAndroidJson.Put("rawId", b64Encode(clientCreateCredentialResult.CredentialId));
            rootAndroidJson.Put("authenticatorAttachment", "platform");
            rootAndroidJson.Put("type", "public-key");
            rootAndroidJson.Put("clientExtensionResults", new JSONObject());
            rootAndroidJson.Put("response", responseInnerAndroidJson);

            var responseAndroidJson = rootAndroidJson.ToString();

            System.Diagnostics.Debug.WriteLine(responseAndroidJson);

            var result = new Intent();
            var publicKeyResponse = new CreatePublicKeyCredentialResponse(responseAndroidJson);
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
