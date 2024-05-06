using System;
using System.Collections.Generic;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.Core.Utilities.Fido2;
using Bit.Core.Utilities.Fido2.Extensions;
using Bit.Test.Common.AutoFixture;
using NSubstitute;
using NSubstitute.ExceptionExtensions;
using Xunit;

namespace Bit.Core.Test.Services
{
    public class Fido2ClientCreateCredentialTests : IDisposable
    {
        private readonly SutProvider<Fido2ClientService> _sutProvider = new SutProvider<Fido2ClientService>().Create();

        private Fido2ClientCreateCredentialParams _params;
        private Fido2AuthenticatorMakeCredentialResult _authenticatorResult;

        public Fido2ClientCreateCredentialTests()
        {
            _params = new Fido2ClientCreateCredentialParams
            {
                Origin = "https://bitwarden.com",
                SameOriginWithAncestors = true,
                Attestation = "none",
                Challenge = RandomBytes(32),
                PubKeyCredParams = new PublicKeyCredentialParameters[]
                {
                    new PublicKeyCredentialParameters
                    {
                        Type = Constants.DefaultFido2CredentialType,
                        Alg = (int) Fido2AlgorithmIdentifier.ES256
                    }
                },
                Rp = new PublicKeyCredentialRpEntity
                {
                    Id = "bitwarden.com",
                    Name = "Bitwarden"
                },
                User = new PublicKeyCredentialUserEntity
                {
                    Id = RandomBytes(32),
                    Name = "user@bitwarden.com",
                    DisplayName = "User"
                }
            };

            _authenticatorResult = new Fido2AuthenticatorMakeCredentialResult
            {
                CredentialId = RandomBytes(32),
                AttestationObject = RandomBytes(32),
                AuthData = RandomBytes(32),
                PublicKey = RandomBytes(32),
                PublicKeyAlgorithm = (int)Fido2AlgorithmIdentifier.ES256,
            };

            _sutProvider.GetDependency<IStateService>().GetAutofillBlacklistedUrisAsync().Returns(Task.FromResult(new List<string>()));
            _sutProvider.GetDependency<IStateService>().IsAuthenticatedAsync().Returns(true);
        }

        public void Dispose()
        {
        }

        [Fact]
        // Spec: If sameOriginWithAncestors is false, return a "NotAllowedError" DOMException.
        public async Task CreateCredentialAsync_ThrowsNotAllowedError_SameOriginWithAncestorsIsFalse()
        {
            // Arrange
            _params.SameOriginWithAncestors = false;

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams()));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.NotAllowedError, exception.Code);
        }

        [Fact]
        // Spec: If the length of options.user.id is not between 1 and 64 bytes (inclusive) then return a TypeError.
        public async Task CreateCredentialAsync_ThrowsTypeError_UserIdIsTooSmall()
        {
            // Arrange
            _params.User.Id = RandomBytes(0);

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams()));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.TypeError, exception.Code);
        }

        [Fact]
        // Spec: If the length of options.user.id is not between 1 and 64 bytes (inclusive) then return a TypeError.
        public async Task CreateCredentialAsync_ThrowsTypeError_UserIdIsTooLarge()
        {
            // Arrange
            _params.User.Id = RandomBytes(65);

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams()));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.TypeError, exception.Code);
        }

        [Fact(Skip = "Not sure how to check this, or if it matters.")]
        // Spec: If callerOrigin is an opaque origin, return a DOMException whose name is "NotAllowedError", and terminate this algorithm.
        public Task CreateCredentialAsync_ThrowsNotAllowedError_OriginIsOpaque() => throw new NotImplementedException();

        [Fact]
        // Spec: Let effectiveDomain be the callerOrigin’s effective domain. If effective domain is not a valid domain, 
        //       then return a DOMException whose name is "SecurityError" and terminate this algorithm.
        public async Task CreateCredentialAsync_ThrowsSecurityError_OriginIsNotValidDomain()
        {
            // Arrange
            _params.Origin = "invalid-domain-name";

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams()));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.SecurityError, exception.Code);
        }

        [Fact]
        // Spec: If options.rp.id is not a registrable domain suffix of and is not equal to effectiveDomain, 
        //       return a DOMException whose name is "SecurityError", and terminate this algorithm.
        public async Task CreateCredentialAsync_ThrowsSecurityError_RpIdIsNotValidForOrigin()
        {
            // Arrange
            _params.Origin = "https://passwordless.dev";
            _params.Rp.Id = "bitwarden.com";

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams()));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.SecurityError, exception.Code);
        }

        [Fact]
        // Spec: The origin's scheme must be https.
        public async Task CreateCredentialAsync_ThrowsSecurityError_OriginIsNotHttps()
        {
            // Arrange
            _params.Origin = "http://bitwarden.com";
            _params.Rp.Id = "bitwarden.com";

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams()));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.SecurityError, exception.Code);
        }

        [Fact]
        // Spec: If the origin's hostname is a blocked uri, then return UriBlockedError.
        public async Task CreateCredentialAsync_ThrowsUriBlockedError_OriginIsBlocked()
        {
            // Arrange
            _params.Origin = "https://sub.bitwarden.com";
            _sutProvider.GetDependency<IStateService>().GetAutofillBlacklistedUrisAsync().Returns(Task.FromResult(new List<string>
            {
                "sub.bitwarden.com"
            }));

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams()));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.UriBlockedError, exception.Code);
        }

        [Fact]
        // Spec: If credTypesAndPubKeyAlgs is empty, return a DOMException whose name is "NotSupportedError", and terminate this algorithm.
        public async Task CreateCredentialAsync_ThrowsNotSupportedError_CredTypesAndPubKeyAlgsIsEmpty()
        {
            // Arrange
            _params.PubKeyCredParams = new PublicKeyCredentialParameters[]
            {
                new PublicKeyCredentialParameters {
                    Type = "not-supported",
                    Alg = (int) Fido2AlgorithmIdentifier.ES256
                },
                new PublicKeyCredentialParameters {
                    Type = Constants.DefaultFido2CredentialType,
                    Alg = -9001
                }
            };

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams()));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.NotSupportedError, exception.Code);
        }

        [Fact(Skip = "Not implemented")]
        // Spec: If the options.signal is present and its aborted flag is set to true, return a DOMException whose name is "AbortError" and terminate this algorithm.
        public Task CreateCredentialAsync_ThrowsAbortError_AbortedByCaller() => throw new NotImplementedException();

        [Fact]
        public async Task CreateCredentialAsync_ReturnsNewCredential()
        {
            // Arrange
            _params.AuthenticatorSelection = new AuthenticatorSelectionCriteria
            {
                ResidentKey = "required",
                UserVerification = "required"
            };
            var authenticatorResult = new Fido2AuthenticatorMakeCredentialResult
            {
                CredentialId = RandomBytes(32),
                AttestationObject = RandomBytes(32),
                AuthData = RandomBytes(32),
                PublicKey = RandomBytes(32),
                PublicKeyAlgorithm = (int)Fido2AlgorithmIdentifier.ES256,
            };
            _sutProvider.GetDependency<IFido2AuthenticatorService>()
                .MakeCredentialAsync(Arg.Any<Fido2AuthenticatorMakeCredentialParams>(), _sutProvider.GetDependency<IFido2MakeCredentialUserInterface>())
                .Returns(authenticatorResult);

            // Act
            var result = await _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams());

            // Assert
            await _sutProvider.GetDependency<IFido2AuthenticatorService>()
                .Received()
                .MakeCredentialAsync(
                    Arg.Is<Fido2AuthenticatorMakeCredentialParams>(x =>
                        x.RequireResidentKey == true &&
                        x.UserVerificationPreference == Fido2UserVerificationPreference.Required &&
                        x.RpEntity.Id == _params.Rp.Id &&
                        x.UserEntity.DisplayName == _params.User.DisplayName
                    ),
                    _sutProvider.GetDependency<IFido2MakeCredentialUserInterface>()
                );
            Assert.Equal(authenticatorResult.CredentialId, result.CredentialId);
            Assert.Equal(authenticatorResult.AttestationObject, result.AttestationObject);
            Assert.Equal(authenticatorResult.AuthData, result.AuthData);
            Assert.Equal(authenticatorResult.PublicKey, result.PublicKey);
            Assert.Equal(authenticatorResult.PublicKeyAlgorithm, result.PublicKeyAlgorithm);
            Assert.Equal(new string[] { "internal" }, result.Transports);

            var clientDataJSON = JsonSerializer.Deserialize<JsonObject>(Encoding.UTF8.GetString(result.ClientDataJSON));
            Assert.Equal("webauthn.create", clientDataJSON["type"].GetValue<string>());
            Assert.Equal(CoreHelpers.Base64UrlEncode(_params.Challenge), clientDataJSON["challenge"].GetValue<string>());
            Assert.Equal(_params.Origin, clientDataJSON["origin"].GetValue<string>());
            Assert.Equal(!_params.SameOriginWithAncestors, clientDataJSON["crossOrigin"].GetValue<bool>());
        }

        [Fact]
        public async Task CreateCredentialAsync_ReturnsNewCredential_WithAndroidPackageName()
        {
            // Arrange
            _params.AuthenticatorSelection = new AuthenticatorSelectionCriteria
            {
                ResidentKey = "required",
                UserVerification = "required"
            };
            var authenticatorResult = new Fido2AuthenticatorMakeCredentialResult
            {
                CredentialId = RandomBytes(32),
                AttestationObject = RandomBytes(32),
                AuthData = RandomBytes(32),
                PublicKey = RandomBytes(32),
                PublicKeyAlgorithm = (int)Fido2AlgorithmIdentifier.ES256,
            };
            _sutProvider.GetDependency<IFido2AuthenticatorService>()
                .MakeCredentialAsync(Arg.Any<Fido2AuthenticatorMakeCredentialParams>(), _sutProvider.GetDependency<IFido2MakeCredentialUserInterface>())
                .Returns(authenticatorResult);
            var packageName = "com.example.app";

            // Act
            var result = await _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams(null, packageName));

            // Assert
            await _sutProvider.GetDependency<IFido2AuthenticatorService>()
                .Received()
                .MakeCredentialAsync(
                    Arg.Is<Fido2AuthenticatorMakeCredentialParams>(x =>
                        x.RequireResidentKey == true &&
                        x.UserVerificationPreference == Fido2UserVerificationPreference.Required &&
                        x.RpEntity.Id == _params.Rp.Id &&
                        x.UserEntity.DisplayName == _params.User.DisplayName
                    ),
                    _sutProvider.GetDependency<IFido2MakeCredentialUserInterface>()
                );
            Assert.Equal(authenticatorResult.CredentialId, result.CredentialId);
            Assert.Equal(authenticatorResult.AttestationObject, result.AttestationObject);
            Assert.Equal(authenticatorResult.AuthData, result.AuthData);
            Assert.Equal(authenticatorResult.PublicKey, result.PublicKey);
            Assert.Equal(authenticatorResult.PublicKeyAlgorithm, result.PublicKeyAlgorithm);
            Assert.Equal(new string[] { "internal" }, result.Transports);

            var clientDataJSON = JsonSerializer.Deserialize<JsonObject>(Encoding.UTF8.GetString(result.ClientDataJSON));
            Assert.Equal("webauthn.create", clientDataJSON["type"].GetValue<string>());
            Assert.Equal(CoreHelpers.Base64UrlEncode(_params.Challenge), clientDataJSON["challenge"].GetValue<string>());
            Assert.Equal(_params.Origin, clientDataJSON["origin"].GetValue<string>());
            Assert.Equal(!_params.SameOriginWithAncestors, clientDataJSON["crossOrigin"].GetValue<bool>());
            Assert.Equal(packageName, clientDataJSON["androidPackageName"].GetValue<string>());
        }

        [Fact]
        public async Task CreateCredentialAsync_ThrowsInvalidStateError_AuthenticatorThrowsInvalidStateError()
        {
            // Arrange
            _params.AuthenticatorSelection = new AuthenticatorSelectionCriteria
            {
                ResidentKey = "required",
                UserVerification = "required"
            };
            _sutProvider.GetDependency<IFido2AuthenticatorService>()
                .MakeCredentialAsync(Arg.Any<Fido2AuthenticatorMakeCredentialParams>(), _sutProvider.GetDependency<IFido2MakeCredentialUserInterface>())
                .Throws(new InvalidStateError());

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams()));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.InvalidStateError, exception.Code);
        }

        [Fact]
        // This keeps sensetive information form leaking
        public async Task CreateCredentialAsync_ThrowsUnknownError_AuthenticatorThrowsUnknownError()
        {
            // Arrange
            _sutProvider.GetDependency<IFido2AuthenticatorService>()
                .MakeCredentialAsync(Arg.Any<Fido2AuthenticatorMakeCredentialParams>(), _sutProvider.GetDependency<IFido2MakeCredentialUserInterface>())
                .Throws(new Exception("unknown error"));

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams()));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.UnknownError, exception.Code);
        }

        [Fact]
        public async Task CreateCredentialAsync_ThrowsInvalidStateError_UserIsLoggedOut()
        {
            // Arrange
            _sutProvider.GetDependency<IStateService>().IsAuthenticatedAsync().Returns(false);

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams()));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.InvalidStateError, exception.Code);
        }

        [Fact]
        public async Task CreateCredentialAsync_ThrowsNotAllowedError_OriginIsBitwardenVault()
        {
            // Arrange
            _params.Origin = "https://vault.bitwarden.com";
            _sutProvider.GetDependency<IEnvironmentService>().GetWebVaultUrl().Returns("https://vault.bitwarden.com");

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams()));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.NotAllowedError, exception.Code);
        }

        [Fact]
        public async Task CreateCredentialAsync_ConstructsClientDataHash_WhenHashIsNotProvided()
        {
            // Arrange
            var mockHash = RandomBytes(32);
            _sutProvider.GetDependency<ICryptoFunctionService>()
                .HashAsync(Arg.Any<byte[]>(), Arg.Is(CryptoHashAlgorithm.Sha256))
                .Returns(Task.FromResult(mockHash));
            _sutProvider.GetDependency<IFido2AuthenticatorService>()
                .MakeCredentialAsync(Arg.Any<Fido2AuthenticatorMakeCredentialParams>(), _sutProvider.GetDependency<IFido2MakeCredentialUserInterface>())
                .Returns(_authenticatorResult);

            // Act
            await _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams());

            // Assert
            await _sutProvider.GetDependency<IFido2AuthenticatorService>().Received()
                .MakeCredentialAsync(
                    Arg.Is((Fido2AuthenticatorMakeCredentialParams x) => x.Hash == mockHash),
                    Arg.Any<IFido2MakeCredentialUserInterface>()
                );
        }

        [Fact]
        public async Task CreateCredentialAsync_UsesProvidedClientDataHash_WhenHashIsProvided()
        {
            // Arrange
            var mockHash = RandomBytes(32);
            _sutProvider.GetDependency<IFido2AuthenticatorService>()
                .MakeCredentialAsync(Arg.Any<Fido2AuthenticatorMakeCredentialParams>(), _sutProvider.GetDependency<IFido2MakeCredentialUserInterface>())
                .Returns(_authenticatorResult);

            // Act
            await _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams(mockHash));

            // Assert
            await _sutProvider.GetDependency<IFido2AuthenticatorService>().Received()
                .MakeCredentialAsync(
                    Arg.Is((Fido2AuthenticatorMakeCredentialParams x) => x.Hash == mockHash),
                    Arg.Any<IFido2MakeCredentialUserInterface>()
                );
        }

        [Fact]
        public async Task CreateCredentialAsync_ReturnsCredPropsRkTrue_WhenCreatingDiscoverableCredential()
        {
            // Arrange
            _params.AuthenticatorSelection = new AuthenticatorSelectionCriteria
            {
                ResidentKey = "required"
            };
            _params.Extensions = new Fido2CreateCredentialExtensionsParams { CredProps = true };
            _sutProvider.GetDependency<IFido2AuthenticatorService>()
                .MakeCredentialAsync(Arg.Any<Fido2AuthenticatorMakeCredentialParams>(), _sutProvider.GetDependency<IFido2MakeCredentialUserInterface>())
                .Returns(_authenticatorResult);

            // Act
            var result = await _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams());

            // Assert
            Assert.True(result.Extensions.CredProps?.Rk);
        }

        [Fact]
        public async Task CreateCredentialAsync_ReturnsCredPropsRkFalse_WhenCreatingNonDiscoverableCredential()
        {
            // Arrange
            _params.AuthenticatorSelection = new AuthenticatorSelectionCriteria
            {
                ResidentKey = "discouraged"
            };
            _params.Extensions = new Fido2CreateCredentialExtensionsParams { CredProps = true };
            _sutProvider.GetDependency<IFido2AuthenticatorService>()
                .MakeCredentialAsync(Arg.Any<Fido2AuthenticatorMakeCredentialParams>(), _sutProvider.GetDependency<IFido2MakeCredentialUserInterface>())
                .Returns(_authenticatorResult);

            // Act
            var result = await _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams());

            // Assert
            Assert.False(result.Extensions.CredProps?.Rk);
        }

        [Fact]
        public async Task CreateCredentialAsync_ReturnsCredPropsUndefined_WhenExtensionIsNotRequested()
        {
            // Arrange
            _params.AuthenticatorSelection = new AuthenticatorSelectionCriteria
            {
                ResidentKey = "required"
            };
            _params.Extensions = new Fido2CreateCredentialExtensionsParams();
            _sutProvider.GetDependency<IFido2AuthenticatorService>()
                .MakeCredentialAsync(Arg.Any<Fido2AuthenticatorMakeCredentialParams>(), _sutProvider.GetDependency<IFido2MakeCredentialUserInterface>())
                .Returns(_authenticatorResult);

            // Act
            var result = await _sutProvider.Sut.CreateCredentialAsync(_params, new Fido2ExtraCreateCredentialParams());

            // Assert
            Assert.Null(result.Extensions.CredProps);
        }

        private byte[] RandomBytes(int length)
        {
            var bytes = new byte[length];
            new Random().NextBytes(bytes);
            return bytes;
        }
    }
}
