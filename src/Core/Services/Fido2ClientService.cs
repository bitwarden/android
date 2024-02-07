using Bit.Core.Abstractions;
using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Services
{
    public class Fido2ClientService : IFido2ClientService
    {
        public Task<Fido2ClientCreateCredentialResult> CreateCredentialAsync(Fido2ClientCreateCredentialParams createCredentialParams) => throw new NotImplementedException();

        public Task<Fido2ClientAssertCredentialResult> AssertCredentialAsync(Fido2ClientAssertCredentialParams assertCredentialParams) => throw new NotImplementedException();
    }
}
