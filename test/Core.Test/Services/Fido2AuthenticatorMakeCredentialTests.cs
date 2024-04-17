using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using Bit.Core.Enums;
using Bit.Core.Utilities.Fido2;
using Bit.Test.Common.AutoFixture;
using NSubstitute;
using NSubstitute.ExceptionExtensions;
using Xunit;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Linq;
using System.Formats.Cbor;

namespace Bit.Core.Test.Services
{
    public class Fido2AuthenticatorMakeCredentialTests : IDisposable
    {
        private readonly string _rpId = "bitwarden.com";
        private readonly SutProvider<Fido2AuthenticatorService> _sutProvider = new SutProvider<Fido2AuthenticatorService>().Create();
        private readonly IFido2MakeCredentialUserInterface _userInterface = Substitute.For<IFido2MakeCredentialUserInterface>();

        private Fido2AuthenticatorMakeCredentialParams _params;
        private List<string> _credentialIds;
        private List<byte[]> _rawCredentialIds;
        private List<CipherView> _ciphers;
        private Cipher _encryptedSelectedCipher;
        private CipherView _selectedCipherView;
        private string _selectedCipherCredentialId;
        private byte[] _selectedCipherRawCredentialId;

        public Fido2AuthenticatorMakeCredentialTests() {
            _credentialIds = new List<string> { "21d6aa04-92bd-4def-bf81-33f046924599", "f70c01ca-d1bf-4704-86e1-b07573aa17fa" };
            _rawCredentialIds = new List<byte[]>
            {
                new byte[] { 0x21, 0xd6, 0xaa, 0x04, 0x92, 0xbd, 0x4d, 0xef, 0xbf, 0x81, 0x33, 0xf0, 0x46, 0x92, 0x45, 0x99 },
                new byte[] { 0xf7, 0x0c, 0x01, 0xca, 0xd1, 0xbf, 0x47, 0x04, 0x86, 0xe1, 0xb0, 0x75, 0x73, 0xaa, 0x17, 0xfa }
            };
            _ciphers = new List<CipherView>
            {
                CreateCipherView(true, _credentialIds[0], "bitwarden.com", false),
                CreateCipherView(true, _credentialIds[1], "bitwarden.com", true)
            };
            _selectedCipherView = _ciphers[0];
            _selectedCipherCredentialId = _credentialIds[0];
            _selectedCipherRawCredentialId = _rawCredentialIds[0];
            _encryptedSelectedCipher = CreateCipher();
            _encryptedSelectedCipher.Id = _selectedCipherView.Id;
            _params = new Fido2AuthenticatorMakeCredentialParams {
                UserEntity = new PublicKeyCredentialUserEntity {
                    Id = RandomBytes(32),
                    Name = "test"
                },
                RpEntity = new PublicKeyCredentialRpEntity {
                    Id = _rpId,
                    Name = "Bitwarden"
                },
                CredTypesAndPubKeyAlgs = new PublicKeyCredentialParameters[]
                {
                    new PublicKeyCredentialParameters
                    {
                        Type = Constants.DefaultFido2CredentialType,
                        Alg = (int) Fido2AlgorithmIdentifier.ES256
                    }
                },
                RequireResidentKey = false,
                UserVerificationPreference = Fido2UserVerificationPreference.Discouraged,
                ExcludeCredentialDescriptorList = null
            };

            _sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(_ciphers);
            _sutProvider.GetDependency<ICipherService>().EncryptAsync(Arg.Any<CipherView>()).Returns(_encryptedSelectedCipher);
            _sutProvider.GetDependency<ICipherService>().GetAsync(Arg.Is(_encryptedSelectedCipher.Id)).Returns(_encryptedSelectedCipher);
            _userInterface.ConfirmNewCredentialAsync(Arg.Any<Fido2ConfirmNewCredentialParams>()).Returns((_selectedCipherView.Id, false));
            _sutProvider.GetDependency<IUserVerificationMediatorService>().CanPerformUserVerificationPreferredAsync(Arg.Any<Fido2UserVerificationOptions>()).Returns(Task.FromResult(false));
            _sutProvider.GetDependency<IUserVerificationMediatorService>().ShouldPerformMasterPasswordRepromptAsync(Arg.Any<Fido2UserVerificationOptions>()).Returns(Task.FromResult(false));

            var cryptoServiceMock = Substitute.For<ICryptoService>();
            ServiceContainer.Register(typeof(CryptoService), cryptoServiceMock);
        }

        public void Dispose()
        {
            ServiceContainer.Reset();
        }

        #region invalid input parameters

        [Fact]
        // Spec: Check if at least one of the specified combinations of PublicKeyCredentialType and cryptographic parameters in credTypesAndPubKeyAlgs is supported. If not, return an error code equivalent to "NotSupportedError" and terminate the operation.
        public async Task MakeCredentialAsync_ThrowsNotSupported_NoSupportedAlgorithm()
        {
            // Arrange
            _params.CredTypesAndPubKeyAlgs = new PublicKeyCredentialParameters[]
            {
                new PublicKeyCredentialParameters
                {
                    Type = Constants.DefaultFido2CredentialType,
                    Alg = -257 // RS256 which we do not support
                }
            };

            // Act & Assert
            await Assert.ThrowsAsync<NotSupportedError>(() => _sutProvider.Sut.MakeCredentialAsync(_params, _userInterface));
        }

        #endregion

        #region vault contains excluded credential

        [Fact]
        // Spec: collect an authorization gesture confirming user consent for creating a new credential.
        // Deviation: Consent is not asked and the user is simply informed of the situation.
        public async Task MakeCredentialAsync_InformsUser_ExcludedCredentialFound()
        {
            // Arrange
            _params.ExcludeCredentialDescriptorList = new PublicKeyCredentialDescriptor[]
            {
                new PublicKeyCredentialDescriptor
                {
                    Type = Constants.DefaultFido2CredentialType,
                    Id = _rawCredentialIds[0]
                }
            };

            // Act
            try
            { 
                await _sutProvider.Sut.MakeCredentialAsync(_params, _userInterface);
            }
            catch {}

            // Assert
            await _userInterface.Received().InformExcludedCredentialAsync(Arg.Is<string[]>(
                (c) => c.SequenceEqual(new string[] { _ciphers[0].Id })
            ));
        }

        [Fact]
        // Spec: return an error code equivalent to "NotAllowedError" and terminate the operation.
        public async Task MakeCredentialAsync_ThrowsNotAllowed_ExcludedCredentialFound()
        {
            _params.ExcludeCredentialDescriptorList = new PublicKeyCredentialDescriptor[]
            {
                new PublicKeyCredentialDescriptor
                {
                    Type = Constants.DefaultFido2CredentialType,
                    Id = _rawCredentialIds[0]
                }
            };

            await Assert.ThrowsAsync<NotAllowedError>(() => _sutProvider.Sut.MakeCredentialAsync(_params, _userInterface));
        }

        [Fact]
        // Deviation: Organization ciphers are not checked against excluded credentials, even if the user has access to them.
        public async Task MakeCredentialAsync_DoesNotInformAboutExcludedCredential_ExcludedCredentialBelongsToOrganization()
        {
            _ciphers[0].OrganizationId = "someOrganizationId";
            _params.ExcludeCredentialDescriptorList = new PublicKeyCredentialDescriptor[]
            {
                new PublicKeyCredentialDescriptor
                {
                    Type = Constants.DefaultFido2CredentialType,
                    Id = _rawCredentialIds[0]
                }
            };

            await _sutProvider.Sut.MakeCredentialAsync(_params, _userInterface);

            await _userInterface.DidNotReceive().InformExcludedCredentialAsync(Arg.Any<string[]>());
        }

        #endregion

        #region credential creation

        [Fact]
        public async Task MakeCredentialAsync_RequestsUserVerification_ParamsRequireUserVerification()
        {
            // Arrange
            _params.UserVerificationPreference = Fido2UserVerificationPreference.Required;
            _userInterface.ConfirmNewCredentialAsync(Arg.Any<Fido2ConfirmNewCredentialParams>()).Returns((_selectedCipherView.Id, true));

            // Act
            await _sutProvider.Sut.MakeCredentialAsync(_params, _userInterface);

            // Assert
            await _userInterface.Received().ConfirmNewCredentialAsync(Arg.Is<Fido2ConfirmNewCredentialParams>(
                (p) => p.UserVerificationPreference == Fido2UserVerificationPreference.Required
            ));
        }

        [Fact]
        public async Task MakeCredentialAsync_RequestsUserVerificationPreferred_ParamsPrefersUserVerification()
        {
            // Arrange
            _params.UserVerificationPreference = Fido2UserVerificationPreference.Preferred;
            _userInterface.ConfirmNewCredentialAsync(Arg.Any<Fido2ConfirmNewCredentialParams>()).Returns((_selectedCipherView.Id, true));

            // Act
            await _sutProvider.Sut.MakeCredentialAsync(_params, _userInterface);

            // Assert
            await _userInterface.Received().ConfirmNewCredentialAsync(Arg.Is<Fido2ConfirmNewCredentialParams>(
                (p) => p.UserVerificationPreference == Fido2UserVerificationPreference.Preferred
            ));
        }

        [Fact]
        public async Task MakeCredentialAsync_DoesNotRequestUserVerification_ParamsDoNotRequireUserVerification()
        {
            // Arrange
            _params.UserVerificationPreference = Fido2UserVerificationPreference.Discouraged;

            // Act
            await _sutProvider.Sut.MakeCredentialAsync(_params, _userInterface);

            // Assert
            await _userInterface.Received().ConfirmNewCredentialAsync(Arg.Is<Fido2ConfirmNewCredentialParams>(
                (p) => p.UserVerificationPreference == Fido2UserVerificationPreference.Discouraged
            ));
        }

        [Fact]
        public async Task MakeCredentialAsync_SavesNewCredential_RequestConfirmedByUser()
        {
            // Arrange
            _params.RequireResidentKey = true;

            // Act
            await _sutProvider.Sut.MakeCredentialAsync(_params, _userInterface);

            // Assert
            await _sutProvider.GetDependency<ICipherService>().Received().EncryptAsync(Arg.Is<CipherView>(
                (c) => 
                    c.Login.MainFido2Credential.KeyType == Constants.DefaultFido2CredentialType &&
                    c.Login.MainFido2Credential.KeyAlgorithm == Constants.DefaultFido2CredentialAlgorithm &&
                    c.Login.MainFido2Credential.KeyCurve == Constants.DefaultFido2CredentialCurve &&
                    c.Login.MainFido2Credential.RpId == _params.RpEntity.Id &&
                    c.Login.MainFido2Credential.RpName == _params.RpEntity.Name &&
                    c.Login.MainFido2Credential.UserHandle == CoreHelpers.Base64UrlEncode(_params.UserEntity.Id) &&
                    c.Login.MainFido2Credential.UserName == _params.UserEntity.Name &&
                    c.Login.MainFido2Credential.CounterValue == 0 &&
                    // c.Login.MainFido2Credential.UserDisplayName == _params.UserEntity.DisplayName &&
                    c.Login.MainFido2Credential.DiscoverableValue == true
            ));
            await _sutProvider.GetDependency<ICipherService>().Received().SaveWithServerAsync(_encryptedSelectedCipher);
        }

        [Fact]
        // Spec: If the user does not consent or if user verification fails, return an error code equivalent to "NotAllowedError" and terminate the operation.
        public async Task MakeCredentialAsync_ThrowsNotAllowed_RequestNotConfirmedByUser()
        {
            // Arrange
            _userInterface.ConfirmNewCredentialAsync(Arg.Any<Fido2ConfirmNewCredentialParams>()).Returns((null, false));

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => _sutProvider.Sut.MakeCredentialAsync(_params, _userInterface));
        }

        [Fact]
        public async Task MakeCredentialAsync_ThrowsNotAllowed_NoUserVerificationWhenRequiredByParams()
        {
            // Arrange
            _params.UserVerificationPreference = Fido2UserVerificationPreference.Required;
            _userInterface.ConfirmNewCredentialAsync(Arg.Any<Fido2ConfirmNewCredentialParams>()).Returns((_encryptedSelectedCipher.Id, false));
            _sutProvider.GetDependency<IUserVerificationMediatorService>().ShouldEnforceFido2RequiredUserVerificationAsync(Arg.Any<Fido2UserVerificationOptions>()).Returns(Task.FromResult(true));

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => _sutProvider.Sut.MakeCredentialAsync(_params, _userInterface));
        }

        [Fact]
        public async Task MakeCredentialAsync_ThrowsNotAllowed_NoUserVerificationForCipherWithReprompt()
        {
            // Arrange
            _params.UserVerificationPreference = Fido2UserVerificationPreference.Discouraged;
            _encryptedSelectedCipher.Reprompt = CipherRepromptType.Password;
            _userInterface.ConfirmNewCredentialAsync(Arg.Any<Fido2ConfirmNewCredentialParams>()).Returns((_encryptedSelectedCipher.Id, false));
            _sutProvider.GetDependency<IUserVerificationMediatorService>()
                .ShouldPerformMasterPasswordRepromptAsync(Arg.Is<Fido2UserVerificationOptions>(opt => opt.ShouldCheckMasterPasswordReprompt))
                .Returns(Task.FromResult(true));
            _sutProvider.GetDependency<IUserVerificationMediatorService>().ShouldEnforceFido2RequiredUserVerificationAsync(Arg.Any<Fido2UserVerificationOptions>()).Returns(Task.FromResult(true));

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => _sutProvider.Sut.MakeCredentialAsync(_params, _userInterface));
        }

        [Fact]
        public async Task MakeCredentialAsync_ThrowsNotAllowed_PreferredUserVerificationPreference_CanPerformUserVerification()
        {
            // Arrange
            _params.UserVerificationPreference = Fido2UserVerificationPreference.Preferred;
            _encryptedSelectedCipher.Reprompt = CipherRepromptType.Password;
            _userInterface.ConfirmNewCredentialAsync(Arg.Any<Fido2ConfirmNewCredentialParams>()).Returns((_encryptedSelectedCipher.Id, false));
            _sutProvider.GetDependency<IUserVerificationMediatorService>()
                .CanPerformUserVerificationPreferredAsync(Arg.Any<Fido2UserVerificationOptions>())
                .Returns(Task.FromResult(true));
            _sutProvider.GetDependency<IUserVerificationMediatorService>().ShouldEnforceFido2RequiredUserVerificationAsync(Arg.Any<Fido2UserVerificationOptions>()).Returns(Task.FromResult(true));

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => _sutProvider.Sut.MakeCredentialAsync(_params, _userInterface));
        }

        [Fact]
        public async Task MakeCredentialAsync_ThrowsUnknownError_SavingCipherFails()
        {
            // Arrange
            _sutProvider.GetDependency<ICipherService>().SaveWithServerAsync(Arg.Any<Cipher>()).Throws(new Exception("Error"));

            // Act & Assert
            await Assert.ThrowsAsync<UnknownError>(() => _sutProvider.Sut.MakeCredentialAsync(_params, _userInterface));
        }

        [Fact]
        public async Task MakeCredentialAsync_ReturnsAttestation()
        {
            // Arrange
            var rpIdHashMock = RandomBytes(32);
            _sutProvider.GetDependency<ICryptoFunctionService>().HashAsync(_params.RpEntity.Id, CryptoHashAlgorithm.Sha256).Returns(rpIdHashMock);
            CipherView generatedCipherView = null;
            _sutProvider.GetDependency<ICipherService>().EncryptAsync(Arg.Any<CipherView>()).Returns((call) => {
                generatedCipherView = call.Arg<CipherView>();
                return _encryptedSelectedCipher;
            });

            // Act
            var result = await _sutProvider.Sut.MakeCredentialAsync(_params, _userInterface);

            // Assert
            var credentialIdBytes = generatedCipherView.Login.MainFido2Credential.CredentialId.GuidToRawFormat();
            var attestationObject = DecodeAttestationObject(result.AttestationObject);
            Assert.Equal("none", attestationObject.Fmt);

            var authData = attestationObject.AuthData;
            var rpIdHash = authData.Take(32).ToArray();
            var flags = authData.Skip(32).Take(1).ToArray();
            var counter = authData.Skip(33).Take(4).ToArray();
            var aaguid = authData.Skip(37).Take(16).ToArray();
            var credentialIdLength = authData.Skip(53).Take(2).ToArray();
            var credentialId = authData.Skip(55).Take(16).ToArray();
            // Unsure how to test public key
            // const publicKey = authData.Skip(71).ToArray(); // Key data is 77 bytes long

            Assert.Equal(71 + 77, authData.Length);
            Assert.Equal(rpIdHashMock, rpIdHash);
            Assert.Equal(new byte[] { 0b01011001 }, flags); // UP = true, AD = true, BS = true, BE = true
            Assert.Equal(new byte[] { 0, 0, 0, 0 }, counter);
            Assert.Equal(Fido2AuthenticatorService.AAGUID, aaguid);
            Assert.Equal(new byte[] { 0, 16 }, credentialIdLength); // 16 bytes because we're using GUIDs
            Assert.Equal(credentialIdBytes, credentialId);
        }

        #endregion

        private byte[] RandomBytes(int length)
        {
            var bytes = new byte[length];
            new Random().NextBytes(bytes);
            return bytes;
        }

        #nullable enable
        private CipherView CreateCipherView(bool? withFido2Credential, string credentialId, string? rpId = null, bool? discoverable = null)
        {
            return new CipherView {
                Type = CipherType.Login,
                Id = Guid.NewGuid().ToString(),
                Reprompt = CipherRepromptType.None,
                Login = new LoginView {
                    Fido2Credentials = withFido2Credential.HasValue && withFido2Credential.Value ? new List<Fido2CredentialView> {
                        new Fido2CredentialView {
                            CredentialId = credentialId,
                            RpId = rpId ?? "bitwarden.com",
                            Discoverable = discoverable.HasValue ? discoverable.ToString() : "true",
                            UserHandleValue = RandomBytes(32)
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
                Key = null,
                Attachments = new List<Attachment>(),
                Login = new Login {},
            };
        }

        private struct AttestationObject
        {
            public string? Fmt { get; set; }
            public object? AttStmt { get; set; }
            public byte[]? AuthData { get; set; }
        }

        private AttestationObject DecodeAttestationObject(byte[] attestationObject) 
        {
            string? fmt = null;
            object? attStmt = null;
            byte[]? authData = null;

            var reader = new CborReader(attestationObject, CborConformanceMode.Ctap2Canonical);
            reader.ReadStartMap();

            while (reader.BytesRemaining != 0)
            {
                var key = reader.ReadTextString();
                switch (key)
                {
                    case "fmt":
                        fmt = reader.ReadTextString();
                        break;
                    case "attStmt":
                        reader.ReadStartMap();
                        reader.ReadEndMap();
                        break;
                    case "authData":
                        authData = reader.ReadByteString();
                        break;
                    default:
                        throw new Exception("Unknown key");
                }
            }

            return new AttestationObject {
                Fmt = fmt,
                AttStmt = attStmt,
                AuthData = authData
            };
        }
    }
}
