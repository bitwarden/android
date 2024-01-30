using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Abstractions
{
    public interface IFido2AuthenticatorService
    {
        Task<Fido2AuthenticatorMakeCredentialResult> MakeCredentialAsync(Fido2AuthenticatorMakeCredentialParams makeCredentialParams);
        Task<Fido2AuthenticatorGetAssertionResult> GetAssertionAsync(Fido2AuthenticatorGetAssertionParams assertionParams);
        // TODO: Should this return a List? Or maybe IEnumerable?
        Task<Fido2AuthenticatorDiscoverableCredentialMetadata[]> SilentCredentialDiscoveryAsync(string rpId);
    }
}
