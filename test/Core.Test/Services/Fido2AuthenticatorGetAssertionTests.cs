using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using Bit.Core.Enums;
using Bit.Core.Utilities.Fido2;
using Bit.Test.Common.AutoFixture;
using Bit.Test.Common.AutoFixture.Attributes;
using NSubstitute;
using NSubstitute.ExceptionExtensions;
using Xunit;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Linq;
using System.Security.Cryptography;

namespace Bit.Core.Test.Services
{
    public class Fido2AuthenticatorGetAssertionTests : IDisposable
    {
        private readonly string _rpId = "bitwarden.com";
        private readonly SutProvider<Fido2AuthenticatorService> _sutProvider = new SutProvider<Fido2AuthenticatorService>().Create();
        private readonly IFido2UserInterface _userInterface = Substitute.For<IFido2UserInterface>();

        private List<Guid> _credentialIds;
        private List<CipherView> _ciphers;
        private Fido2AuthenticatorGetAssertionParams _params;
        private CipherView _selectedCipher;

        /// <summary>
        /// Sets up a working environment for the tests.
        /// </summary>
        public Fido2AuthenticatorGetAssertionTests()
        {
            _credentialIds =  [ Guid.NewGuid(), Guid.NewGuid(), Guid.NewGuid(), Guid.NewGuid() ];
            _ciphers = [
                CreateCipherView(_credentialIds[0].ToString(), _rpId, false),
                CreateCipherView(_credentialIds[1].ToString(), _rpId, true),
            ];
            _selectedCipher = _ciphers[0];
            _params = CreateParams(
                rpId: _rpId, 
                allowCredentialDescriptorList: [
                    new PublicKeyCredentialDescriptor {
                        Id = _credentialIds[0].ToByteArray(),
                        Type = "public-key"
                    },
                    new PublicKeyCredentialDescriptor {
                        Id = _credentialIds[1].ToByteArray(),
                        Type = "public-key"
                    },
                ],
                requireUserVerification: false
            );
            _sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(_ciphers);
            _userInterface.PickCredentialAsync(Arg.Any<Fido2PickCredentialParams>()).Returns(new Fido2PickCredentialResult {
                CipherId = _ciphers[0].Id,
                UserVerified = false
            });
            _sutProvider.Sut.Init(_userInterface);
        }

        public void Dispose() 
        {
        }

        #region missing non-discoverable credential

        [Fact]
        // Spec: If credentialOptions is now empty, return an error code equivalent to "NotAllowedError" and terminate the operation.
        public async Task GetAssertionAsync_ThrowsNotAllowed_NoCredentialsExists()
        {
            // Arrange
            _ciphers.Clear();

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => _sutProvider.Sut.GetAssertionAsync(_params));
        }

        [Fact]
        public async Task GetAssertionAsync_ThrowsNotAllowed_CredentialExistsButRpIdDoesNotMatch()
        {
            // Arrange
            _params.RpId = "mismatch-rpid";

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => _sutProvider.Sut.GetAssertionAsync(_params));
        }

        #endregion

        #region vault contains credential

        [Fact]
        public async Task GetAssertionAsync_AsksForAllCredentials_ParamsContainsAllowedCredentialsList()
        {
            // Arrange
            _params.AllowCredentialDescriptorList = [
                new PublicKeyCredentialDescriptor {
                    Id = _credentialIds[0].ToByteArray(),
                    Type = "public-key"
                },
                new PublicKeyCredentialDescriptor {
                    Id = _credentialIds[1].ToByteArray(),
                    Type = "public-key"
                },
            ];

            // Act
            await _sutProvider.Sut.GetAssertionAsync(_params);

            // Assert
            await _userInterface.Received().PickCredentialAsync(Arg.Is<Fido2PickCredentialParams>(
                (pickCredentialParams) => pickCredentialParams.CipherIds.SequenceEqual(_ciphers.Select((cipher) => cipher.Id))
            ));
        }

        [Fact]
        public async Task GetAssertionAsync_AsksForDiscoverableCredentials_ParamsDoesNotContainAllowedCredentialsList()
        {
            // Arrange
            _params.AllowCredentialDescriptorList = null;
            var discoverableCiphers = _ciphers.Where((cipher) => cipher.Login.MainFido2Credential.DiscoverableValue).ToList();
            _userInterface.PickCredentialAsync(Arg.Any<Fido2PickCredentialParams>()).Returns(new Fido2PickCredentialResult {
                CipherId = discoverableCiphers[0].Id,
                UserVerified = false
            });

            // Act
            await _sutProvider.Sut.GetAssertionAsync(_params);

            // Assert
            await _userInterface.Received().PickCredentialAsync(Arg.Is<Fido2PickCredentialParams>(
                (pickCredentialParams) => pickCredentialParams.CipherIds.SequenceEqual(discoverableCiphers.Select((cipher) => cipher.Id))
            ));
        }

        [Fact]
        // Spec: Prompt the user to select a public key credential source `selectedCredential` from `credentialOptions`.
        //       If requireUserVerification is true, the authorization gesture MUST include user verification.
        public async Task GetAssertionAsync_RequestsUserVerification_ParamsRequireUserVerification() {
            // Arrange
            _params.RequireUserVerification = true;
            _userInterface.PickCredentialAsync(Arg.Any<Fido2PickCredentialParams>()).Returns(new Fido2PickCredentialResult {
                CipherId = _ciphers[0].Id,
                UserVerified = true
            });

            // Act
            await _sutProvider.Sut.GetAssertionAsync(_params);

            // Assert
            await _userInterface.Received().PickCredentialAsync(Arg.Is<Fido2PickCredentialParams>(
                (pickCredentialParams) => pickCredentialParams.UserVerification == true
            ));
        }

        [Fact]
        // Spec: Prompt the user to select a public key credential source `selectedCredential` from `credentialOptions`.
        //       If `requireUserPresence` is true, the authorization gesture MUST include a test of user presence.
        // Comment: User presence is implied by the UI returning a credential.
        public async Task GetAssertionAsync_DoesNotRequestUserVerification_ParamsDoNotRequireUserVerification() {
            // Arrange
            _params.RequireUserVerification = false;

            // Act
            await _sutProvider.Sut.GetAssertionAsync(_params);

            // Assert
            await _userInterface.Received().PickCredentialAsync(Arg.Is<Fido2PickCredentialParams>(
                (pickCredentialParams) => pickCredentialParams.UserVerification == false
            ));
        }

        [Fact]
        // Spec: If the user does not consent, return an error code equivalent to "NotAllowedError" and terminate the operation.
        public async Task GetAssertionAsync_ThrowsNotAllowed_UserDoesNotConsent() {
            // Arrange
            _userInterface.PickCredentialAsync(Arg.Any<Fido2PickCredentialParams>()).Returns(new Fido2PickCredentialResult {
                CipherId = null,
                UserVerified = false
            });

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => _sutProvider.Sut.GetAssertionAsync(_params));
        }

        [Fact]
        // Spec: If the user does not consent, return an error code equivalent to "NotAllowedError" and terminate the operation.
        public async Task GetAssertionAsync_ThrowsNotAllowed_NoUserVerificationWhenRequired() {
            // Arrange
            _params.RequireUserVerification = true;
            _userInterface.PickCredentialAsync(Arg.Any<Fido2PickCredentialParams>()).Returns(new Fido2PickCredentialResult {
                CipherId = _selectedCipher.Id,
                UserVerified = false
            });

            // Act and assert
            await Assert.ThrowsAsync<NotAllowedError>(() => _sutProvider.Sut.GetAssertionAsync(_params));
        }

        [Fact]
        // Spec: If the user does not consent, return an error code equivalent to "NotAllowedError" and terminate the operation.
        public async Task GetAssertionAsync_ThrowsNotAllowed_NoUserVerificationForCipherWithReprompt() {
            // Arrange
            _selectedCipher.Reprompt = CipherRepromptType.Password;
            _params.RequireUserVerification = false;
            _userInterface.PickCredentialAsync(Arg.Any<Fido2PickCredentialParams>()).Returns(new Fido2PickCredentialResult {
                CipherId = _selectedCipher.Id,
                UserVerified = false
            });

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => _sutProvider.Sut.GetAssertionAsync(_params));
        }

        #endregion

        #region assertion of credential

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        // Spec: Increment the credential associated signature counter
        public async Task GetAssertionAsync_IncrementsCounter_CounterIsLargerThanZero(Cipher encryptedCipher) {
            // Arrange
            _selectedCipher.Login.MainFido2Credential.CounterValue = 9000;
            _sutProvider.GetDependency<ICipherService>().EncryptAsync(_selectedCipher).Returns(encryptedCipher);
            
            // Act
            await _sutProvider.Sut.GetAssertionAsync(_params);

            // Assert
            await _sutProvider.GetDependency<ICipherService>().Received().SaveWithServerAsync(encryptedCipher);
            await _sutProvider.GetDependency<ICipherService>().Received().EncryptAsync(Arg.Is<CipherView>(
                (cipher) => cipher.Login.MainFido2Credential.CounterValue == 9001
            ));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        // Spec: Increment the credential associated signature counter
        public async Task GetAssertionAsync_DoesNotIncrementsCounter_CounterIsZero(Cipher encryptedCipher) {
            // Arrange
            _selectedCipher.Login.MainFido2Credential.CounterValue = 0;
            _sutProvider.GetDependency<ICipherService>().EncryptAsync(_selectedCipher).Returns(encryptedCipher);
            
            // Act
            await _sutProvider.Sut.GetAssertionAsync(_params);

            // Assert
            await _sutProvider.GetDependency<ICipherService>().Received().SaveWithServerAsync(encryptedCipher);
            await _sutProvider.GetDependency<ICipherService>().Received().EncryptAsync(Arg.Is<CipherView>(
                (cipher) => cipher.Login.MainFido2Credential.CounterValue == 0
            ));
        }

        [Fact]
        public async Task GetAssertionAsync_ReturnsAssertion() {
            // Arrange
            var keyPair = GenerateKeyPair();
            var rpIdHashMock = RandomBytes(32);
            _params.Hash = RandomBytes(32);
            _params.RequireUserVerification = true;
            _selectedCipher.Login.MainFido2Credential.CounterValue = 9000;
            _selectedCipher.Login.MainFido2Credential.KeyValue = CoreHelpers.Base64UrlEncode(keyPair.ExportPkcs8PrivateKey());
            _sutProvider.GetDependency<ICryptoFunctionService>().HashAsync(_params.RpId, CryptoHashAlgorithm.Sha256).Returns(rpIdHashMock);
            _userInterface.PickCredentialAsync(Arg.Any<Fido2PickCredentialParams>()).Returns(new Fido2PickCredentialResult {
                CipherId = _selectedCipher.Id,
                UserVerified = true
            });
            
            // Act
            var result = await _sutProvider.Sut.GetAssertionAsync(_params);

            // Assert
            var authData = result.AuthenticatorData;
            var rpIdHash = authData.Take(32);
            var flags = authData.Skip(32).Take(1);
            var counter = authData.Skip(33).Take(4);

            Assert.Equal(Guid.Parse(_selectedCipher.Login.MainFido2Credential.CredentialId).ToByteArray(), result.SelectedCredential.Id);
            Assert.Equal(CoreHelpers.Base64UrlDecode(_selectedCipher.Login.MainFido2Credential.UserHandle), result.SelectedCredential.UserHandle);
            Assert.Equal(rpIdHashMock, rpIdHash);
            Assert.Equal(new byte[] { 0b00011101 }, flags); // UP = true, UV = true, BS = true, BE = true
            Assert.Equal(new byte[] { 0, 0, 0x23, 0x29 }, counter); // 9001 in binary big-endian format
            Assert.True(keyPair.VerifyData(authData.Concat(_params.Hash).ToArray(), result.Signature, HashAlgorithmName.SHA256, DSASignatureFormat.Rfc3279DerSequence), "Signature verification failed");
        }

        [Fact]
        public async Task GetAssertionAsync_DoesNotAskForConfirmation_ParamsContainsOneAllowedCredentialAndUserPresenceIsFalse()
        {
            // Arrange
            var rpIdHashMock = RandomBytes(32);
            _params.RequireUserPresence = false;
            _params.AllowCredentialDescriptorList = [
                new PublicKeyCredentialDescriptor {
                    Id = _credentialIds[0].ToByteArray(),
                    Type = "public-key"
                },
            ];
            _sutProvider.GetDependency<ICryptoFunctionService>().HashAsync(_params.RpId, CryptoHashAlgorithm.Sha256).Returns(rpIdHashMock);
            
            // Act
            var result = await _sutProvider.Sut.GetAssertionAsync(_params);

            // Assert
            await _userInterface.DidNotReceive().PickCredentialAsync(Arg.Any<Fido2PickCredentialParams>());
            var authData = result.AuthenticatorData;
            var flags = authData.Skip(32).Take(1);
            Assert.Equal(new byte[] { 0b00011000 }, flags); // UP = false, UV = false, BE = true, BS = true
        }

        [Fact]
        public async Task GetAssertionAsync_ThrowsUnknownError_SaveFails() {
            // Arrange
            _sutProvider.GetDependency<ICipherService>().SaveWithServerAsync(Arg.Any<Cipher>()).Throws(new Exception());

            // Act & Assert
            await Assert.ThrowsAsync<UnknownError>(() => _sutProvider.Sut.GetAssertionAsync(_params));
        }

        #endregion

        private byte[] RandomBytes(int length)
        {
            var bytes = new byte[length];
            new Random().NextBytes(bytes);
            return bytes;
        }

        private ECDsa GenerateKeyPair()
        {
            var dsa = ECDsa.Create();
            dsa.GenerateKey(ECCurve.NamedCurves.nistP256);

            return dsa;
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
                            KeyValue = "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgO4wC7AlY4eJP7uedRUJGYsAIJAd6gN1Vp7uJh6xXAp6hRANCAARGvr56F_t27DEG1Tzl-qJRhrTUtC7jOEbasAEEZcE3TiMqoWCan0sxKDPylhRYk-1qyrBC_feN1UtGWH57sROa"
                        }
                    }
                }
            };
        }

        private Fido2AuthenticatorGetAssertionParams CreateParams(string? rpId = null, byte[]? hash = null, PublicKeyCredentialDescriptor[]? allowCredentialDescriptorList = null, bool? requireUserPresence = null, bool? requireUserVerification = null)
        {
            return new Fido2AuthenticatorGetAssertionParams {
                RpId = rpId ?? "bitwarden.com",
                Hash = hash ?? RandomBytes(32),
                AllowCredentialDescriptorList = allowCredentialDescriptorList ?? null,
                RequireUserPresence = requireUserPresence ?? true,
                RequireUserVerification = requireUserPresence ?? false
            };
        }
    }
}
