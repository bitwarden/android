using System.ComponentModel.DataAnnotations;
using System.Text.Json.Nodes;
using Android.App;
using Android.Content;
using Android.OS;
using AndroidX.Credentials;
using AndroidX.Credentials.Exceptions;
using AndroidX.Credentials.Provider;
using AndroidX.Credentials.WebAuthn;
using Bit.App.Abstractions;
using Bit.App.Droid.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.Core.Utilities.Fido2;
using Bit.Core.Utilities.Fido2.Extensions;
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

            // We need to change the request code for every pending intent on mapping the credential so the extras are not overriten by the last
            // credential entry created.
            int requestCodeAddition = 0;
            passkeyEntries = credentials.Select(credential => MapCredential(credential, option, context, hasVaultBeenUnlockedInThisTransaction, Bit.Droid.Autofill.CredentialProviderService.UniqueGetRequestCode + requestCodeAddition++) as CredentialEntry).ToList();

            return passkeyEntries;
        }

        private static PublicKeyCredentialEntry MapCredential(Fido2AuthenticatorDiscoverableCredentialMetadata credential, BeginGetPublicKeyCredentialOption option, Context context, bool hasVaultBeenUnlockedInThisTransaction, int requestCode)
        {
            var credDataBundle = new Bundle();
            credDataBundle.PutByteArray(Bit.Core.Utilities.Fido2.CredentialProviderConstants.CredentialIdIntentExtra, credential.Id);

            var intent = new Intent(context, typeof(Bit.Droid.Autofill.CredentialProviderSelectionActivity))
                .SetAction(Bit.Droid.Autofill.CredentialProviderService.GetFido2IntentAction).SetPackage(Constants.PACKAGE_NAME);
            intent.PutExtra(Bit.Core.Utilities.Fido2.CredentialProviderConstants.CredentialDataIntentExtra, credDataBundle);
            intent.PutExtra(Bit.Core.Utilities.Fido2.CredentialProviderConstants.CredentialProviderCipherId, credential.CipherId);
            intent.PutExtra(Bit.Core.Utilities.Fido2.CredentialProviderConstants.CredentialHasVaultBeenUnlockedInThisTransactionExtra, hasVaultBeenUnlockedInThisTransaction);
            var pendingIntent = PendingIntent.GetActivity(context, requestCode, intent,
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

        private static PublicKeyCredentialCreationOptions GetPublicKeyCredentialCreationOptionsFromJson(string json)
        {
            var request = new PublicKeyCredentialCreationOptions(json);
            var jsonObj = new JSONObject(json);
            var authenticatorSelection = jsonObj.GetJSONObject("authenticatorSelection");
            request.AuthenticatorSelection = new AndroidX.Credentials.WebAuthn.AuthenticatorSelectionCriteria(
                authenticatorSelection.OptString("authenticatorAttachment", "platform"),
                authenticatorSelection.OptString("residentKey", null),
                authenticatorSelection.OptBoolean("requireResidentKey", false),
                authenticatorSelection.OptString("userVerification", "preferred"));

            return request;
        }

        public static async Task CreateCipherPasskeyAsync(ProviderCreateCredentialRequest getRequest, Activity activity)
        {
            var callingRequest = getRequest?.CallingRequest as CreatePublicKeyCredentialRequest;

            if (callingRequest is null)
            {
                await DisplayAlertAsync(AppResources.AnErrorHasOccurred, string.Empty);
                FailAndFinish();
                return;
            }

            var credentialCreationOptions = GetPublicKeyCredentialCreationOptionsFromJson(callingRequest.RequestJson);
            string origin;
            try
            {
                origin = await ValidateCallingAppInfoAndGetOriginAsync(getRequest.CallingAppInfo, credentialCreationOptions.Rp.Id);
            }
            catch (Core.Exceptions.ValidationException valEx)
            {
                await DisplayAlertAsync(AppResources.AnErrorHasOccurred, valEx.Message);
                FailAndFinish();
                return;
            }

            if (origin is null)
            {
                await DisplayAlertAsync(AppResources.ErrorCreatingPasskey, AppResources.PasskeysNotSupportedForThisApp);
                FailAndFinish();
                return;
            }

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
                excludeCredentials.Add(new Core.Utilities.Fido2.PublicKeyCredentialDescriptor() { Id = excludeCred.GetId(), Type = excludeCred.Type, Transports = excludeCred.Transports.ToArray() });
            }

            var authenticatorSelection = new Core.Utilities.Fido2.AuthenticatorSelectionCriteria()
            {
                UserVerification = credentialCreationOptions.AuthenticatorSelection.UserVerification,
                ResidentKey = credentialCreationOptions.AuthenticatorSelection.ResidentKey,
                RequireResidentKey = credentialCreationOptions.AuthenticatorSelection.RequireResidentKey
            };

            var timeout = Convert.ToInt32(credentialCreationOptions.Timeout);

            var credentialCreateParams = new Fido2ClientCreateCredentialParams()
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
                Extensions = MapExtensionsFromJson(credentialCreationOptions),
                SameOriginWithAncestors = true
            };

            var credentialExtraCreateParams = new Fido2ExtraCreateCredentialParams
            (
                callingRequest.GetClientDataHash(),
                getRequest.CallingAppInfo?.PackageName
            );

            var fido2MediatorService = ServiceContainer.Resolve<IFido2MediatorService>();
            var clientCreateCredentialResult = await fido2MediatorService.CreateCredentialAsync(credentialCreateParams, credentialExtraCreateParams);
            if (clientCreateCredentialResult == null)
            {
                FailAndFinish();
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
            if (clientCreateCredentialResult.ClientDataJSON != null)
            {
                responseInnerAndroidJson.Put("clientDataJSON", CoreHelpers.Base64UrlEncode(clientCreateCredentialResult.ClientDataJSON));
            }
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
            rootAndroidJson.Put("clientExtensionResults", MapExtensionsToJson(clientCreateCredentialResult.Extensions));
            rootAndroidJson.Put("response", responseInnerAndroidJson);

            var result = new Intent();
            var publicKeyResponse = new CreatePublicKeyCredentialResponse(rootAndroidJson.ToString());
            PendingIntentHandler.SetCreateCredentialResponse(result, publicKeyResponse);

            activity.SetResult(Result.Ok, result);
            activity.Finish();

            async Task DisplayAlertAsync(string title, string message)
            {
                if (ServiceContainer.TryResolve<IDeviceActionService>(out var deviceActionService))
                {
                    await deviceActionService.DisplayAlertAsync(title, message, AppResources.Ok);
                }
            }

            void FailAndFinish()
            {
                var result = new Intent();
                PendingIntentHandler.SetCreateCredentialException(result, new CreateCredentialUnknownException());

                activity.SetResult(Result.Ok, result);
                activity.Finish();
            }
        }

        private static Fido2CreateCredentialExtensionsParams MapExtensionsFromJson(PublicKeyCredentialCreationOptions options)
        {
            if (options == null || !options.Json.Has("extensions"))
            {
                return null;
            }

            var extensions = options.Json.GetJSONObject("extensions");
            return new Fido2CreateCredentialExtensionsParams
            {
                CredProps = extensions.Has("credProps") && extensions.GetBoolean("credProps")
            };
        }

        private static JSONObject MapExtensionsToJson(Fido2CreateCredentialExtensionsResult extensions)
        {
            if (extensions == null)
            {
                return null;
            }

            var extensionsJson = new JSONObject();
            if (extensions.CredProps != null)
            {
                var credPropsJson = new JSONObject();
                credPropsJson.Put("rk", extensions.CredProps.Rk);
                extensionsJson.Put("credProps", credPropsJson);
            }

            return extensionsJson;
        }

        public static async Task<string> LoadFido2PrivilegedAllowedListAsync()
        {
            try
            {
                using var stream = await FileSystem.OpenAppPackageFileAsync("fido2_privileged_allow_list.json");
                using var reader = new StreamReader(stream);

                return reader.ReadToEnd();
            }
            catch
            {
                return null;
            }
        }

        public static async Task<string> ValidateCallingAppInfoAndGetOriginAsync(CallingAppInfo callingAppInfo, string rpId)
        {
            if (callingAppInfo.Origin is null)
            {
                return await ValidateAssetLinksAndGetOriginAsync(callingAppInfo, rpId);
            }

            var privilegedAllowedList = await LoadFido2PrivilegedAllowedListAsync();
            if (privilegedAllowedList is null)
            {
                throw new InvalidOperationException("Could not load Fido2 privileged allowed list");
            }

            if (!privilegedAllowedList.Contains($"\"package_name\": \"{callingAppInfo.PackageName}\""))
            {
                throw new Core.Exceptions.ValidationException(AppResources.PasskeyOperationFailedBecauseBrowserIsNotPrivileged);
            }

            try
            {
                return callingAppInfo.GetOrigin(privilegedAllowedList);
            }
            catch (Java.Lang.IllegalStateException)
            {
                throw new Core.Exceptions.ValidationException(AppResources.PasskeyOperationFailedBecauseBrowserSignatureDoesNotMatch);
            }
            catch (Java.Lang.IllegalArgumentException)
            {
                return null; // wrong list format
            }
        }

        private static async Task<string> ValidateAssetLinksAndGetOriginAsync(CallingAppInfo callingAppInfo, string rpId)
        {
            if (!ServiceContainer.TryResolve<IAssetLinksService>(out var assetLinksService))
            {
                throw new InvalidOperationException("Can't resolve IAssetLinksService");
            }

            var normalizedFingerprint = callingAppInfo.GetLatestCertificationFingerprint();

            var isValid = await assetLinksService.ValidateAssetLinksAsync(rpId, callingAppInfo.PackageName, normalizedFingerprint);

            return isValid ? callingAppInfo.GetAndroidOrigin() : null;
        }
    }
}
