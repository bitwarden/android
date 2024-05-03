using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Cryptography;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.Core.Utilities.Fido2;
using Bit.Test.Common.AutoFixture;
using Bit.Test.Common.AutoFixture.Attributes;
using NSubstitute;
using NSubstitute.ExceptionExtensions;
using Xunit;

namespace Bit.Core.Test.Services
{
    public class Fido2AuthenticatorGetAssertionTests : IDisposable
    {
        private readonly string _rpId = "bitwarden.com";
        private readonly SutProvider<Fido2AuthenticatorService> _sutProvider = new SutProvider<Fido2AuthenticatorService>().Create();
        private readonly IFido2GetAssertionUserInterface _userInterface = Substitute.For<IFido2GetAssertionUserInterface>();

        private List<string> _credentialIds;
        private List<byte[]> _rawCredentialIds;
        private List<CipherView> _ciphers;
        private Fido2AuthenticatorGetAssertionParams _params;
        private CipherView _selectedCipher;
        private string _selectedCipherCredentialId;
        private byte[] _selectedCipherRawCredentialId;

        /// <summary>
        /// Sets up a working environment for the tests.
        /// </summary>
        public Fido2AuthenticatorGetAssertionTests()
        {
            _credentialIds = new List<string>
            {
                "2a346a27-02c5-4967-ae9e-8a090a1a8ef3",
                "924e812b-540e-445f-a2fc-b392a1bf9f27",
                "547d7aea-0d0e-493c-bf86-d8587e730dc1",
                "c07c71c4-030f-4e24-b284-c853aad72e2b"
            };
            _rawCredentialIds = new List<byte[]>
            {
                new byte[] { 0x2a, 0x34, 0x6a, 0x27, 0x02, 0xc5, 0x49, 0x67, 0xae, 0x9e, 0x8a, 0x09, 0x0a, 0x1a, 0x8e, 0xf3 },
                new byte[] { 0x92, 0x4e, 0x81, 0x2b, 0x54, 0x0e, 0x44, 0x5f, 0xa2, 0xfc, 0xb3, 0x92, 0xa1, 0xbf, 0x9f, 0x27 },
                new byte[] { 0x54, 0x7d, 0x7a, 0xea, 0x0d, 0x0e, 0x49, 0x3c, 0xbf, 0x86, 0xd8, 0x58, 0x7e, 0x73, 0x0d, 0xc1 },
                new byte[] { 0xc0, 0x7c, 0x71, 0xc4, 0x03, 0x0f, 0x4e, 0x24, 0xb2, 0x84, 0xc8, 0x53, 0xaa, 0xd7, 0x2e, 0x2b }
             };
            _ciphers = new List<CipherView>
            {
                CreateCipherView(_credentialIds[0].ToString(), _rpId, false, false),
                CreateCipherView(_credentialIds[1].ToString(), _rpId, true, true),
            };
            _selectedCipher = _ciphers[0];
            _selectedCipherCredentialId = _credentialIds[0];
            _selectedCipherRawCredentialId = _rawCredentialIds[0];
            _params = CreateParams(
                rpId: _rpId,
                allowCredentialDescriptorList: new PublicKeyCredentialDescriptor[]
                {
                    new PublicKeyCredentialDescriptor
                    {
                        Id = _rawCredentialIds[0],
                        Type = Constants.DefaultFido2CredentialType
                    },
                    new PublicKeyCredentialDescriptor
                    {
                        Id = _rawCredentialIds[1],
                        Type = Constants.DefaultFido2CredentialType
                    },
                },
                userVerificationPreference: Fido2UserVerificationPreference.Discouraged
            );
            _sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(_ciphers);
            _userInterface.PickCredentialAsync(Arg.Any<Fido2GetAssertionUserInterfaceCredential[]>()).Returns((_ciphers[0].Id, false));
            _sutProvider.GetDependency<IUserVerificationMediatorService>().CanPerformUserVerificationPreferredAsync(Arg.Any<Fido2UserVerificationOptions>()).Returns(Task.FromResult(false));
            _sutProvider.GetDependency<IUserVerificationMediatorService>().ShouldPerformMasterPasswordRepromptAsync(Arg.Any<Fido2UserVerificationOptions>()).Returns(Task.FromResult(false));
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
            await Assert.ThrowsAsync<NotAllowedError>(() => _sutProvider.Sut.GetAssertionAsync(_params, _userInterface));
        }

        [Fact]
        public async Task GetAssertionAsync_ThrowsNotAllowed_CredentialExistsButRpIdDoesNotMatch()
        {
            // Arrange
            _params.RpId = "mismatch-rpid";

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => _sutProvider.Sut.GetAssertionAsync(_params, _userInterface));
        }

        #endregion

        #region vault contains credential

        [Fact]
        public async Task GetAssertionAsync_AsksForAllCredentials_ParamsContainsAllowedCredentialsList()
        {
            // Arrange
            _params.AllowCredentialDescriptorList = new PublicKeyCredentialDescriptor[]
            {
                new PublicKeyCredentialDescriptor {
                    Id = _rawCredentialIds[0],
                    Type = Constants.DefaultFido2CredentialType
                },
                new PublicKeyCredentialDescriptor {
                    Id = _rawCredentialIds[1],
                    Type = Constants.DefaultFido2CredentialType
                },
            };

            // Act
            await _sutProvider.Sut.GetAssertionAsync(_params, _userInterface);

            // Assert
            await _userInterface.Received().PickCredentialAsync(Arg.Is<Fido2GetAssertionUserInterfaceCredential[]>(
                (credentials) => credentials.Select(c => c.CipherId).SequenceEqual(_ciphers.Select((c) => c.Id))
            ));
        }

        [Fact]
        public async Task GetAssertionAsync_AsksForDiscoverableCredentials_ParamsDoesNotContainAllowedCredentialsList()
        {
            // Arrange
            _params.AllowCredentialDescriptorList = null;
            var discoverableCiphers = _ciphers.Where((cipher) => cipher.Login.MainFido2Credential.DiscoverableValue).ToList();
            _userInterface.PickCredentialAsync(Arg.Any<Fido2GetAssertionUserInterfaceCredential[]>()).Returns((discoverableCiphers[0].Id, true));

            // Act
            await _sutProvider.Sut.GetAssertionAsync(_params, _userInterface);

            // Assert
            await _userInterface.Received().PickCredentialAsync(Arg.Is<Fido2GetAssertionUserInterfaceCredential[]>(
                (credentials) => credentials.Select(c => c.CipherId).SequenceEqual(discoverableCiphers.Select((c) => c.Id))
            ));
        }

        [Fact]
        // Spec: Prompt the user to select a public key credential source `selectedCredential` from `credentialOptions`.
        //       If UserVerificationPreference is Required, the authorization gesture MUST include user verification.
        public async Task GetAssertionAsync_RequestsUserVerification_ParamsRequireUserVerification()
        {
            // Arrange
            _params.UserVerificationPreference = Fido2UserVerificationPreference.Required;
            _userInterface.PickCredentialAsync(Arg.Any<Fido2GetAssertionUserInterfaceCredential[]>()).Returns((_ciphers[0].Id, true));

            // Act
            await _sutProvider.Sut.GetAssertionAsync(_params, _userInterface);

            // Assert
            await _userInterface.Received().PickCredentialAsync(Arg.Is<Fido2GetAssertionUserInterfaceCredential[]>(
                (credentials) => credentials.All((c) => c.UserVerificationPreference == Fido2UserVerificationPreference.Required)
            ));
        }

        [Fact]
        // Spec: Prompt the user to select a public key credential source `selectedCredential` from `credentialOptions`.
        //       If UserVerificationPreference is Preferred and MP reprompt is on then the authorization gesture MUST include user verification.
        //       If MP reprompt is off then the authorization gestue MAY include user verification
        public async Task GetAssertionAsync_RequestsPreferredUserVerification_ParamsPreferUserVerification()
        {
            // Arrange
            _params.UserVerificationPreference = Fido2UserVerificationPreference.Preferred;
            _userInterface.PickCredentialAsync(Arg.Any<Fido2GetAssertionUserInterfaceCredential[]>()).Returns((_ciphers[0].Id, true));

            // Act
            await _sutProvider.Sut.GetAssertionAsync(_params, _userInterface);

            // Assert
            await _userInterface.Received().PickCredentialAsync(Arg.Is<Fido2GetAssertionUserInterfaceCredential[]>(
                (credentials) => credentials.Any((c) => _ciphers.First(cip => cip.Id == c.CipherId).Reprompt == CipherRepromptType.None && c.UserVerificationPreference == Fido2UserVerificationPreference.Preferred)
            ));

            await _userInterface.Received().PickCredentialAsync(Arg.Is<Fido2GetAssertionUserInterfaceCredential[]>(
                (credentials) => credentials.Any((c) => _ciphers.First(cip => cip.Id == c.CipherId).Reprompt != CipherRepromptType.None && c.UserVerificationPreference == Fido2UserVerificationPreference.Required)
            ));
        }

        [Fact]
        // Spec: Prompt the user to select a public key credential source `selectedCredential` from `credentialOptions`.
        //       If `requireUserPresence` is true, the authorization gesture MUST include a test of user presence.
        // Comment: User presence is implied by the UI returning a credential.
        // Extension: UserVerification is required if the cipher requires reprompting.
        // Deviation: We send the actual preference instead of just a boolean, user presence (not user verification) is therefore required when that value is `discouraged`
        public async Task GetAssertionAsync_DoesNotRequestUserVerification_ParamsDoNotRequireUserVerification()
        {
            // Arrange
            _params.UserVerificationPreference = Fido2UserVerificationPreference.Discouraged;

            // Act
            await _sutProvider.Sut.GetAssertionAsync(_params, _userInterface);

            // Assert
            await _userInterface.Received().PickCredentialAsync(Arg.Is<Fido2GetAssertionUserInterfaceCredential[]>(
                (credentials) => credentials.Select(c => c.UserVerificationPreference == Fido2UserVerificationPreference.Required).SequenceEqual(_ciphers.Select((c) => c.Reprompt == CipherRepromptType.Password))
            ));
        }

        [Fact]
        // Spec: If the user does not consent, return an error code equivalent to "NotAllowedError" and terminate the operation.
        public async Task GetAssertionAsync_ThrowsNotAllowed_UserDoesNotConsent()
        {
            // Arrange
            _userInterface.PickCredentialAsync(Arg.Any<Fido2GetAssertionUserInterfaceCredential[]>()).Returns((null, false));

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => _sutProvider.Sut.GetAssertionAsync(_params, _userInterface));
        }

        [Fact]
        // Spec: If the user does not consent, return an error code equivalent to "NotAllowedError" and terminate the operation.
        public async Task GetAssertionAsync_ThrowsNotAllowed_NoUserVerificationWhenRequired()
        {
            // Arrange
            _params.UserVerificationPreference = Fido2UserVerificationPreference.Required;
            _userInterface.PickCredentialAsync(Arg.Any<Fido2GetAssertionUserInterfaceCredential[]>()).Returns((_selectedCipher.Id, false));
            _sutProvider.GetDependency<IUserVerificationMediatorService>().ShouldEnforceFido2RequiredUserVerificationAsync(Arg.Any<Fido2UserVerificationOptions>()).Returns(Task.FromResult(true));
            
            // Act and assert
            await Assert.ThrowsAsync<NotAllowedError>(() => _sutProvider.Sut.GetAssertionAsync(_params, _userInterface));
        }

        [Fact]
        // Spec: If the user does not consent, return an error code equivalent to "NotAllowedError" and terminate the operation.
        public async Task GetAssertionAsync_ThrowsNotAllowed_NoUserVerificationForCipherWithReprompt()
        {
            // Arrange
            _selectedCipher.Reprompt = CipherRepromptType.Password;
            _params.UserVerificationPreference = Fido2UserVerificationPreference.Discouraged;
            _userInterface.PickCredentialAsync(Arg.Any<Fido2GetAssertionUserInterfaceCredential[]>()).Returns((_selectedCipher.Id, false));
            _sutProvider.GetDependency<IUserVerificationMediatorService>()
                .ShouldPerformMasterPasswordRepromptAsync(Arg.Is<Fido2UserVerificationOptions>(opt => opt.ShouldCheckMasterPasswordReprompt))
                .Returns(Task.FromResult(true));
            _sutProvider.GetDependency<IUserVerificationMediatorService>().ShouldEnforceFido2RequiredUserVerificationAsync(Arg.Any<Fido2UserVerificationOptions>()).Returns(Task.FromResult(true));

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => _sutProvider.Sut.GetAssertionAsync(_params, _userInterface));
        }

        [Fact]
        // Spec: If the user does not consent, return an error code equivalent to "NotAllowedError" and terminate the operation.
        public async Task GetAssertionAsync_ThrowsNotAllowed_PreferredUserVerificationPreference_CanPerformUserVerification()
        {
            // Arrange
            _selectedCipher.Reprompt = CipherRepromptType.Password;
            _params.UserVerificationPreference = Fido2UserVerificationPreference.Preferred;
            _userInterface.PickCredentialAsync(Arg.Any<Fido2GetAssertionUserInterfaceCredential[]>()).Returns((_selectedCipher.Id, false));
            _sutProvider.GetDependency<IUserVerificationMediatorService>()
                .CanPerformUserVerificationPreferredAsync(Arg.Any<Fido2UserVerificationOptions>())
                .Returns(Task.FromResult(true));
            _sutProvider.GetDependency<IUserVerificationMediatorService>().ShouldEnforceFido2RequiredUserVerificationAsync(Arg.Any<Fido2UserVerificationOptions>()).Returns(Task.FromResult(true));

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => _sutProvider.Sut.GetAssertionAsync(_params, _userInterface));
        }

        #endregion

        #region assertion of credential

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        // Spec: Increment the credential associated signature counter
        public async Task GetAssertionAsync_IncrementsCounter_CounterIsLargerThanZero(Cipher encryptedCipher)
        {
            // Arrange
            _selectedCipher.Login.MainFido2Credential.CounterValue = 9000;
            _sutProvider.GetDependency<ICipherService>().EncryptAsync(_selectedCipher).Returns(encryptedCipher);

            // Act
            await _sutProvider.Sut.GetAssertionAsync(_params, _userInterface);

            // Assert
            await _sutProvider.GetDependency<ICipherService>().Received().SaveWithServerAsync(encryptedCipher);
            await _sutProvider.GetDependency<ICipherService>().Received().EncryptAsync(Arg.Is<CipherView>(
                (cipher) => cipher.Login.MainFido2Credential.CounterValue == 9001
            ));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        // Spec: Authenticators that do not implement a signature counter leave the signCount in the authenticator data constant at zero.
        public async Task GetAssertionAsync_DoesNotIncrementsCounter_CounterIsZero(Cipher encryptedCipher)
        {
            // Arrange
            _selectedCipher.Login.MainFido2Credential.CounterValue = 0;
            _sutProvider.GetDependency<ICipherService>().EncryptAsync(_selectedCipher).Returns(encryptedCipher);

            // Act
            await _sutProvider.Sut.GetAssertionAsync(_params, _userInterface);

            // Assert
            await _sutProvider.GetDependency<ICipherService>().DidNotReceive().SaveWithServerAsync(Arg.Any<Cipher>());
        }

        [Fact]
        public async Task GetAssertionAsync_ReturnsAssertion()
        {
            // Arrange
            var keyPair = GenerateKeyPair();
            var rpIdHashMock = RandomBytes(32);
            _params.Hash = RandomBytes(32);
            _params.UserVerificationPreference = Fido2UserVerificationPreference.Required;
            _selectedCipher.Login.MainFido2Credential.CounterValue = 9000;
            _selectedCipher.Login.MainFido2Credential.KeyValue = CoreHelpers.Base64UrlEncode(keyPair.ExportPkcs8PrivateKey());
            _sutProvider.GetDependency<ICryptoFunctionService>().HashAsync(_params.RpId, CryptoHashAlgorithm.Sha256).Returns(rpIdHashMock);
            _userInterface.PickCredentialAsync(Arg.Any<Fido2GetAssertionUserInterfaceCredential[]>()).Returns((_selectedCipher.Id, true));

            // Act
            var result = await _sutProvider.Sut.GetAssertionAsync(_params, _userInterface);

            // Assert
            var authData = result.AuthenticatorData;
            var rpIdHash = authData.Take(32);
            var flags = authData.Skip(32).Take(1);
            var counter = authData.Skip(33).Take(4);

            Assert.Equal(_selectedCipherRawCredentialId, result.SelectedCredential.Id);
            Assert.Equal(CoreHelpers.Base64UrlDecode(_selectedCipher.Login.MainFido2Credential.UserHandle), result.SelectedCredential.UserHandle);
            Assert.Equal(rpIdHashMock, rpIdHash);
            Assert.Equal(new byte[] { 0b00011101 }, flags); // UP = true, UV = true, BS = true, BE = true
            Assert.Equal(new byte[] { 0, 0, 0x23, 0x29 }, counter); // 9001 in binary big-endian format
            Assert.True(keyPair.VerifyData(authData.Concat(_params.Hash).ToArray(), result.Signature, HashAlgorithmName.SHA256, DSASignatureFormat.Rfc3279DerSequence), "Signature verification failed");
        }

        [Fact]
        public async Task GetAssertionAsync_ThrowsUnknownError_SaveFails()
        {
            // Arrange
            _selectedCipher.Login.MainFido2Credential.CounterValue = 1;
            _sutProvider.GetDependency<ICipherService>().SaveWithServerAsync(Arg.Any<Cipher>()).Throws(new Exception());

            // Act & Assert
            await Assert.ThrowsAsync<UnknownError>(() => _sutProvider.Sut.GetAssertionAsync(_params, _userInterface));
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
        private CipherView CreateCipherView(string credentialId, string? rpId, bool? discoverable, bool reprompt = false)
        {
            return new CipherView
            {
                Type = CipherType.Login,
                Id = Guid.NewGuid().ToString(),
                Reprompt = reprompt ? CipherRepromptType.Password : CipherRepromptType.None,
                Login = new LoginView
                {
                    Fido2Credentials = new List<Fido2CredentialView> {
                        new Fido2CredentialView {
                            CredentialId = credentialId,
                            RpId = rpId ?? "bitwarden.com",
                            Discoverable = discoverable.HasValue ? discoverable.ToString() : "true",
                            UserHandleValue = RandomBytes(32),
                            KeyValue = "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgO4wC7AlY4eJP7uedRUJGYsAIJAd6gN1Vp7uJh6xXAp6hRANCAARGvr56F_t27DEG1Tzl-qJRhrTUtC7jOEbasAEEZcE3TiMqoWCan0sxKDPylhRYk-1qyrBC_feN1UtGWH57sROa"
                        }
                    }
                }
            };
        }

        private Fido2AuthenticatorGetAssertionParams CreateParams(string? rpId = null, byte[]? hash = null, PublicKeyCredentialDescriptor[]? allowCredentialDescriptorList = null, bool? requireUserPresence = null, Fido2UserVerificationPreference? userVerificationPreference = null)
        {
            return new Fido2AuthenticatorGetAssertionParams
            {
                RpId = rpId ?? "bitwarden.com",
                Hash = hash ?? RandomBytes(32),
                AllowCredentialDescriptorList = allowCredentialDescriptorList ?? null,
                UserVerificationPreference = userVerificationPreference ?? Fido2UserVerificationPreference.Preferred
            };
        }
    }
}
