using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Services;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using Bit.Core.Enums;
using Bit.Core.Test.AutoFixture;
using Bit.Core.Utilities.Fido2;
using Bit.Test.Common.AutoFixture;
using Bit.Test.Common.AutoFixture.Attributes;
using NSubstitute;
using NSubstitute.ExceptionExtensions;
using Xunit;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Linq;

namespace Bit.Core.Test.Services
{
    public class Fido2AuthenticatorMakeCredentialTests
    {
        #region missing non-discoverable credential

        // Spec: If credentialOptions is now empty, return an error code equivalent to "NotAllowedError" and terminate the operation.
        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        public async Task GetAssertionAsync_ThrowsNotSupported_NoSupportedAlgorithm(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorMakeCredentialParams mParams)
        {
            mParams.CredTypesAndPubKeyAlgs = [
                new PublicKeyCredentialAlgorithmDescriptor {
                    Type = "public-key",
                    Algorithm = -257 // RS256 which we do not support
                }
            ];

            await Assert.ThrowsAsync<NotSupportedError>(() => sutProvider.Sut.MakeCredentialAsync(mParams));
        }

        // [Theory]
        // [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        // public async Task GetAssertionAsync_Throws_CredentialExistsButRpIdDoesNotMatch(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorGetAssertionParams aParams)
        // {
        //     var credentialId = Guid.NewGuid();
        //     aParams.RpId = "bitwarden.com";
        //     aParams.AllowCredentialDescriptorList = [
        //         new PublicKeyCredentialDescriptor {
        //             Id = credentialId.ToByteArray(),
        //             Type = "public-key"
        //         }
        //     ];
        //     sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns([
        //         CreateCipherView(credentialId.ToString(), "mismatch-rpid", false),
        //     ]);

        //     await Assert.ThrowsAsync<NotAllowedError>(() => sutProvider.Sut.GetAssertionAsync(aParams));
        // }

        #endregion

        private byte[] RandomBytes(int length)
        {
            var bytes = new byte[length];
            new Random().NextBytes(bytes);
            return bytes;
        }

        #nullable enable
        private CipherView CreateCipherView(string? credentialId, string? rpId, bool? discoverable)
        {
            return new CipherView {
                Type = CipherType.Login,
                Id = Guid.NewGuid().ToString(),
                Reprompt = CipherRepromptType.None,
                Login = new LoginView {
                    Fido2Credentials = new List<Fido2CredentialView> {
                        new Fido2CredentialView {
                            CredentialId = credentialId ?? Guid.NewGuid().ToString(),
                            RpId = rpId ?? "bitwarden.com",
                            Discoverable = discoverable.HasValue ? discoverable.ToString() : "true",
                            UserHandleValue = RandomBytes(32),
                        }
                    }
                }
            };
        }
    }
}
