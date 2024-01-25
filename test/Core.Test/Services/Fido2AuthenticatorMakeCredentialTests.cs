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
        #region invalid input parameters

        // Spec: Check if at least one of the specified combinations of PublicKeyCredentialType and cryptographic parameters in credTypesAndPubKeyAlgs is supported. If not, return an error code equivalent to "NotSupportedError" and terminate the operation.
        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        public async Task MakeCredentialAsync_ThrowsNotSupported_NoSupportedAlgorithm(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorMakeCredentialParams mParams)
        {
            mParams.CredTypesAndPubKeyAlgs = [
                new PublicKeyCredentialAlgorithmDescriptor {
                    Type = "public-key",
                    Algorithm = -257 // RS256 which we do not support
                }
            ];

            await Assert.ThrowsAsync<NotSupportedError>(() => sutProvider.Sut.MakeCredentialAsync(mParams));
        }

        #endregion

        #region vault contains excluded credential

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        // Spec: collect an authorization gesture confirming user consent for creating a new credential.
        // Deviation: Consent is not asked and the user is simply informed of the situation.
        public async Task MakeCredentialAsync_InformsUser_ExcludedCredentialFound(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorMakeCredentialParams mParams)
        {
            var credentialIds = new[] { Guid.NewGuid(), Guid.NewGuid() };
            List<CipherView> ciphers = [ 
                CreateCipherView(credentialIds[0].ToString(), "bitwarden.com", false),
                CreateCipherView(credentialIds[1].ToString(), "bitwarden.com", true)
            ];
            mParams.CredTypesAndPubKeyAlgs = [
                new PublicKeyCredentialAlgorithmDescriptor {
                    Type = "public-key",
                    Algorithm = -7 // ES256
                }
            ];
            mParams.RpEntity = new PublicKeyCredentialRpEntity { Id = "bitwarden.com" };
            mParams.RequireUserVerification = false;
            mParams.ExcludeCredentialDescriptorList = [
                new PublicKeyCredentialDescriptor {
                    Type = "public-key",
                    Id = credentialIds[0].ToByteArray()
                }
            ];
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(ciphers);

            try
            { 
                await sutProvider.Sut.MakeCredentialAsync(mParams);
            }
            catch {}

            await sutProvider.GetDependency<IFido2UserInterface>().Received().InformExcludedCredential(Arg.Is<string[]>(
                (c) => c.SequenceEqual(new string[] { ciphers[0].Id })
            ));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        // Spec: return an error code equivalent to "NotAllowedError" and terminate the operation.
        public async Task MakeCredentialAsync_ThrowsNotAllowed_ExcludedCredentialFound(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorMakeCredentialParams mParams)
        {
            var credentialIds = new[] { Guid.NewGuid(), Guid.NewGuid() };
            List<CipherView> ciphers = [ 
                CreateCipherView(credentialIds[0].ToString(), "bitwarden.com", false),
                CreateCipherView(credentialIds[1].ToString(), "bitwarden.com", true)
            ];
            mParams.CredTypesAndPubKeyAlgs = [
                new PublicKeyCredentialAlgorithmDescriptor {
                    Type = "public-key",
                    Algorithm = -7 // ES256
                }
            ];
            mParams.RpEntity = new PublicKeyCredentialRpEntity { Id = "bitwarden.com" };
            mParams.RequireUserVerification = false;
            mParams.ExcludeCredentialDescriptorList = [
                new PublicKeyCredentialDescriptor {
                    Type = "public-key",
                    Id = credentialIds[0].ToByteArray()
                }
            ];
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(ciphers);

            await Assert.ThrowsAsync<NotAllowedError>(() => sutProvider.Sut.MakeCredentialAsync(mParams));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        // Deviation: Organization ciphers are not checked against excluded credentials, even if the user has access to them.
        public async Task MakeCredentialAsync_DoesNotInformAboutExcludedCredential_ExcludedCredentialBelongsToOrganization(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorMakeCredentialParams mParams)
        {
            var credentialIds = new[] { Guid.NewGuid(), Guid.NewGuid() };
            List<CipherView> ciphers = [ 
                CreateCipherView(credentialIds[0].ToString(), "bitwarden.com", false),
                CreateCipherView(credentialIds[1].ToString(), "bitwarden.com", true)
            ];
            ciphers[0].OrganizationId = "someOrganizationId";
            mParams.CredTypesAndPubKeyAlgs = [
                new PublicKeyCredentialAlgorithmDescriptor {
                    Type = "public-key",
                    Algorithm = -7 // ES256
                }
            ];
            mParams.RpEntity = new PublicKeyCredentialRpEntity { Id = "bitwarden.com" };
            mParams.RequireUserVerification = false;
            mParams.ExcludeCredentialDescriptorList = [
                new PublicKeyCredentialDescriptor {
                    Type = "public-key",
                    Id = credentialIds[0].ToByteArray()
                }
            ];
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(ciphers);

            try 
            {
                await sutProvider.Sut.MakeCredentialAsync(mParams);
            } catch {}

            await sutProvider.GetDependency<IFido2UserInterface>().DidNotReceive().InformExcludedCredential(Arg.Any<string[]>());
        }

        #endregion

        #region credential creation

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        public async Task MakeCredentialAsync_RequestsUserVerification_ParamsRequireUserVerification(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorMakeCredentialParams mParams)
        {
            // Common Arrange
            var credentialIds = new[] { Guid.NewGuid(), Guid.NewGuid() };
            List<CipherView> ciphers = [
                CreateCipherView(credentialIds[0].ToString(), "bitwarden.com", false),
                CreateCipherView(credentialIds[1].ToString(), "bitwarden.com", true)
            ];
            mParams.CredTypesAndPubKeyAlgs = [
                new PublicKeyCredentialAlgorithmDescriptor {
                    Type = "public-key",
                    Algorithm = -7 // ES256
                }
            ];
            mParams.RpEntity = new PublicKeyCredentialRpEntity { Id = "bitwarden.com" };
            mParams.RequireUserVerification = false;
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(ciphers);

            // Arrange
            mParams.RequireUserVerification = true;

            // Act
            await sutProvider.Sut.MakeCredentialAsync(mParams);

            // Assert
            await sutProvider.GetDependency<IFido2UserInterface>().Received().ConfirmNewCredentialAsync(Arg.Is<Fido2ConfirmNewCredentialParams>(
                (p) => p.UserVerification == true
            ));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        public async Task MakeCredentialAsync_DoesNotRequestUserVerification_ParamsDoNotRequireUserVerification(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorMakeCredentialParams mParams)
        {
            // Common Arrange
            var credentialIds = new[] { Guid.NewGuid(), Guid.NewGuid() };
            List<CipherView> ciphers = [
                CreateCipherView(credentialIds[0].ToString(), "bitwarden.com", false),
                CreateCipherView(credentialIds[1].ToString(), "bitwarden.com", true)
            ];
            mParams.CredTypesAndPubKeyAlgs = [
                new PublicKeyCredentialAlgorithmDescriptor {
                    Type = "public-key",
                    Algorithm = -7 // ES256
                }
            ];
            mParams.RpEntity = new PublicKeyCredentialRpEntity { Id = "bitwarden.com" };
            mParams.RequireUserVerification = false;
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(ciphers);

            // Arrange
            mParams.RequireUserVerification = false;

            // Act
            await sutProvider.Sut.MakeCredentialAsync(mParams);

            // Assert
            await sutProvider.GetDependency<IFido2UserInterface>().Received().ConfirmNewCredentialAsync(Arg.Is<Fido2ConfirmNewCredentialParams>(
                (p) => p.UserVerification == false
            ));
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
