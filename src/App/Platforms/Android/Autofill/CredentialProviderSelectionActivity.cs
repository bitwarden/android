using System.Security.Cryptography;
using System.Text;
using Android.App;
using Android.Content;
using Android.Content.PM;
using Android.OS;
using Android.Util;
using AndroidX.Credentials;
using AndroidX.Credentials.Provider;
using AndroidX.Credentials.WebAuthn;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Bit.App.Droid.Utilities;
using Bit.Core.Utilities.Fido2;
using Java.Security;
using Java.Security.Spec;
using Org.Json;

namespace Bit.Droid.Autofill
{
    [Activity(
        NoHistory = true,
        LaunchMode = LaunchMode.SingleTop)]
    public class CredentialProviderSelectionActivity : MauiAppCompatActivity
    {

        private IFido2AuthenticatorService _fido2AuthenticatorService;

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

            _fido2AuthenticatorService = ServiceContainer.Resolve<IFido2AuthenticatorService>();

            GetCipherAndPerformPasskeyAuthAsync(cipherId).FireAndForget();
        }

        private async Task GetCipherAndPerformPasskeyAuthAsync(string cipherId)
        {
            // TODO this is a work in progress
            // https://developer.android.com/training/sign-in/credential-provider#passkeys-implement

            var getRequest = PendingIntentHandler.RetrieveProviderGetCredentialRequest(Intent);

            var credentialOption = getRequest?.CredentialOptions.FirstOrDefault();
            var credentialPublic = credentialOption as GetPublicKeyCredentialOption;
            
            var requestOptions = new PublicKeyCredentialRequestOptions(credentialPublic.RequestJson);

            var requestInfo = Intent.GetBundleExtra(CredentialProviderConstants.CredentialDataIntentExtra);
            var credentialId = requestInfo?.GetByteArray(CredentialProviderConstants.CredentialIdIntentExtra);

            var origin = getRequest?.CallingAppInfo.Origin;
            var androidOrigin = AppInfoToOrigin(getRequest?.CallingAppInfo);
            var packageName = getRequest?.CallingAppInfo.PackageName;

            System.Diagnostics.Debug.WriteLine($"RequestOptions JSON: {requestOptions.Json}");

            // TODO: Properly implement this interface 
            var userInterface = new Fido2GetAssertionUserInterface(
                cipherId: cipherId,
                userVerified: true,
                ensureUnlockedVaultCallback: () => Task.FromResult(true),
                hasVaultBeenUnlockedInThisTransaction: () => true,
                verifyUserCallback: (cipherId, verificationPreference) => Task.FromResult(true));

            var assertParams = new Fido2AuthenticatorGetAssertionParams
            {
                Challenge = requestOptions.GetChallenge(),
                RpId = requestOptions.RpId,
                UserVerificationPreference = Fido2UserVerificationPreferenceExtensions.ToFido2UserVerificationPreference(requestOptions.UserVerification),
                Hash = credentialPublic.GetClientDataHash(),
                AllowCredentialDescriptorList = new Core.Utilities.Fido2.PublicKeyCredentialDescriptor[]
                {
                    new Core.Utilities.Fido2.PublicKeyCredentialDescriptor
                    {
                        Id = credentialId
                    }
                },
                Extensions = new object()
            };
            var assertResult = await _fido2AuthenticatorService.GetAssertionAsync(assertParams, userInterface);

            var response = new AuthenticatorAssertionResponse(
                requestOptions,
                assertResult.SelectedCredential.Id,
                androidOrigin,
                false, // These flags have no effect, we set our own within `SetAuthenticatorData`
                false,
                false,
                false,
                assertResult.SelectedCredential.UserHandle,
                packageName,
                credentialPublic.GetClientDataHash() //clientDataHash
            );
            response.SetAuthenticatorData(assertResult.AuthenticatorData);
            response.SetSignature(assertResult.Signature);

            var result = new Intent();
            var fidoCredential = new FidoPublicKeyCredential(assertResult.SelectedCredential.Id, response, "platform");
            var cred = new PublicKeyCredential(fidoCredential.Json());
            var credResponse = new GetCredentialResponse(cred);
            PendingIntentHandler.SetGetCredentialResponse(result, credResponse);
            SetResult(Result.Ok, result);
            Finish();
        }

        //TODO: Delete if not used
        private byte[] GenerateSignature(byte[] dataToSign, byte[] privateKey)
        {
            var sigBase = dataToSign.ToArray();
            var dsa = ECDsa.Create();
            dsa.ImportPkcs8PrivateKey(privateKey, out var bytesRead);

            if (bytesRead == 0)
            {
                throw new Exception("Failed to import private key");
            }

            return dsa.SignData(sigBase, HashAlgorithmName.SHA256, DSASignatureFormat.Rfc3279DerSequence);
        }

        //TODO: WIP Region below to delete if not needed in the end. They are based on Android Sample
        #region ANDROID_SPECIFIC_IMPLEMENTATIONS_TO_DELETE_AFTERWARDS
        private IPrivateKey ConvertPrivateKey(byte[] privateKeyBytes)
        {
            var paramsPri = AlgorithmParameters.GetInstance("EC");
            paramsPri.Init(new ECGenParameterSpec("secp256r1"));
            var classType = Java.Lang.Class.FromType(typeof(ECParameterSpec));
            var spec = paramsPri.GetParameterSpec(classType);
            var bi = new Java.Math.BigInteger(1, privateKeyBytes);
            var privateKeySpec = new ECPrivateKeySpec(bi, spec as ECParameterSpec);
            var keyFactory = KeyFactory.GetInstance("EC");
            return keyFactory.GeneratePrivate(privateKeySpec);// as ECPrivateKey;
        }

        private byte[] b64Decode(string data)
        {
            return Base64.Decode(data, Base64Flags.NoPadding | Base64Flags.NoWrap | Base64Flags.UrlSafe);
        }

        private string b64Encode(byte[] data)
        {
            return Base64.EncodeToString(data, Base64Flags.NoPadding | Base64Flags.NoWrap | Base64Flags.UrlSafe);
        }
        
        private string AppInfoToOrigin(CallingAppInfo info)
        {
            var cert = info.SigningInfo.GetApkContentsSigners()[0].ToByteArray();
            var md = MessageDigest.GetInstance("SHA-256");
            var certHash = md.Digest(cert);
            return $"android:apk-key-hash:${b64Encode(certHash)}";
        }

        private JSONObject ToJson(byte[] clientDataHash, byte[] authenticatorData, byte[] signature, byte[] userHandle, string packageName, PublicKeyCredentialRequestOptions requestOptions, string origin)
        {
            var clientJson = new JSONObject();
            clientJson.Put("type", "webauthn.get");
            clientJson.Put("challenge", b64Encode(requestOptions.GetChallenge()));
            clientJson.Put("origin", origin);
            if (packageName != null)
            {
                clientJson.Put("androidPackageName", packageName);
            }

            var clientData = Encoding.UTF8.GetBytes(clientJson.ToString());
            var response = new JSONObject();
            if (clientDataHash == null)
            {
                response.Put("clientDataJSON", b64Encode(clientData));
            }
            response.Put("authenticatorData", b64Encode(authenticatorData));
            response.Put("signature", b64Encode(signature));
            response.Put("userHandle", b64Encode(userHandle));
            return response;
        }
        #endregion
    }

    #region TMP_OBJECTS_FOR_JSON_RESPONSE_TO_DELETE_AFTERWARDS
    public class AuthenticationResponseJSON 
    {
        public string id { get; set; }
        public string rawId { get; set; }
        public AuthenticatorAssertionResponseJSON response { get; set; }
        public string authenticatorAttachment { get; set; }
        public AuthenticationExtensionsClientOutputsJSON clientExtensionResults { get; set; }
        public string type { get; set; }
    }

    public class AuthenticationExtensionsClientOutputsJSON 
    {
    }

    public class AuthenticatorAssertionResponseJSON 
    {
        public string clientDataJSON { get; set; }
        public string authenticatorData { get; set; }
        public string signature { get; set; }
        public string userHandle { get; set; }
        //public string attestationObject { get; set; }
    }
    #endregion
}
