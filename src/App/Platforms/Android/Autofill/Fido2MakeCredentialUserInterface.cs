using Bit.Core.Abstractions;

namespace Bit.App.Platforms.Android.Autofill
{
    //TODO: WIP: Temporary Dummy implementation
    public class Fido2MakeCredentialUserInterface : IFido2MakeCredentialUserInterface
    {
        public bool HasVaultBeenUnlockedInThisTransaction => true;

        public Task<(string CipherId, bool UserVerified)> ConfirmNewCredentialAsync(Fido2ConfirmNewCredentialParams confirmNewCredentialParams)
        { 
            return Task.FromResult<(string CipherId, bool UserVerified)>((confirmNewCredentialParams.RpId, true));
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
