using Bit.Core.Abstractions;
using Bit.Core.Utilities;

namespace Bit.App.Platforms.Android.Autofill
{
    //TODO: WIP: Temporary Dummy implementation
    public class Fido2MakeCredentialUserInterface : IFido2MakeCredentialUserInterface
    {
        private ICipherService _cipherService;

        public Fido2MakeCredentialUserInterface()
        {
            _cipherService ??= ServiceContainer.Resolve<ICipherService>();
        }

        public bool HasVaultBeenUnlockedInThisTransaction => true;

        public async Task<(string CipherId, bool UserVerified)> ConfirmNewCredentialAsync(Fido2ConfirmNewCredentialParams confirmNewCredentialParams)
        {
            //TODO: WIP: Need to actually check if user verified and potentially other things
            var cipherId = await _cipherService.CreateNewLoginForPasskeyAsync(confirmNewCredentialParams);
            var verified = true;
            return (cipherId, verified);
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
