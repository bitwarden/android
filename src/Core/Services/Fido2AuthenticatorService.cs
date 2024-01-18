using Bit.Core.Abstractions;
using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Services
{
    public class Fido2AuthenticatorService : IFido2AuthenticatorService
    {
        private ICipherService _cipherService;
        
        public Fido2AuthenticatorService(ICipherService cipherService)
        {
            _cipherService = cipherService;
        }
        
        public Task<Fido2AuthenticatorGetAssertionResult> GetAssertionAsync(Fido2AuthenticatorGetAssertionParams assertionParams)
        {
            throw new NotAllowedError();
            
            // TODO: IMPLEMENT this
            // return Task.FromResult(new Fido2AuthenticatorGetAssertionResult
            // {
            //     AuthenticatorData = new byte[32],
            //     Signature = new byte[8]
            // });
        }
    }
}
