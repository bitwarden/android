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
            var credentialId = Guid.NewGuid();
            aParams.RpId = "bitwarden.com";
            aParams.AllowCredentialDescriptorList = [
                new PublicKeyCredentialDescriptor {
                    Id = credentialId.ToByteArray(),
                    Type = "public-key"
                }
            ];
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns([
                CreateCipherView(credentialId.ToString(), "mismatch-rpid", false),
            ]);

            var exception = await Assert.ThrowsAsync<NotAllowedError>(() => sutProvider.Sut.GetAssertionAsync(aParams));
        }

        #endregion

        #region vault contains credential

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        public async Task GetAssertionAsync_AsksForAllCredentials_ParamsContainsAllowedCredentialsList(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorGetAssertionParams aParams)
        {
            var credentialIds = new[] { Guid.NewGuid(), Guid.NewGuid() };
            List<CipherView> ciphers = [ 
                CreateCipherView(credentialIds[0].ToString(), "bitwarden.com", false),
                CreateCipherView(credentialIds[1].ToString(), "bitwarden.com", true)
            ];
            aParams.RpId = "bitwarden.com";
            aParams.AllowCredentialDescriptorList = credentialIds.Select((credentialId) => new PublicKeyCredentialDescriptor {
                Id = credentialId.ToByteArray(),
                Type = "public-key"
            }).ToArray();
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(ciphers);

            await sutProvider.Sut.GetAssertionAsync(aParams);

            await sutProvider.GetDependency<IFido2UserInterface>().Received().PickCredentialAsync(Arg.Is<Fido2PickCredentialParams>(
                (pickCredentialParams) => pickCredentialParams.CipherIds.SequenceEqual(ciphers.Select((cipher) => cipher.Id)) && pickCredentialParams.UserVerification == aParams.RequireUserVerification
            ));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        public async Task GetAssertionAsync_AsksForDiscoverableCredentials_ParamsDoesNotContainsAllowedCredentialsList(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorGetAssertionParams aParams)
        {
            var credentialIds = new[] { Guid.NewGuid(), Guid.NewGuid() };
            List<CipherView> ciphers = [ 
                CreateCipherView(credentialIds[0].ToString(), "bitwarden.com", false),
                CreateCipherView(credentialIds[1].ToString(), "bitwarden.com", true)
            ];
            var discoverableCiphers = ciphers.Where((cipher) => cipher.Login.MainFido2Credential.IsDiscoverable).ToList();
            aParams.RpId = "bitwarden.com";
            aParams.AllowCredentialDescriptorList = null;
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(ciphers);

            await sutProvider.Sut.GetAssertionAsync(aParams);

            await sutProvider.GetDependency<IFido2UserInterface>().Received().PickCredentialAsync(Arg.Is<Fido2PickCredentialParams>(
                (pickCredentialParams) => pickCredentialParams.CipherIds.SequenceEqual(discoverableCiphers.Select((cipher) => cipher.Id)) && pickCredentialParams.UserVerification == aParams.RequireUserVerification
            ));
        }

    //   it("should only ask for discoverable credentials matched by rpId when params does not contains allowedCredentials list", async () => {
    //     params.allowCredentialDescriptorList = undefined;
    //     const discoverableCiphers = ciphers.filter((c) => c.login.fido2Credentials[0].discoverable);
    //     userInterfaceSession.pickCredential.mockResolvedValue({
    //       cipherId: discoverableCiphers[0].id,
    //       userVerified: false,
    //     });

    //     await authenticator.getAssertion(params, tab);

    //     expect(userInterfaceSession.pickCredential).toHaveBeenCalledWith({
    //       cipherIds: [discoverableCiphers[0].id],
    //       userVerification: false,
    //     });
    //   });

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
                Login = new LoginView {
                    Fido2Credentials = new List<Fido2CredentialView> {
                        new Fido2CredentialView {
                            CredentialId = credentialId ?? Guid.NewGuid().ToString(),
                            RpId = rpId ?? "bitwarden.com",
                            Discoverable = discoverable.HasValue ? discoverable.ToString() : "true"
                        }
                    }
                }
            };
        }
    }
}
