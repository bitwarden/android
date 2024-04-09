using Android.App;
using Android.Content;
using Android.OS;
using AndroidX.Credentials;
using AndroidX.Credentials.Exceptions;
using AndroidX.Credentials.Provider;
using AndroidX.Credentials.WebAuthn;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Bit.Droid;
using Org.Json;
using Activity = Android.App.Activity;
using Drawables = Android.Graphics.Drawables;

namespace Bit.App.Platforms.Android.Autofill
{
    public static class CredentialHelpers
    {
        public static async Task<List<CredentialEntry>> PopulatePasskeyDataAsync(CallingAppInfo callingAppInfo,
            BeginGetPublicKeyCredentialOption option, Context context, bool hasVaultBeenUnlockedInThisTransaction)
        {
            var passkeyEntries = new List<CredentialEntry>();
            var requestOptions = new PublicKeyCredentialRequestOptions(option.RequestJson);

            var authenticator = Bit.Core.Utilities.ServiceContainer.Resolve<IFido2AuthenticatorService>();
            var credentials = await authenticator.SilentCredentialDiscoveryAsync(requestOptions.RpId);

            passkeyEntries = credentials.Select(credential => MapCredential(credential, option, context, hasVaultBeenUnlockedInThisTransaction) as CredentialEntry).ToList();

            return passkeyEntries;
        }

        private static PublicKeyCredentialEntry MapCredential(Fido2AuthenticatorDiscoverableCredentialMetadata credential, BeginGetPublicKeyCredentialOption option, Context context, bool hasVaultBeenUnlockedInThisTransaction)
        {
            var credDataBundle = new Bundle();
            credDataBundle.PutByteArray(Bit.Core.Utilities.Fido2.CredentialProviderConstants.CredentialIdIntentExtra, credential.Id);

            var intent = new Intent(context, typeof(Bit.Droid.Autofill.CredentialProviderSelectionActivity))
                .SetAction(Bit.Droid.Autofill.CredentialProviderService.GetFido2IntentAction).SetPackage(Constants.PACKAGE_NAME);
            intent.PutExtra(Bit.Core.Utilities.Fido2.CredentialProviderConstants.CredentialDataIntentExtra, credDataBundle);
            intent.PutExtra(Bit.Core.Utilities.Fido2.CredentialProviderConstants.CredentialProviderCipherId, credential.CipherId);
            intent.PutExtra(Bit.Core.Utilities.Fido2.CredentialProviderConstants.CredentialHasVaultBeenUnlockedInThisTransactionExtra, hasVaultBeenUnlockedInThisTransaction);
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
                DisplayName = credentialCreationOptions.User.DisplayName
            };

            var pubKeyCredParams = new List<Core.Utilities.Fido2.PublicKeyCredentialParameters>();
            foreach (var pubKeyCredParam in credentialCreationOptions.PubKeyCredParams)
            {
                pubKeyCredParams.Add(new Core.Utilities.Fido2.PublicKeyCredentialParameters() { Alg = Convert.ToInt32(pubKeyCredParam.Alg), Type = pubKeyCredParam.Type });
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
                //Extensions = // Can be improved later to add support for 'credProps'
                SameOriginWithAncestors = true
            };

            var fido2MediatorService = ServiceContainer.Resolve<IFido2MediatorService>();
            var clientCreateCredentialResult = await fido2MediatorService.CreateCredentialAsync(credentialCreateParams);
            if (clientCreateCredentialResult == null)
            {
                var resultErrorIntent = new Intent();
                PendingIntentHandler.SetCreateCredentialException(resultErrorIntent, new CreateCredentialUnknownException());
                activity.SetResult(Result.Ok, resultErrorIntent);
                activity.Finish();
                return;
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
            responseInnerAndroidJson.Put("clientDataJSON", CoreHelpers.Base64UrlEncode(clientCreateCredentialResult.ClientDataJSON));
            responseInnerAndroidJson.Put("authenticatorData", CoreHelpers.Base64UrlEncode(clientCreateCredentialResult.AuthData));
            responseInnerAndroidJson.Put("attestationObject", CoreHelpers.Base64UrlEncode(clientCreateCredentialResult.AttestationObject));
            responseInnerAndroidJson.Put("transports", transportsArray);
            responseInnerAndroidJson.Put("publicKeyAlgorithm", clientCreateCredentialResult.PublicKeyAlgorithm);
            responseInnerAndroidJson.Put("publicKey", CoreHelpers.Base64UrlEncode(clientCreateCredentialResult.PublicKey));

            var rootAndroidJson = new JSONObject();
            rootAndroidJson.Put("id", CoreHelpers.Base64UrlEncode(clientCreateCredentialResult.CredentialId));
            rootAndroidJson.Put("rawId", CoreHelpers.Base64UrlEncode(clientCreateCredentialResult.CredentialId));
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
    }
}
