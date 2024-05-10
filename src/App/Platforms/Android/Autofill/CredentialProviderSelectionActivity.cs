using System.Diagnostics;
using Android.App;
using Android.Content;
using Android.Content.PM;
using Android.OS;
using Android.Runtime;
using AndroidX.Activity.Result;
using AndroidX.Activity.Result.Contract;
using AndroidX.Credentials;
using AndroidX.Credentials.Exceptions;
using AndroidX.Credentials.Provider;
using AndroidX.Credentials.WebAuthn;
using Bit.App.Abstractions;
using Bit.App.Droid.Utilities;
using Bit.App.Platforms.Android.Autofill;
using Bit.Core.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.Core.Utilities.Fido2;
using Org.Json;

namespace Bit.Droid.Autofill
{
    [Activity(
        NoHistory = false,
        LaunchMode = LaunchMode.SingleInstance)]
    [Register("com.x8bit.bitwarden.CredentialProviderSelectionActivity")]
    public class CredentialProviderSelectionActivity : MauiAppCompatActivity
    {
        private LazyResolve<IFido2MediatorService> _fido2MediatorService = new LazyResolve<IFido2MediatorService>();
        private LazyResolve<IFido2AndroidGetAssertionUserInterface> _fido2GetAssertionUserInterface = new LazyResolve<IFido2AndroidGetAssertionUserInterface>();
        private LazyResolve<IVaultTimeoutService> _vaultTimeoutService = new LazyResolve<IVaultTimeoutService>();
        private LazyResolve<IStateService> _stateService = new LazyResolve<IStateService>();
        private LazyResolve<ICipherService> _cipherService = new LazyResolve<ICipherService>();
        private LazyResolve<IUserVerificationMediatorService> _userVerificationMediatorService = new LazyResolve<IUserVerificationMediatorService>();
        private LazyResolve<IDeviceActionService> _deviceActionService = new LazyResolve<IDeviceActionService>();

        private ActivityResultLauncher _activityResultLauncher;

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

                if (getRequest is null)
                {
                    FailAndFinish();
                    return;
                }

                var credentialOption = getRequest.CredentialOptions.FirstOrDefault();
                var credentialPublic = credentialOption as GetPublicKeyCredentialOption;

                var requestOptions = new PublicKeyCredentialRequestOptions(credentialPublic.RequestJson);
                RpId = requestOptions.RpId;

                var requestInfo = Intent.GetBundleExtra(CredentialProviderConstants.CredentialDataIntentExtra);
                var credentialId = requestInfo?.GetByteArray(CredentialProviderConstants.CredentialIdIntentExtra);
                var hasVaultBeenUnlockedInThisTransaction = Intent.GetBooleanExtra(CredentialProviderConstants.CredentialHasVaultBeenUnlockedInThisTransactionExtra, false);

                var packageName = getRequest.CallingAppInfo.PackageName;

                string origin;
                try
                {
                    origin = await CredentialHelpers.ValidateCallingAppInfoAndGetOriginAsync(getRequest.CallingAppInfo, RpId);
                }
                catch (Core.Exceptions.ValidationException valEx)
                {
                    await _deviceActionService.Value.DisplayAlertAsync(AppResources.AnErrorHasOccurred, valEx.Message, AppResources.Ok);
                    FailAndFinish();
                    return;
                }

                if (origin is null)
                {
                    await _deviceActionService.Value.DisplayAlertAsync(AppResources.ErrorReadingPasskey, AppResources.PasskeysNotSupportedForThisApp, AppResources.Ok);
                    FailAndFinish();
                    return;
                }

                _fido2GetAssertionUserInterface.Value.Init(
                    cipherId,
                    false,
                    () => hasVaultBeenUnlockedInThisTransaction,
                        RpId
                    );

                _activityResultLauncher = RegisterForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback(result =>
                    {
                        _fido2GetAssertionUserInterface.Value.ConfirmVaultUnlocked(result.ResultCode == (int)Result.Ok);
                    }));

                var clientAssertParams = new Fido2ClientAssertCredentialParams
                {
                    Challenge = requestOptions.GetChallenge(),
                    RpId = RpId,
                    AllowCredentials = new Core.Utilities.Fido2.PublicKeyCredentialDescriptor[] { new Core.Utilities.Fido2.PublicKeyCredentialDescriptor { Id = credentialId } },
                    Origin = origin,
                    SameOriginWithAncestors = true,
                    UserVerification = requestOptions.UserVerification
                };

                var extraAssertParams = new Fido2ExtraAssertCredentialParams
                (
                    getRequest.CallingAppInfo.Origin != null ? credentialPublic.GetClientDataHash() : null,
                    packageName
                );

                var assertResult = await _fido2MediatorService.Value.AssertCredentialAsync(clientAssertParams, extraAssertParams);

                var result = new Intent();

                var responseInnerAndroidJson = new JSONObject();
                if (assertResult.ClientDataJSON != null)
                {
                    responseInnerAndroidJson.Put("clientDataJSON", CoreHelpers.Base64UrlEncode(assertResult.ClientDataJSON));
                }
                responseInnerAndroidJson.Put("authenticatorData", CoreHelpers.Base64UrlEncode(assertResult.AuthenticatorData));
                responseInnerAndroidJson.Put("signature", CoreHelpers.Base64UrlEncode(assertResult.Signature));
                responseInnerAndroidJson.Put("userHandle", CoreHelpers.Base64UrlEncode(assertResult.SelectedCredential.UserHandle));

                var rootAndroidJson = new JSONObject();
                rootAndroidJson.Put("id", CoreHelpers.Base64UrlEncode(assertResult.SelectedCredential.Id));
                rootAndroidJson.Put("rawId", CoreHelpers.Base64UrlEncode(assertResult.SelectedCredential.Id));
                rootAndroidJson.Put("authenticatorAttachment", "platform");
                rootAndroidJson.Put("type", "public-key");
                rootAndroidJson.Put("clientExtensionResults", new JSONObject());
                rootAndroidJson.Put("response", responseInnerAndroidJson);

                var json = rootAndroidJson.ToString();

                var cred = new PublicKeyCredential(json);
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
                    FailAndFinish();
                });
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                await MainThread.InvokeOnMainThreadAsync(async () =>
                {
                    await _deviceActionService.Value.DisplayAlertAsync(AppResources.ErrorReadingPasskey, string.Format(AppResources.ThereWasAProblemReadingAPasskeyForXTryAgainLater, RpId), AppResources.Ok);
                    FailAndFinish();
                });
            }
        }

        public void LaunchToUnlock()
        {
            if (_activityResultLauncher is null)
            {
                throw new InvalidOperationException("There is no activity result launcher available");
            }

            var intent = new Intent(this, typeof(MainActivity));
            intent.PutExtra(CredentialProviderConstants.Fido2CredentialAction, CredentialProviderConstants.Fido2CredentialNeedsUnlockingAgainBecauseImmediateTimeout);

            _activityResultLauncher.Launch(intent);
        }

        private void FailAndFinish()
        {
            var result = new Intent();
            PendingIntentHandler.SetGetCredentialException(result, new GetCredentialUnknownException());

            SetResult(Result.Ok, result);
            Finish();
        }
    }

    public class ActivityResultCallback : Java.Lang.Object, IActivityResultCallback
    {
        readonly Action<ActivityResult> _callback;
        public ActivityResultCallback(Action<ActivityResult> callback) => _callback = callback;
        public ActivityResultCallback(TaskCompletionSource<ActivityResult> tcs) => _callback = tcs.SetResult;
        public void OnActivityResult(Java.Lang.Object p0) => _callback((ActivityResult)p0);
    }
}
