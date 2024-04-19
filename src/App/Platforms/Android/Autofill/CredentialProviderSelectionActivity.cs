using Android.App;
using Android.Content;
using Android.Content.PM;
using Android.OS;
using AndroidX.Credentials;
using AndroidX.Credentials.Provider;
using AndroidX.Credentials.WebAuthn;
using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Bit.App.Droid.Utilities;
using Bit.Core.Resources.Localization;
using Bit.Core.Utilities.Fido2;
using Java.Security;
using Bit.Core.Services;
using Bit.App.Platforms.Android.Autofill;

namespace Bit.Droid.Autofill
{
    [Activity(
        NoHistory = true,
        LaunchMode = LaunchMode.SingleTop)]
    public class CredentialProviderSelectionActivity : MauiAppCompatActivity
    {
        private LazyResolve<IFido2MediatorService> _fido2MediatorService = new LazyResolve<IFido2MediatorService>();
        private LazyResolve<IFido2AndroidGetAssertionUserInterface> _fido2GetAssertionUserInterface = new LazyResolve<IFido2AndroidGetAssertionUserInterface>();
        private LazyResolve<IVaultTimeoutService> _vaultTimeoutService = new LazyResolve<IVaultTimeoutService>();
        private LazyResolve<IStateService> _stateService = new LazyResolve<IStateService>();
        private LazyResolve<ICipherService> _cipherService = new LazyResolve<ICipherService>();
        private LazyResolve<IUserVerificationMediatorService> _userVerificationMediatorService = new LazyResolve<IUserVerificationMediatorService>();
        private LazyResolve<IDeviceActionService> _deviceActionService = new LazyResolve<IDeviceActionService>();

        protected override void OnCreate(Bundle bundle)
        {
            Intent?.Validate();
            base.OnCreate(bundle);

            var cipherId = Intent?.GetStringExtra(CredentialProviderConstants.CredentialProviderCipherId);
            if (string.IsNullOrEmpty(cipherId))
            {
                Finish();
                return;
            }

            GetCipherAndPerformFido2AuthAsync(cipherId).FireAndForget();
        }

        //Used to avoid crash on MAUI when doing back
        public override void OnBackPressed()
        {
            Finish();
        }

        private async Task GetCipherAndPerformFido2AuthAsync(string cipherId)
        {
            string RpId = string.Empty;
            try
            {
                var getRequest = PendingIntentHandler.RetrieveProviderGetCredentialRequest(Intent);

                var credentialOption = getRequest?.CredentialOptions.FirstOrDefault();
                var credentialPublic = credentialOption as GetPublicKeyCredentialOption;

                var requestOptions = new PublicKeyCredentialRequestOptions(credentialPublic.RequestJson);
                RpId = requestOptions.RpId;

                var requestInfo = Intent.GetBundleExtra(CredentialProviderConstants.CredentialDataIntentExtra);
                var credentialId = requestInfo?.GetByteArray(CredentialProviderConstants.CredentialIdIntentExtra);
                var hasVaultBeenUnlockedInThisTransaction = Intent.GetBooleanExtra(CredentialProviderConstants.CredentialHasVaultBeenUnlockedInThisTransactionExtra, false);

                var androidOrigin = AppInfoToOrigin(getRequest?.CallingAppInfo);
                var packageName = getRequest?.CallingAppInfo.PackageName;
                var appInfoOrigin = getRequest?.CallingAppInfo.Origin;

                _fido2GetAssertionUserInterface.Value.Init(
                    cipherId,
                    false,
                    () => hasVaultBeenUnlockedInThisTransaction,
                    RpId
                );

                var clientAssertParams = new Fido2ClientAssertCredentialParams
                {
                    Challenge = requestOptions.GetChallenge(),
                    RpId = RpId,
                    AllowCredentials = new Core.Utilities.Fido2.PublicKeyCredentialDescriptor[] { new Core.Utilities.Fido2.PublicKeyCredentialDescriptor { Id = credentialId } },
                    Origin = appInfoOrigin,
                    SameOriginWithAncestors = true,
                    UserVerification = requestOptions.UserVerification
                };

                var assertResult = await _fido2MediatorService.Value.AssertCredentialAsync(clientAssertParams, credentialPublic.GetClientDataHash());

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
                    assertResult.ClientDataHash
                );
                response.SetAuthenticatorData(assertResult.AuthenticatorData);
                response.SetSignature(assertResult.Signature);

                var result = new Intent();
                var fidoCredential = new FidoPublicKeyCredential(assertResult.SelectedCredential.Id, response, "platform");
                var cred = new PublicKeyCredential(fidoCredential.Json());
                var credResponse = new GetCredentialResponse(cred);
                PendingIntentHandler.SetGetCredentialResponse(result, credResponse);

                await MainThread.InvokeOnMainThreadAsync(() =>
                {
                    SetResult(Result.Ok, result);
                    Finish();
                });
            }
            catch (NotAllowedError)
            {
                await MainThread.InvokeOnMainThreadAsync(async () =>
                {
                    await _deviceActionService.Value.DisplayAlertAsync(AppResources.ErrorReadingPasskey, string.Format(AppResources.ThereWasAProblemReadingAPasskeyForXTryAgainLater, RpId), AppResources.Ok);
                    Finish();
                });
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                await MainThread.InvokeOnMainThreadAsync(async () =>
                {
                    await _deviceActionService.Value.DisplayAlertAsync(AppResources.ErrorReadingPasskey, string.Format(AppResources.ThereWasAProblemReadingAPasskeyForXTryAgainLater, RpId), AppResources.Ok);
                    Finish();
                });
            }
        }

        private string AppInfoToOrigin(CallingAppInfo info)
        {
            var cert = info.SigningInfo.GetApkContentsSigners()[0].ToByteArray();
            var md = MessageDigest.GetInstance("SHA-256");
            var certHash = md.Digest(cert);
            return $"android:apk-key-hash:${CoreHelpers.Base64UrlEncode(certHash)}";
        }
    }
}
