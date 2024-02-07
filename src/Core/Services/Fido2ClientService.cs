using Bit.Core.Abstractions;
using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Services
{
    public class Fido2ClientService : IFido2ClientService
    {
        public Task<Fido2ClientCreateCredentialResult> CreateCredentialAsync(Fido2ClientCreateCredentialParams createCredentialParams) 
        {
            if (!createCredentialParams.SameOriginWithAncestors)
            {
                throw new Fido2ClientException(
                    Fido2ClientException.ErrorCode.NotAllowedError,
                    "Credential creation is now allowed from embedded contexts with different origins.");
            }

            if (createCredentialParams.User.Id.Length < 1 || createCredentialParams.User.Id.Length > 64)
            {
                // TODO: Should we use ArgumentException here instead?
                throw new Fido2ClientException(
                    Fido2ClientException.ErrorCode.TypeError,
                    "The length of user.id is not between 1 and 64 bytes (inclusive).");
            }

            throw new NotImplementedException();
        }

        public Task<Fido2ClientAssertCredentialResult> AssertCredentialAsync(Fido2ClientAssertCredentialParams assertCredentialParams) => throw new NotImplementedException();
    }
}
