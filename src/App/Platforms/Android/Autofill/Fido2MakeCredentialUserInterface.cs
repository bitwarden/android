using Bit.Core.Abstractions;

namespace Bit.App.Platforms.Android.Autofill
{
    //TODO: WIP: Temporary Dummy implementation
    public class Fido2MakeCredentialUserInterface : IFido2MakeCredentialUserInterface
    {
        public Task<(string CipherId, bool UserVerified)> ConfirmNewCredentialAsync(Fido2ConfirmNewCredentialParams confirmNewCredentialParams)
        { 
            return Task.FromResult<(string CipherId, bool UserVerified)>((confirmNewCredentialParams.RpId, confirmNewCredentialParams.UserVerification));
        }

        public Task EnsureUnlockedVaultAsync()
        {
            return Task.FromResult(true);
        }

        public Task InformExcludedCredentialAsync(string[] existingCipherIds)
        {
            return Task.FromResult(true);
        }
    }
}
