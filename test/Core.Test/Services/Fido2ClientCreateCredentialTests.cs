using System;
using System.Threading.Tasks;
using Bit.Core.Services;
using Bit.Core.Utilities.Fido2;
using Bit.Test.Common.AutoFixture;
using Xunit;

namespace Bit.Core.Test.Services
{
    public class Fido2ClientCreateCredentialTests : IDisposable
    {
        private readonly SutProvider<Fido2ClientService> _sutProvider = new SutProvider<Fido2ClientService>().Create();

        private Fido2ClientCreateCredentialParams _params;

        public Fido2ClientCreateCredentialTests()
        {
            _params = new Fido2ClientCreateCredentialParams {
                Origin = "https://bitwarden.com",
                SameOriginWithAncestors = true,
                Attestation = "none",
                Challenge = RandomBytes(32),
                PubKeyCredParams = [
                    new PublicKeyCredentialParameters {
                        Type = "public-key",
                        Alg = -7
                    }
                ],
                Rp = new PublicKeyCredentialRpEntity {
                    Id = "bitwarden.com",
                    Name = "Bitwarden"
                },
                User = new PublicKeyCredentialUserEntity {
                    Id = RandomBytes(32),
                    Name = "user@bitwarden.com",
                    DisplayName = "User"
                }
            };
        }

        public void Dispose() 
        {
        }

        [Fact]
        // Spec: If sameOriginWithAncestors is false, return a "NotAllowedError" DOMException.
        public async Task CreateCredentialAsync_ThrowsNotAllowedError_SameOriginWithAncestorsIsFalse()
        {
            _params.SameOriginWithAncestors = false;

            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.CreateCredentialAsync(_params));

            Assert.Equal(Fido2ClientException.ErrorCode.NotAllowedError, exception.Code);
        }
        
        [Fact]
        // Spec: If the length of options.user.id is not between 1 and 64 bytes (inclusive) then return a TypeError.
        public async Task CreateCredentialAsync_ThrowsTypeError_UserIdIsTooSmall()
        {
            _params.User.Id = RandomBytes(0);

            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.CreateCredentialAsync(_params));

            Assert.Equal(Fido2ClientException.ErrorCode.TypeError, exception.Code);
        }

        [Fact]
        // Spec: If the length of options.user.id is not between 1 and 64 bytes (inclusive) then return a TypeError.
        public async Task CreateCredentialAsync_ThrowsTypeError_UserIdIsTooLarge()
        {
            _params.User.Id = RandomBytes(65);

            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.CreateCredentialAsync(_params));

            Assert.Equal(Fido2ClientException.ErrorCode.TypeError, exception.Code);
        }


        private byte[] RandomBytes(int length)
        {
            var bytes = new byte[length];
            new Random().NextBytes(bytes);
            return bytes;
        }
    }
}
