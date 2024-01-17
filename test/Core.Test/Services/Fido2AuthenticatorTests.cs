using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Services;
using Bit.Core.Test.AutoFixture;
using Bit.Core.Utilities.Fido2;
using Bit.Test.Common.AutoFixture;
using Bit.Test.Common.AutoFixture.Attributes;
using NSubstitute;
using NSubstitute.ExceptionExtensions;
using Xunit;

namespace Bit.Core.Test.Services
{
    public class Fido2AuthenticatorTests
    {
        // Spec: If credentialOptions is now empty, return an error code equivalent to "NotAllowedError" and terminate the operation.
        [Theory, SutAutoData]
        public async Task GetAssertionAsync_Throws_NoCredentialExists(Fido2AuthenticatorService sut)
        {
            var assertionParams = CreateAssertionParams();
            var exception = await Assert.ThrowsAsync<NotAllowedError>(() => sut.GetAssertionAsync(assertionParams));
        }

        private Fido2AuthenticatorGetAssertionParams CreateAssertionParams()
        {
            return new Fido2AuthenticatorGetAssertionParams
            {
                RpId = "bitwarden.com",
                Hash = new byte[32],
                AllowCredentialDescriptorList = new PublicKeyCredentialDescriptor[0],
                RequireUserVerification = true,
                Extensions = new object()
            };
        }
    }
}
