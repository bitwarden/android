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

        private IFido2ClientService _fido2ClientService;

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

            _fido2ClientService = ServiceContainer.Resolve<IFido2ClientService>();

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
            var credIdEnc = requestInfo?.GetString(CredentialProviderConstants.CredentialIdIntentExtra);

            var cipherService = ServiceContainer.Resolve<ICipherService>();
            var cipher = await cipherService.GetAsync(cipherId);
            var decCipher = await cipher.DecryptAsync();

            //TODO: Can be deleted if not needed
            var passkey = decCipher.Login.Fido2Credentials.Find(f => f.CredentialId == credIdEnc);

            //TODO: WIP line below to delete if not needed in the end. They are based on Android Sample
            //var credId = Convert.FromBase64String(credIdEnc);
            //var privateKey = Convert.FromBase64String(passkey.KeyValue);
            //var uid = Convert.FromBase64String(passkey.UserHandle);

            var origin = getRequest?.CallingAppInfo.Origin;
            var androidOrigin = AppInfoToOrigin(getRequest?.CallingAppInfo);
            var packageName = getRequest?.CallingAppInfo.PackageName;

            System.Diagnostics.Debug.WriteLine($"RequestOptions JSON: {requestOptions.Json}");

            var assertParams = new Fido2ClientAssertCredentialParams()
            {
                Challenge = requestOptions.GetChallenge(),
                Origin = origin,
                RpId = requestOptions.RpId,
                //AllowCredentials = new Bit.Core.Utilities.Fido2.PublicKeyCredentialDescriptor[]  //TODO: Confirm if this is needed and where to get it.
                //{
                //    new Bit.Core.Utilities.Fido2.PublicKeyCredentialDescriptor
                //    {
                //        Id = credId
                //    }
                //},
                //SameOriginWithAncestors = false, //TODO: Confirm if this is needed and where to get it.
                Timeout = Convert.ToInt32(requestOptions.Timeout),
                UserVerification = requestOptions.UserVerification
            };
            var assertResult = await _fido2ClientService.AssertCredentialAsync(assertParams);

            //TODO: WIP line below to delete if not needed in the end. They are based on Android Sample
            //var clientDataHash = credentialPublic.GetClientDataHash();
            //System.Diagnostics.Debug.WriteLine($"ClientDataHash: {clientDataHash}");

            var response = new AuthenticatorAssertionResponse(
                requestOptions,
                assertResult.RawId,
                androidOrigin,
                true,
                true,
                true,
                true,
                assertResult.UserHandle,
                packageName,
                assertResult.ClientDataJSON //clientDataHash
            );

            System.Diagnostics.Debug.WriteLine($"Base64Id: {assertResult.Id}");

            response.SetAuthenticatorData(assertResult.AuthenticatorData);

            System.Diagnostics.Debug.WriteLine($"RequestJson: {credentialPublic.RequestJson}");
            System.Diagnostics.Debug.WriteLine($"UserHandle: {assertResult.UserHandle}");

            /*
            var signature = GenerateSignature(response.DataToSign(), passkey.KeyBytes);
            response.SetSignature(signature);
            */

            //TODO: WIP lines below to delete if not needed in the end. They are based on Android Sample
            //var privateKeyBytes = Convert.FromBase64String(passkey.KeyValue);
            //var privateKey = ConvertPrivateKey(passkey.KeyBytes);
            //var sig = Java.Security.Signature.GetInstance("SHA256withECDSA");
            //sig.InitSign(privateKey);
            //sig.Update(response.DataToSign());
            //response.SetSignature(sig.Sign());

            response.SetSignature(assertResult.Signature);

            var credential = new FidoPublicKeyCredential
            (
                assertResult.RawId, 
                response, 
                "cross-platform" //TODO: use "platform" vs "cross-platform"?
            );
            
            var clientJson = credential.Response.ClientJson;
            System.Diagnostics.Debug.WriteLine($"ClientJson: {clientJson}");

            
            //TODO: Original Android sample return code
            /*
            var result = new Intent();
            var credentialJson = credential.Json();
            var cred = new PublicKeyCredential(credentialJson);
            var responseJson = cred.AuthenticationResponseJson;
            System.Diagnostics.Debug.WriteLine($"ResponseJson: {responseJson}");
            var credResponse = new GetCredentialResponse(cred);
            PendingIntentHandler.SetGetCredentialResponse(result, credResponse);
            SetResult(Result.Ok, result);
            Finish();
            */

            //TODO: Code below until end of method is using alternative method of building Json ourselves

            var result = new Intent();

            var responseInnerAndroidJson = new JSONObject();
            responseInnerAndroidJson.Put("clientDataJSON", b64Encode(assertResult.ClientDataJSON));
            responseInnerAndroidJson.Put("authenticatorData", b64Encode(assertResult.AuthenticatorData));
            responseInnerAndroidJson.Put("signature", b64Encode(assertResult.Signature));
            responseInnerAndroidJson.Put("userHandle", b64Encode(assertResult.UserHandle));

            var rootAndroidJson = new JSONObject();
            rootAndroidJson.Put("id", b64Encode(assertResult.RawId));
            rootAndroidJson.Put("rawId", b64Encode(assertResult.RawId));
            rootAndroidJson.Put("authenticatorAttachment", "cross-platform");
            rootAndroidJson.Put("type", "public-key");
            rootAndroidJson.Put("clientExtensionResults", new JSONObject());
            rootAndroidJson.Put("response", responseInnerAndroidJson);

            var responseAndroidJson = rootAndroidJson.ToString();
            var cred = new PublicKeyCredential(responseAndroidJson);
            var responseJson = cred.AuthenticationResponseJson;
            System.Diagnostics.Debug.WriteLine($"ResponseJson: {responseJson}");
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
