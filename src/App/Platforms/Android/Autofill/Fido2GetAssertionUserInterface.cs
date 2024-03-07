using Bit.Core.Abstractions;

namespace Bit.App.Platforms.Android.Autofill
{
    //TODO: WIP: Temporary Dummy implementation
    public class Fido2GetAssertionUserInterface : IFido2GetAssertionUserInterface
    {
        public Task EnsureUnlockedVaultAsync()
        {
            return Task.FromResult(true);
        }

        public Task<(string CipherId, bool UserVerified)> PickCredentialAsync(Fido2GetAssertionUserInterfaceCredential[] credentials)
        {
            var credential = credentials[0];
            return Task.FromResult<(string CipherId, bool UserVerified)>((credential.CipherId, true));
        }
    }
}
