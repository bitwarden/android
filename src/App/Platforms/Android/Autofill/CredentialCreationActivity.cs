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

namespace Bit.Droid.Autofill
{
    [Activity(
        NoHistory = true,
        LaunchMode = LaunchMode.SingleTop)]
    public class CredentialCreationActivity : MauiAppCompatActivity
    {

        private IFido2ClientService _fido2ClientService;

        protected override void OnCreate(Bundle bundle)
        {
            Intent?.Validate();
            base.OnCreate(bundle);

            //TODO: Check if we have a Request, CallingRequest and origin and cancel if otherwise

            _fido2ClientService = ServiceContainer.Resolve<IFido2ClientService>();

            CreateCipherPasskeyAsync().FireAndForget();
        }

        private async Task CreateCipherPasskeyAsync()
        {
            var getRequest = PendingIntentHandler.RetrieveProviderCreateCredentialRequest(Intent);
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
                //Extensions = //TODO: Do we need to handle Extensions?
                SameOriginWithAncestors = true //TODO: Where to get value? Or logic to set?
            };

            var clientCreateCredentialResult = await _fido2ClientService.CreateCredentialAsync(credentialCreateParams);
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

            SetResult(Result.Ok, result);
            Finish();
        }

        //TODO: To Delete if not needed
        private string AppInfoToOrigin(CallingAppInfo info)
        {
            var cert = info.SigningInfo.GetApkContentsSigners()[0].ToByteArray();
            var md = MessageDigest.GetInstance("SHA-256");
            var certHash = md.Digest(cert);
            return $"android:apk-key-hash:${b64Encode(certHash)}";
        }

        //TODO: To Delete if not needed
        private string b64Encode(byte[] data)
        {
            return Base64.EncodeToString(data, Base64Flags.NoPadding | Base64Flags.NoWrap | Base64Flags.UrlSafe);
        }
    }
}
