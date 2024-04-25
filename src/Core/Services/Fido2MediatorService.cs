using Bit.Core.Abstractions;
using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Services
{
    public class Fido2MediatorService : IFido2MediatorService
    {
        private readonly IFido2AuthenticatorService _fido2AuthenticatorService;
        private readonly IFido2ClientService _fido2ClientService;
        private readonly ICipherService _cipherService;

        public Fido2MediatorService(IFido2AuthenticatorService fido2AuthenticatorService,
            IFido2ClientService fido2ClientService,
            ICipherService cipherService)
        {
            _fido2AuthenticatorService = fido2AuthenticatorService;
            _fido2ClientService = fido2ClientService;
            _cipherService = cipherService;
        }

        public async Task<Fido2ClientAssertCredentialResult> AssertCredentialAsync(Fido2ClientAssertCredentialParams assertCredentialParams, Fido2ExtraAssertCredentialParams extraParams)
        {
            var result = await _fido2ClientService.AssertCredentialAsync(assertCredentialParams, extraParams);

            if (result?.SelectedCredential?.Cipher != null)
            {
                await _cipherService.CopyTotpCodeIfNeededAsync(result.SelectedCredential.Cipher);
            }

            return result;
        }

        public Task<Fido2ClientCreateCredentialResult> CreateCredentialAsync(Fido2ClientCreateCredentialParams createCredentialParams, Fido2ExtraCreateCredentialParams extraParams)
        {
            return _fido2ClientService.CreateCredentialAsync(createCredentialParams, extraParams);
        }

        public async Task<Fido2AuthenticatorGetAssertionResult> GetAssertionAsync(Fido2AuthenticatorGetAssertionParams assertionParams, IFido2GetAssertionUserInterface userInterface)
        {
            var result = await _fido2AuthenticatorService.GetAssertionAsync(assertionParams, userInterface);

            if (result?.SelectedCredential?.Cipher != null)
            {
                await _cipherService.CopyTotpCodeIfNeededAsync(result.SelectedCredential.Cipher);
            }

            return result;
        }

        public Task<Fido2AuthenticatorMakeCredentialResult> MakeCredentialAsync(Fido2AuthenticatorMakeCredentialParams makeCredentialParams, IFido2MakeCredentialUserInterface userInterface)
        {
            return _fido2AuthenticatorService.MakeCredentialAsync(makeCredentialParams, userInterface);
        }

        public Task<Fido2AuthenticatorDiscoverableCredentialMetadata[]> SilentCredentialDiscoveryAsync(string rpId)
        {
            return _fido2AuthenticatorService.SilentCredentialDiscoveryAsync(rpId);
        }
    }
}
