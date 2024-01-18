using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Services;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using Bit.Core.Test.AutoFixture;
using Bit.Core.Utilities.Fido2;
using Bit.Test.Common.AutoFixture;
using Bit.Test.Common.AutoFixture.Attributes;
using NSubstitute;
using NSubstitute.ExceptionExtensions;
using Xunit;
using Bit.Core.Utilities;
using System.Collections.Generic;

namespace Bit.Core.Test.Services
{
    public class Fido2AuthenticatorTests
    {
        #region missing non-discoverable credential

        // Spec: If credentialOptions is now empty, return an error code equivalent to "NotAllowedError" and terminate the operation.
        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        public async Task GetAssertionAsync_Throws_NoCredentialExists(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorGetAssertionParams aParams)
        {
            var exception = await Assert.ThrowsAsync<NotAllowedError>(() => sutProvider.Sut.GetAssertionAsync(aParams));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        public async Task GetAssertionAsync_Throws_CredentialExistsButRpIdDoesNotMatch(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorGetAssertionParams aParams)
        {
            var credentialId = RandomBytes(32);
            aParams.RpId = "bitwarden.com";
            aParams.AllowCredentialDescriptorList = [
                new PublicKeyCredentialDescriptor {
                    Id = credentialId,
                    Type = "public-key"
                }
            ];
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(new List<CipherView> {
                new CipherView {
                    Login = new LoginView {
                        Fido2Credentials = new List<Fido2CredentialView> {
                            new Fido2CredentialView {
                                CredentialId = CoreHelpers.Base64UrlEncode(credentialId),
                                RpId = "mismatch-rpid"
                            }
                        }
                    }
                }
            });

            var exception = await Assert.ThrowsAsync<NotAllowedError>(() => sutProvider.Sut.GetAssertionAsync(aParams));
        }

        #endregion

        private byte[] RandomBytes(int length)
        {
            var bytes = new byte[length];
            new Random().NextBytes(bytes);
            return bytes;
        }
    }
}
