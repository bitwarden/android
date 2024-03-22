using Android;
using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using AndroidX.Credentials.Provider;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using AndroidX.Credentials.Exceptions;
using Bit.App.Droid.Utilities;
using Bit.Core.Resources.Localization;
using Bit.Core.Utilities.Fido2;

namespace Bit.Droid.Autofill
{
    [Service(Permission = Manifest.Permission.BindCredentialProviderService, Label = "Bitwarden", Exported = true)]
    [IntentFilter(new string[] { "android.service.credentials.CredentialProviderService" })]
    [MetaData("android.credentials.provider", Resource = "@xml/provider")]
    [Register("com.x8bit.bitwarden.Autofill.CredentialProviderService")]
    public class CredentialProviderService : AndroidX.Credentials.Provider.CredentialProviderService
    {
        public const string GetFido2IntentAction = "PACKAGE_NAME.GET_PASSKEY";
        public const string CreateFido2IntentAction = "PACKAGE_NAME.CREATE_PASSKEY";
        public const int UniqueGetRequestCode = 94556023;
        public const int UniqueCreateRequestCode = 94556024;

        private readonly LazyResolve<IVaultTimeoutService> _vaultTimeoutService = new LazyResolve<IVaultTimeoutService>();
        private readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>();

        public override void OnBeginCreateCredentialRequest(BeginCreateCredentialRequest request,
            CancellationSignal cancellationSignal, IOutcomeReceiver callback)
        {
            var response = ProcessCreateCredentialsRequestAsync(request);
            if (response != null)
            {
                callback.OnResult(response);
            } 
            else
            {
                callback.OnError(null); //TODO: Reply with an exception based from Java.Lang.Object
            }
        }

        public override async void OnBeginGetCredentialRequest(BeginGetCredentialRequest request,
            CancellationSignal cancellationSignal, IOutcomeReceiver callback)
        {
            try
            {
                await _vaultTimeoutService.Value.CheckVaultTimeoutAsync();
                var locked = await _vaultTimeoutService.Value.IsLockedAsync();
                if (!locked)
                {
                    var response = await ProcessGetCredentialsRequestAsync(request);
                    callback.OnResult(response);
                    return;
                }

                var intent = new Intent(ApplicationContext, typeof(MainActivity));
                intent.PutExtra(CredentialProviderConstants.Fido2CredentialAction, CredentialProviderConstants.Fido2CredentialGet);
                var pendingIntent = PendingIntent.GetActivity(ApplicationContext, UniqueGetRequestCode, intent,
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

        private BeginCreateCredentialResponse ProcessCreateCredentialsRequestAsync(
            BeginCreateCredentialRequest request)
        {
            if (request == null) { return null; }

            if (request is BeginCreatePasswordCredentialRequest beginCreatePasswordCredentialRequest)
            {
                //TODO: Is the Create Password needed?
                throw new NotImplementedException();
                //return HandleCreatePasswordQuery(beginCreatePasswordCredentialRequest);
            }
            else if (request is BeginCreatePublicKeyCredentialRequest beginCreatePublicKeyCredentialRequest)
            {
                return HandleCreatePasskeyQuery(beginCreatePublicKeyCredentialRequest);
            }

            return null;
        }

        private BeginCreateCredentialResponse HandleCreatePasskeyQuery(BeginCreatePublicKeyCredentialRequest optionRequest)
        {
            var intent = new Intent(ApplicationContext, typeof(MainActivity));
            intent.PutExtra(CredentialProviderConstants.Fido2CredentialAction, CredentialProviderConstants.Fido2CredentialCreate);
            var pendingIntent = PendingIntent.GetActivity(ApplicationContext, UniqueCreateRequestCode, intent,
                AndroidHelpers.AddPendingIntentMutabilityFlag(PendingIntentFlags.UpdateCurrent, true));

            //TODO: i81n needs to be done
            var createEntryBuilder = new CreateEntry.Builder("Bitwarden Vault", pendingIntent)
                .SetDescription("Your passkey will be saved securely to the Bitwarden Vault. You can use it from any other device for sign-in in the future.")
                .Build();

            var createCredentialResponse = new BeginCreateCredentialResponse.Builder()
                .AddCreateEntry(createEntryBuilder);

            return createCredentialResponse.Build();
        }

        private async Task<BeginGetCredentialResponse> ProcessGetCredentialsRequestAsync(
            BeginGetCredentialRequest request)
        {
            var credentialEntries = new List<CredentialEntry>();

            foreach (var option in request.BeginGetCredentialOptions.OfType<BeginGetPublicKeyCredentialOption>())
            {
                credentialEntries.AddRange(await Bit.App.Platforms.Android.Autofill.CredentialHelpers.PopulatePasskeyDataAsync(request.CallingAppInfo, option, ApplicationContext, false));
            }

            if (!credentialEntries.Any())
            {
                return new BeginGetCredentialResponse();
            }

            return new BeginGetCredentialResponse.Builder()
                .SetCredentialEntries(credentialEntries)
                .Build();
        }

        public override void OnClearCredentialStateRequest(ProviderClearCredentialStateRequest request,
            CancellationSignal cancellationSignal, IOutcomeReceiver callback)
        {
            callback.OnResult(null);
        }
    }
}
