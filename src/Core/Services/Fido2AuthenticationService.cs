using Bit.Core.Abstractions;
using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Services
{
    public class Fido2AuthenticationService : IFido2AuthenticationService
    {
        public Task<Fido2AuthenticatorGetAssertionResult> GetAssertionAsync(Fido2AuthenticatorGetAssertionParams assertionParams)
        {
            // TODO: IMPLEMENT this
            return Task.FromResult(new Fido2AuthenticatorGetAssertionResult
            {
                AuthenticatorData = new byte[32],
                Signature = new byte[8]
            });
        }
    }
}
