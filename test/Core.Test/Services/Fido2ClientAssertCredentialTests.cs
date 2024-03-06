using System;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.Core.Utilities.Fido2;
using Bit.Test.Common.AutoFixture;
using NSubstitute;
using NSubstitute.ExceptionExtensions;
using Xunit;

namespace Bit.Core.Test.Services
{
    public class Fido2ClientAssertCredentialTests : IDisposable
    {
        private readonly SutProvider<Fido2ClientService> _sutProvider = new SutProvider<Fido2ClientService>().Create();

        private Fido2ClientAssertCredentialParams _params;

        public Fido2ClientAssertCredentialTests()
        {
            _params = new Fido2ClientAssertCredentialParams {
                Origin = "https://bitwarden.com",
                Challenge = RandomBytes(32),
                RpId = "bitwarden.com",
                UserVerification = "required",
                AllowCredentials = [
                    new PublicKeyCredentialDescriptor {
                        Id = RandomBytes(32),
                        Type = Constants.DefaultFido2CredentialType
                    }
                ],
                Timeout = 60000,
            };

            _sutProvider.GetDependency<IStateService>().GetAutofillBlacklistedUrisAsync().Returns([]);
            _sutProvider.GetDependency<IStateService>().IsAuthenticatedAsync().Returns(true);
        }

        public void Dispose() 
        {
        }

        [Fact(Skip = "Not sure how to check this, or if it matters.")]
        // Spec: If callerOrigin is an opaque origin, return a DOMException whose name is "NotAllowedError", and terminate this algorithm.
        public Task AssertCredentialAsync_ThrowsNotAllowedError_OriginIsOpaque() => throw new NotImplementedException();

        [Fact]
        // Spec: Let effectiveDomain be the callerOrigin’s effective domain. If effective domain is not a valid domain, 
        //       then return a DOMException whose name is "SecurityError" and terminate this algorithm.
        public async Task AssertCredentialAsync_ThrowsSecurityError_OriginIsNotValidDomain()
        {
            // Arrange
            _params.Origin = "invalid-domain-name";

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.AssertCredentialAsync(_params));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.SecurityError, exception.Code);
        }

        [Fact]
        // Spec: If options.rp.id is not a registrable domain suffix of and is not equal to effectiveDomain, 
        //       return a DOMException whose name is "SecurityError", and terminate this algorithm.
        public async Task AssertCredentialAsync_ThrowsSecurityError_RpIdIsNotValidForOrigin()
        {
            // Arrange
            _params.Origin = "https://passwordless.dev";
            _params.RpId = "bitwarden.com";

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.AssertCredentialAsync(_params));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.SecurityError, exception.Code);
        }

        [Fact]
        // Spec: The origin's scheme must be https.
        public async Task AssertCredentialAsync_ThrowsSecurityError_OriginIsNotHttps()
        {
            // Arrange
            _params.Origin = "http://bitwarden.com";
            _params.RpId = "bitwarden.com";

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.AssertCredentialAsync(_params));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.SecurityError, exception.Code);
        }

        [Fact]
        // Spec: If the origin's hostname is a blocked uri, then return UriBlockedError.
        public async Task AssertCredentialAsync_ThrowsUriBlockedError_OriginIsBlocked()
        {
            // Arrange
            _params.Origin = "https://sub.bitwarden.com";
            _sutProvider.GetDependency<IStateService>().GetAutofillBlacklistedUrisAsync().Returns([
                "sub.bitwarden.com"
            ]);

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.AssertCredentialAsync(_params));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.UriBlockedError, exception.Code);
        }

        [Fact]
        public async Task AssertCredentialAsync_ThrowsInvalidStateError_AuthenticatorThrowsInvalidStateError()
        {
            // Arrange
            _sutProvider.GetDependency<IFido2AuthenticatorService>()
                .GetAssertionAsync(Arg.Any<Fido2AuthenticatorGetAssertionParams>(), _sutProvider.GetDependency<IFido2GetAssertionUserInterface>())
                .Throws(new InvalidStateError());

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.AssertCredentialAsync(_params));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.InvalidStateError, exception.Code);
        }

        [Fact]
        // This keeps sensetive information form leaking
        public async Task AssertCredentialAsync_ThrowsUnknownError_AuthenticatorThrowsUnknownError()
        {
            // Arrange
            _sutProvider.GetDependency<IFido2AuthenticatorService>()
                .GetAssertionAsync(Arg.Any<Fido2AuthenticatorGetAssertionParams>(), _sutProvider.GetDependency<IFido2GetAssertionUserInterface>())
                .Throws(new Exception("unknown error"));

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.AssertCredentialAsync(_params));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.UnknownError, exception.Code);
        }

        [Fact]
        public async Task AssertCredentialAsync_ThrowsInvalidStateError_UserIsLoggedOut()
        {
            // Arrange
            _sutProvider.GetDependency<IStateService>().IsAuthenticatedAsync().Returns(false);

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.AssertCredentialAsync(_params));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.InvalidStateError, exception.Code);
        }

        [Fact]
        public async Task AssertCredentialAsync_ThrowsNotAllowedError_OriginIsBitwardenVault()
        {
            // Arrange
            _params.Origin = "https://vault.bitwarden.com";
            _sutProvider.GetDependency<IEnvironmentService>().GetWebVaultUrl().Returns("https://vault.bitwarden.com");

            // Act
            var exception = await Assert.ThrowsAsync<Fido2ClientException>(() => _sutProvider.Sut.AssertCredentialAsync(_params));

            // Assert
            Assert.Equal(Fido2ClientException.ErrorCode.NotAllowedError, exception.Code);
        }

        [Fact]
        public async Task AssertCredentialAsync_ReturnsAssertion()
        {
            // Arrange
            _params.UserVerification = "required";
            var authenticatorResult = new Fido2AuthenticatorGetAssertionResult {
                AuthenticatorData = RandomBytes(32),
                SelectedCredential = new Fido2AuthenticatorGetAssertionSelectedCredential {
                    Id = RandomBytes(16),
                    UserHandle = RandomBytes(32)
                },
                Signature = RandomBytes(32)
            };
            _sutProvider.GetDependency<IFido2AuthenticatorService>()
                .GetAssertionAsync(Arg.Any<Fido2AuthenticatorGetAssertionParams>(), _sutProvider.GetDependency<IFido2GetAssertionUserInterface>())
                .Returns(authenticatorResult);

            // Act
            var result = await _sutProvider.Sut.AssertCredentialAsync(_params);

            // Assert
            await _sutProvider.GetDependency<IFido2AuthenticatorService>()
                .Received()
                .GetAssertionAsync(
                    Arg.Is<Fido2AuthenticatorGetAssertionParams>(x =>
                        x.RpId == _params.RpId &&
                        x.RequireUserVerification == true &&
                        x.AllowCredentialDescriptorList.Length == 1 &&
                        x.AllowCredentialDescriptorList[0].Id == _params.AllowCredentials[0].Id
                    ), 
                    _sutProvider.GetDependency<IFido2GetAssertionUserInterface>()
                );

            Assert.Equal(authenticatorResult.SelectedCredential.Id, result.RawId);
            Assert.Equal(CoreHelpers.Base64UrlEncode(authenticatorResult.SelectedCredential.Id), result.Id);
            Assert.Equal(authenticatorResult.AuthenticatorData, result.AuthenticatorData);
            Assert.Equal(authenticatorResult.Signature, result.Signature);

            var clientDataJSON = JsonSerializer.Deserialize<JsonObject>(Encoding.UTF8.GetString(result.ClientDataJSON));
            Assert.Equal("webauthn.get", clientDataJSON["type"].GetValue<string>());
            Assert.Equal(CoreHelpers.Base64UrlEncode(_params.Challenge), clientDataJSON["challenge"].GetValue<string>());
            Assert.Equal(_params.Origin, clientDataJSON["origin"].GetValue<string>());
            Assert.Equal(!_params.SameOriginWithAncestors, clientDataJSON["crossOrigin"].GetValue<bool>());
        }

        private byte[] RandomBytes(int length)
        {
            var bytes = new byte[length];
            new Random().NextBytes(bytes);
            return bytes;
        }
    }
}