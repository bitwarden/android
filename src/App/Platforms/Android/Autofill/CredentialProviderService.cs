using Android;
using Android.App;
using Android.Content;
using Android.Graphics.Drawables;
using Android.OS;
using Android.Runtime;
using AndroidX.Credentials.Provider;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using AndroidX.Credentials.Exceptions;
using AndroidX.Credentials.WebAuthn;
using Bit.App.Droid.Utilities;
using Bit.Core.Models.View;
using Resource = Microsoft.Maui.Resource;
using Bit.Core.Resources.Localization;

namespace Bit.Droid.Autofill
{
    [Service(Permission = Manifest.Permission.BindCredentialProviderService, Label = "Bitwarden", Exported = true)]
    [IntentFilter(new string[] { "android.service.credentials.CredentialProviderService" })]
    [MetaData("android.credentials.provider", Resource = "@xml/provider")]
    [Register("com.x8bit.bitwarden.Autofill.CredentialProviderService")]
    public class CredentialProviderService : AndroidX.Credentials.Provider.CredentialProviderService
    {
        private const string GetPasskeyIntentAction = "PACKAGE_NAME.GET_PASSKEY";
        private const int UniqueRequestCode = 94556023;

        private ICipherService _cipherService;
        private IUserVerificationService _userVerificationService;
        private IVaultTimeoutService _vaultTimeoutService;
        private LazyResolve<ILogger> _logger = new LazyResolve<ILogger>("logger");

        public override async void OnBeginCreateCredentialRequest(BeginCreateCredentialRequest request,
            CancellationSignal cancellationSignal, IOutcomeReceiver callback) => throw new NotImplementedException();

        public override async void OnBeginGetCredentialRequest(BeginGetCredentialRequest request,
            CancellationSignal cancellationSignal, IOutcomeReceiver callback)
        {
            try
            {
                _vaultTimeoutService ??= ServiceContainer.Resolve<IVaultTimeoutService>();

                await _vaultTimeoutService.CheckVaultTimeoutAsync();
                var locked = await _vaultTimeoutService.IsLockedAsync();
                if (!locked)
                {
                    var response = await ProcessGetCredentialsRequestAsync(request);
                    callback.OnResult(response);
                    return;
                }

                var intent = new Intent(ApplicationContext, typeof(MainActivity));
                intent.PutExtra(CredentialProviderConstants.PasskeyFramework, true);
                var pendingIntent = PendingIntent.GetActivity(ApplicationContext, UniqueRequestCode, intent,
                    AndroidHelpers.AddPendingIntentMutabilityFlag(PendingIntentFlags.UpdateCurrent, true));

                var unlockAction = new AuthenticationAction(AppResources.Unlock, pendingIntent);

                var unlockResponse = new BeginGetCredentialResponse.Builder()
                    .SetAuthenticationActions(new List<AuthenticationAction>() { unlockAction } )
                    .Build();
                callback.OnResult(unlockResponse);
            }
            catch (GetCredentialException e)
            {
                _logger.Value.Exception(e);
                callback.OnError(e.ErrorMessage ?? "Error getting credentials");
            }
            catch (Exception e)
            {
                _logger.Value.Exception(e);
                throw;
            }
        }

        private async Task<BeginGetCredentialResponse> ProcessGetCredentialsRequestAsync(
            BeginGetCredentialRequest request)
        {
            List<CredentialEntry> credentialEntries = null;

            foreach (var option in request.BeginGetCredentialOptions)
            {
                var credentialOption = option as BeginGetPublicKeyCredentialOption;
                if (credentialOption != null)
                {
                    credentialEntries ??= new List<CredentialEntry>();
                    credentialEntries.AddRange(await PopulatePasskeyDataAsync(request.CallingAppInfo, credentialOption));
                }
            }

            if (credentialEntries == null)
            {
                return new BeginGetCredentialResponse();
            }

            return new BeginGetCredentialResponse.Builder()
                .SetCredentialEntries(credentialEntries)
                .Build();
        }

        private async Task<List<CredentialEntry>> PopulatePasskeyDataAsync(CallingAppInfo callingAppInfo,
            BeginGetPublicKeyCredentialOption option)
        {
            var packageName = callingAppInfo.PackageName;
            var origin = callingAppInfo.Origin;
            var signingInfo = callingAppInfo.SigningInfo;

            var request = new PublicKeyCredentialRequestOptions(option.RequestJson);

            var passkeyEntries = new List<CredentialEntry>();

            _cipherService ??= ServiceContainer.Resolve<ICipherService>();
            var ciphers = await _cipherService.GetAllDecryptedForUrlAsync(origin);
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
                var passkeyEntry = GetPasskey(cipher, option);
                passkeyEntries.Add(passkeyEntry);
            }

            return passkeyEntries;
        }

        private PublicKeyCredentialEntry GetPasskey(CipherView cipher, BeginGetPublicKeyCredentialOption option)
        {
            var credDataBundle = new Bundle();
            credDataBundle.PutString(CredentialProviderConstants.CredentialIdIntentExtra,
                cipher.Login.MainFido2Credential.CredentialId);

            var intent = new Intent(ApplicationContext, typeof(CredentialProviderSelectionActivity))
                .SetAction(GetPasskeyIntentAction).SetPackage(Constants.PACKAGE_NAME);
            intent.PutExtra(CredentialProviderConstants.CredentialDataIntentExtra, credDataBundle);
            intent.PutExtra(CredentialProviderConstants.CredentialProviderCipherId, cipher.Id);
            var pendingIntent = PendingIntent.GetActivity(ApplicationContext, UniqueRequestCode, intent,
                PendingIntentFlags.Mutable | PendingIntentFlags.UpdateCurrent);

            return new PublicKeyCredentialEntry.Builder(
                    ApplicationContext,
                    cipher.Login.Username ?? "No username",
                    pendingIntent,
                    option)
                .SetDisplayName(cipher.Name)
                .SetIcon(Icon.CreateWithResource(ApplicationContext, Resource.Drawable.icon))
                .Build();
        }

        public override void OnClearCredentialStateRequest(ProviderClearCredentialStateRequest request,
            CancellationSignal cancellationSignal, IOutcomeReceiver callback) => throw new NotImplementedException();
    }
}
