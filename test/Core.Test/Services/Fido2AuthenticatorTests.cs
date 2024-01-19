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
        public async Task GetAssertionAsync_ThrowsNotAllowed_NoCredentialExists(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorGetAssertionParams aParams)
        {
            await Assert.ThrowsAsync<NotAllowedError>(() => sutProvider.Sut.GetAssertionAsync(aParams));
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

            await Assert.ThrowsAsync<NotAllowedError>(() => sutProvider.Sut.GetAssertionAsync(aParams));
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
            sutProvider.GetDependency<IFido2UserInterface>().PickCredentialAsync(Arg.Any<Fido2PickCredentialParams>()).Returns(new Fido2PickCredentialResult {
                CipherId = ciphers[0].Id,
                UserVerified = false
            });

            await sutProvider.Sut.GetAssertionAsync(aParams);

            await sutProvider.GetDependency<IFido2UserInterface>().Received().PickCredentialAsync(Arg.Is<Fido2PickCredentialParams>(
                (pickCredentialParams) => pickCredentialParams.CipherIds.SequenceEqual(ciphers.Select((cipher) => cipher.Id))
            ));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        public async Task GetAssertionAsync_AsksForDiscoverableCredentials_ParamsDoesNotContainAllowedCredentialsList(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorGetAssertionParams aParams)
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
            sutProvider.GetDependency<IFido2UserInterface>().PickCredentialAsync(Arg.Any<Fido2PickCredentialParams>()).Returns(new Fido2PickCredentialResult {
                CipherId = discoverableCiphers[0].Id,
                UserVerified = false
            });

            await sutProvider.Sut.GetAssertionAsync(aParams);

            await sutProvider.GetDependency<IFido2UserInterface>().Received().PickCredentialAsync(Arg.Is<Fido2PickCredentialParams>(
                (pickCredentialParams) => pickCredentialParams.CipherIds.SequenceEqual(discoverableCiphers.Select((cipher) => cipher.Id))
            ));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        // Spec: Prompt the user to select a public key credential source `selectedCredential` from `credentialOptions`.
        //       If requireUserVerification is true, the authorization gesture MUST include user verification.
        public async Task GetAssertionAsync_RequestsUserVerification_ParamsRequireUserVerification(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorGetAssertionParams aParams) {
            var credentialIds = new[] { Guid.NewGuid(), Guid.NewGuid() };
            List<CipherView> ciphers = [ 
                CreateCipherView(credentialIds[0].ToString(), "bitwarden.com", false),
                CreateCipherView(credentialIds[1].ToString(), "bitwarden.com", true)
            ];
            var discoverableCiphers = ciphers.Where((cipher) => cipher.Login.MainFido2Credential.IsDiscoverable).ToList();
            aParams.RpId = "bitwarden.com";
            aParams.AllowCredentialDescriptorList = null;
            aParams.RequireUserVerification = true;
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(ciphers);
            sutProvider.GetDependency<IFido2UserInterface>().PickCredentialAsync(Arg.Any<Fido2PickCredentialParams>()).Returns(new Fido2PickCredentialResult {
                CipherId = discoverableCiphers[0].Id,
                UserVerified = false
            });

            await sutProvider.Sut.GetAssertionAsync(aParams);

            await sutProvider.GetDependency<IFido2UserInterface>().Received().PickCredentialAsync(Arg.Is<Fido2PickCredentialParams>(
                (pickCredentialParams) => pickCredentialParams.UserVerification == true
            ));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        // Spec: Prompt the user to select a public key credential source `selectedCredential` from `credentialOptions`.
        //       If requireUserPresence is true, the authorization gesture MUST include a test of user presence.
        // Comment: User presence in implied by the UI returning a credential.
        public async Task GetAssertionAsync_DoesNotRequestUserVerification_ParamsDoNotRequireUserVerification(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorGetAssertionParams aParams) {
            var credentialIds = new[] { Guid.NewGuid(), Guid.NewGuid() };
            List<CipherView> ciphers = [ 
                CreateCipherView(credentialIds[0].ToString(), "bitwarden.com", false),
                CreateCipherView(credentialIds[1].ToString(), "bitwarden.com", true)
            ];
            var discoverableCiphers = ciphers.Where((cipher) => cipher.Login.MainFido2Credential.IsDiscoverable).ToList();
            aParams.RpId = "bitwarden.com";
            aParams.AllowCredentialDescriptorList = null;
            aParams.RequireUserVerification = false;
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(ciphers);
            sutProvider.GetDependency<IFido2UserInterface>().PickCredentialAsync(Arg.Any<Fido2PickCredentialParams>()).Returns(new Fido2PickCredentialResult {
                CipherId = discoverableCiphers[0].Id,
                UserVerified = false
            });

            await sutProvider.Sut.GetAssertionAsync(aParams);

            await sutProvider.GetDependency<IFido2UserInterface>().Received().PickCredentialAsync(Arg.Is<Fido2PickCredentialParams>(
                (pickCredentialParams) => pickCredentialParams.UserVerification == false
            ));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        // Spec: If the user does not consent, return an error code equivalent to "NotAllowedError" and terminate the operation.
        public async Task GetAssertionAsync_ThrowsNotAllowed_UserDoesNotConsent(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorGetAssertionParams aParams) {
            var credentialIds = new[] { Guid.NewGuid(), Guid.NewGuid() };
            List<CipherView> ciphers = [ 
                CreateCipherView(credentialIds[0].ToString(), "bitwarden.com", false),
                CreateCipherView(credentialIds[1].ToString(), "bitwarden.com", true)
            ];
            var discoverableCiphers = ciphers.Where((cipher) => cipher.Login.MainFido2Credential.IsDiscoverable).ToList();
            aParams.RpId = "bitwarden.com";
            aParams.AllowCredentialDescriptorList = null;
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(ciphers);
            sutProvider.GetDependency<IFido2UserInterface>().PickCredentialAsync(Arg.Any<Fido2PickCredentialParams>()).Returns(new Fido2PickCredentialResult {
                CipherId = null,
                UserVerified = false
            });

            await Assert.ThrowsAsync<NotAllowedError>(() => sutProvider.Sut.GetAssertionAsync(aParams));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        // Spec: If the user does not consent, return an error code equivalent to "NotAllowedError" and terminate the operation.
        public async Task GetAssertionAsync_ThrowsNotAllowed_NoUserVerificationForCipherWithReprompt(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorGetAssertionParams aParams) {
            var credentialIds = new[] { Guid.NewGuid(), Guid.NewGuid() };
            List<CipherView> ciphers = [ 
                CreateCipherView(credentialIds[0].ToString(), "bitwarden.com", false),
                CreateCipherView(credentialIds[1].ToString(), "bitwarden.com", true)
            ];
            ciphers[0].Reprompt = CipherRepromptType.Password;
            var discoverableCiphers = ciphers.Where((cipher) => cipher.Login.MainFido2Credential.IsDiscoverable).ToList();
            aParams.RpId = "bitwarden.com";
            aParams.AllowCredentialDescriptorList = null;
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(ciphers);
            sutProvider.GetDependency<IFido2UserInterface>().PickCredentialAsync(Arg.Any<Fido2PickCredentialParams>()).Returns(new Fido2PickCredentialResult {
                CipherId = ciphers[0].Id,
                UserVerified = false
            });

            await Assert.ThrowsAsync<NotAllowedError>(() => sutProvider.Sut.GetAssertionAsync(aParams));
        }

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
