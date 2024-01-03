using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Abstractions
{
    public interface IFido2AuthenticationService
    {
        Task<Fido2AuthenticatorGetAssertionResult> GetAssertionAsync(Fido2AuthenticatorGetAssertionParams assertionParams);
    }
}
