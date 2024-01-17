using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Services;
using Bit.Core.Test.AutoFixture;
using Bit.Test.Common.AutoFixture;
using Bit.Test.Common.AutoFixture.Attributes;
using NSubstitute;
using NSubstitute.ExceptionExtensions;
using Xunit;

namespace Bit.Core.Test.Services
{
    public class Fido2AuthenticatorTests
    {
        [Theory]
        public async Task GetAssertionAsync_Throws_InputIsMissingSupportedAlgorithm(Fido2AuthenticatorService sut)
        {
            await Assert.ThrowsAsync<NotFoundException>(async () => await sut.GetAssertionAsync(new Fido2AuthenticatorGetAssertionParams()));
        }

        // it("should throw error when input does not contain any supported algorithms", async () => {
        //     const result = async () =>
        //     await authenticator.makeCredential(invalidParams.unsupportedAlgorithm, tab);

        //     await expect(result).rejects.toThrowError(Fido2AuthenticatorErrorCode.NotSupported);
        // });

        private Fido2AuthenticatorGetAssertionParams GetAssertionParams()
        {
            return new Fido2AuthenticatorGetAssertionParams
            {
                RpId = "test",
                Counter = 0,
                CredentialId = new byte[32]
            };
        }
    }
}
