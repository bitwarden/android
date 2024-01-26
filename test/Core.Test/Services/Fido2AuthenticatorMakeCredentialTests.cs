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
using System.Security.Policy;
using NSubstitute.Extensions;

namespace Bit.Core.Test.Services
{
    public class Fido2AuthenticatorMakeCredentialTests : IDisposable
    {
        private Cipher _encryptedCipher;

        public Fido2AuthenticatorMakeCredentialTests() {
            var cryptoServiceMock = Substitute.For<ICryptoService>();
            ServiceContainer.Register(typeof(CryptoService), cryptoServiceMock);
            
            _encryptedCipher = CreateCipher();
        }

        public void Dispose()
        {
            ServiceContainer.Reset();
        }
        
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
                CreateCipherView(true, credentialIds[0].ToString(), "bitwarden.com", false),
                CreateCipherView(true, credentialIds[1].ToString(), "bitwarden.com", true)
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
                CreateCipherView(true, credentialIds[0].ToString(), "bitwarden.com", false),
                CreateCipherView(true, credentialIds[1].ToString(), "bitwarden.com", true)
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
                CreateCipherView(false, credentialIds[0].ToString(), "bitwarden.com", false),
                CreateCipherView(false, credentialIds[1].ToString(), "bitwarden.com", true)
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
                CreateCipherView(false, credentialIds[0].ToString(), "bitwarden.com", false),
                CreateCipherView(false, credentialIds[1].ToString(), "bitwarden.com", true)
            ];
            mParams.CredTypesAndPubKeyAlgs = [
                new PublicKeyCredentialAlgorithmDescriptor {
                    Type = "public-key",
                    Algorithm = -7 // ES256
                }
            ];
            mParams.RpEntity = new PublicKeyCredentialRpEntity { Id = "bitwarden.com" };
            mParams.RequireUserVerification = false;
            sutProvider.GetDependency<ICryptoFunctionService>().EcdsaGenerateKeyPairAsync(Arg.Any<CryptoEcdsaAlgorithm>())
                .Returns((RandomBytes(32), RandomBytes(32)));
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(ciphers);
            sutProvider.GetDependency<IFido2UserInterface>().ConfirmNewCredentialAsync(Arg.Any<Fido2ConfirmNewCredentialParams>()).Returns(new Fido2ConfirmNewCredentialResult {
                CipherId = null,
                UserVerified = false
            });

            // Arrange
            mParams.RequireUserVerification = true;

            // Act
            await Assert.ThrowsAsync<NotAllowedError>(() => sutProvider.Sut.MakeCredentialAsync(mParams));

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
                CreateCipherView(false, credentialIds[0].ToString(), "bitwarden.com", false),
                CreateCipherView(false, credentialIds[1].ToString(), "bitwarden.com", true)
            ];
            mParams.CredTypesAndPubKeyAlgs = [
                new PublicKeyCredentialAlgorithmDescriptor {
                    Type = "public-key",
                    Algorithm = -7 // ES256
                }
            ];
            mParams.RpEntity = new PublicKeyCredentialRpEntity { Id = "bitwarden.com" };
            sutProvider.GetDependency<ICryptoFunctionService>().EcdsaGenerateKeyPairAsync(Arg.Any<CryptoEcdsaAlgorithm>())
                .Returns((RandomBytes(32), RandomBytes(32)));
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(ciphers);

            // Arrange
            mParams.RequireUserVerification = false;

            // Act
            await Assert.ThrowsAsync<NotAllowedError>(() => sutProvider.Sut.MakeCredentialAsync(mParams));

            // Assert
            await sutProvider.GetDependency<IFido2UserInterface>().Received().ConfirmNewCredentialAsync(Arg.Is<Fido2ConfirmNewCredentialParams>(
                (p) => p.UserVerification == false
            ));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        public async Task MakeCredentialAsync_RequestsUserVerification_RequestConfirmedByUser(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorMakeCredentialParams mParams)
        {
            // Common Arrange
            mParams.CredTypesAndPubKeyAlgs = [
                new PublicKeyCredentialAlgorithmDescriptor {
                    Type = "public-key",
                    Algorithm = -7 // ES256
                }
            ];
            mParams.RpEntity = new PublicKeyCredentialRpEntity { Id = "bitwarden.com" };
            mParams.RequireUserVerification = false;
            sutProvider.GetDependency<ICryptoFunctionService>().EcdsaGenerateKeyPairAsync(Arg.Any<CryptoEcdsaAlgorithm>())
                .Returns((RandomBytes(32), RandomBytes(32)));
            _encryptedCipher.Key = null;
            _encryptedCipher.Attachments = [];

            // Arrange
            mParams.RequireResidentKey = false;
            sutProvider.GetDependency<ICipherService>().EncryptAsync(Arg.Any<CipherView>()).Returns(_encryptedCipher);
            sutProvider.GetDependency<ICipherService>().GetAsync(Arg.Is(_encryptedCipher.Id)).Returns(_encryptedCipher);
            sutProvider.GetDependency<IFido2UserInterface>().ConfirmNewCredentialAsync(Arg.Any<Fido2ConfirmNewCredentialParams>()).Returns(new Fido2ConfirmNewCredentialResult {
                CipherId = _encryptedCipher.Id,
                UserVerified = false
            });

            // Act
            await sutProvider.Sut.MakeCredentialAsync(mParams);

            // Assert
            await sutProvider.GetDependency<ICipherService>().Received().EncryptAsync(Arg.Is<CipherView>(
                (c) => 
                    c.Login.MainFido2Credential.KeyType == "public-key" &&
                    c.Login.MainFido2Credential.KeyAlgorithm == "ECDSA" &&
                    c.Login.MainFido2Credential.KeyCurve == "P-256" &&
                    c.Login.MainFido2Credential.RpId == mParams.RpEntity.Id &&
                    c.Login.MainFido2Credential.RpName == mParams.RpEntity.Name &&
                    c.Login.MainFido2Credential.UserHandle == CoreHelpers.Base64UrlEncode(mParams.UserEntity.Id) &&
                    c.Login.MainFido2Credential.UserName == mParams.UserEntity.Name &&
                    c.Login.MainFido2Credential.CounterValue == 0 &&
                    // c.Login.MainFido2Credential.UserDisplayName == mParams.UserEntity.DisplayName &&
                    c.Login.MainFido2Credential.DiscoverableValue == false
            ));
            await sutProvider.GetDependency<ICipherService>().Received().SaveWithServerAsync(_encryptedCipher);
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        // Spec: If the user does not consent or if user verification fails, return an error code equivalent to "NotAllowedError" and terminate the operation.
        public async Task MakeCredentialAsync_ThrowsNotAllowed_RequestNotConfirmedByUser(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorMakeCredentialParams mParams)
        {
            // Common Arrange
            mParams.CredTypesAndPubKeyAlgs = [
                new PublicKeyCredentialAlgorithmDescriptor {
                    Type = "public-key",
                    Algorithm = -7 // ES256
                }
            ];
            mParams.RpEntity = new PublicKeyCredentialRpEntity { Id = "bitwarden.com" };
            mParams.RequireUserVerification = false;
            sutProvider.GetDependency<ICryptoFunctionService>().EcdsaGenerateKeyPairAsync(Arg.Any<CryptoEcdsaAlgorithm>())
                .Returns((RandomBytes(32), RandomBytes(32)));

            // Arrange
            sutProvider.GetDependency<ICipherService>().GetAsync(Arg.Is(_encryptedCipher.Id)).Returns(_encryptedCipher);
            sutProvider.GetDependency<IFido2UserInterface>().ConfirmNewCredentialAsync(Arg.Any<Fido2ConfirmNewCredentialParams>()).Returns(new Fido2ConfirmNewCredentialResult {
                CipherId = null,
                UserVerified = false
            });

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => sutProvider.Sut.MakeCredentialAsync(mParams));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        public async Task MakeCredentialAsync_ThrowsNotAllowed_NoUserVerificationWhenRequiredByParams(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorMakeCredentialParams mParams)
        {
            // Common Arrange
            mParams.CredTypesAndPubKeyAlgs = [
                new PublicKeyCredentialAlgorithmDescriptor {
                    Type = "public-key",
                    Algorithm = -7 // ES256
                }
            ];
            mParams.RpEntity = new PublicKeyCredentialRpEntity { Id = "bitwarden.com" };
            mParams.RequireUserVerification = true;
            sutProvider.GetDependency<ICryptoFunctionService>().EcdsaGenerateKeyPairAsync(Arg.Any<CryptoEcdsaAlgorithm>())
                .Returns((RandomBytes(32), RandomBytes(32)));

            // Arrange
            sutProvider.GetDependency<ICipherService>().GetAsync(Arg.Is(_encryptedCipher.Id)).Returns(_encryptedCipher);
            sutProvider.GetDependency<IFido2UserInterface>().ConfirmNewCredentialAsync(Arg.Any<Fido2ConfirmNewCredentialParams>()).Returns(new Fido2ConfirmNewCredentialResult {
                CipherId = _encryptedCipher.Id,
                UserVerified = false
            });

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => sutProvider.Sut.MakeCredentialAsync(mParams));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        public async Task MakeCredentialAsync_ThrowsNotAllowed_NoUserVerificationForCipherWithReprompt(SutProvider<Fido2AuthenticatorService> sutProvider, Fido2AuthenticatorMakeCredentialParams mParams)
        {
            // Common Arrange
            mParams.CredTypesAndPubKeyAlgs = [
                new PublicKeyCredentialAlgorithmDescriptor {
                    Type = "public-key",
                    Algorithm = -7 // ES256
                }
            ];
            mParams.RpEntity = new PublicKeyCredentialRpEntity { Id = "bitwarden.com" };
            mParams.RequireUserVerification = false;
            sutProvider.GetDependency<ICryptoFunctionService>().EcdsaGenerateKeyPairAsync(Arg.Any<CryptoEcdsaAlgorithm>())
                .Returns((RandomBytes(32), RandomBytes(32)));
            _encryptedCipher.Reprompt = CipherRepromptType.Password;

            // Arrange
            sutProvider.GetDependency<ICipherService>().GetAsync(Arg.Is(_encryptedCipher.Id)).Returns(_encryptedCipher);
            sutProvider.GetDependency<IFido2UserInterface>().ConfirmNewCredentialAsync(Arg.Any<Fido2ConfirmNewCredentialParams>()).Returns(new Fido2ConfirmNewCredentialResult {
                CipherId = _encryptedCipher.Id,
                UserVerified = false
            });

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => sutProvider.Sut.MakeCredentialAsync(mParams));
        }

    //   /** Spec: If any error occurred while creating the new credential object, return an error code equivalent to "UnknownError" and terminate the operation. */
    //   it("should throw unkown error if creation fails", async () => {
    //     const _encryptedCipher = Symbol();
    //     userInterfaceSession.confirmNewCredential.mockResolvedValue({
    //       cipherId: existingCipher.id,
    //       userVerified: false,
    //     });
    //     cipherService.encrypt.mockResolvedValue(_encryptedCipher as unknown as Cipher);
    //     cipherService.updateWithServer.mockRejectedValue(new Error("Internal error"));

    //     const result = async () => await authenticator.makeCredential(params, tab);

    //     await expect(result).rejects.toThrowError(Fido2AuthenticatorErrorCode.Unknown);
    //   });
        
        #endregion

        private byte[] RandomBytes(int length)
        {
            var bytes = new byte[length];
            new Random().NextBytes(bytes);
            return bytes;
        }

        #nullable enable
        private CipherView CreateCipherView(bool? withFido2Credential, string? credentialId = null, string? rpId = null, bool? discoverable = null)
        {
            return new CipherView {
                Type = CipherType.Login,
                Id = Guid.NewGuid().ToString(),
                Reprompt = CipherRepromptType.None,
                Login = new LoginView {
                    Fido2Credentials = withFido2Credential.HasValue && withFido2Credential.Value ? new List<Fido2CredentialView> {
                        new Fido2CredentialView {
                            CredentialId = credentialId ?? Guid.NewGuid().ToString(),
                            RpId = rpId ?? "bitwarden.com",
                            Discoverable = discoverable.HasValue ? discoverable.ToString() : "true",
                            UserHandleValue = RandomBytes(32),
                        }
                    } : null
                }
            };
        }

        private Cipher CreateCipher()
        {
            return new Cipher {
                Id = Guid.NewGuid().ToString(),
                Type = CipherType.Login,
                Login = new Login {}
            };
        }
    }
}
