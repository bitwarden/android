using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Abstractions
{
    public interface IFido2MediatorService
    {
        Task<Fido2ClientCreateCredentialResult> CreateCredentialAsync(Fido2ClientCreateCredentialParams createCredentialParams, Fido2ExtraCreateCredentialParams extraParams);
        Task<Fido2ClientAssertCredentialResult> AssertCredentialAsync(Fido2ClientAssertCredentialParams assertCredentialParams, Fido2ExtraAssertCredentialParams extraParams);

        Task<Fido2AuthenticatorMakeCredentialResult> MakeCredentialAsync(Fido2AuthenticatorMakeCredentialParams makeCredentialParams, IFido2MakeCredentialUserInterface userInterface);
        Task<Fido2AuthenticatorGetAssertionResult> GetAssertionAsync(Fido2AuthenticatorGetAssertionParams assertionParams, IFido2GetAssertionUserInterface userInterface);
        Task<Fido2AuthenticatorDiscoverableCredentialMetadata[]> SilentCredentialDiscoveryAsync(string rpId);
    }
}
