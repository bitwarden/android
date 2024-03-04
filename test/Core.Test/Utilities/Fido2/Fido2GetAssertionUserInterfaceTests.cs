using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Utilities.Fido2;
using Xunit;

namespace Bit.Core.Test.Utilities.Fido2
{
    public class Fido2GetAssertionUserInterfaceTests
    {
        [Fact]
        public async Task PickCredentialAsync_ThrowsNotAllowed_PrePickedCredentialDoesNotMatch()
        {
            // Arrange
            var userInterface = new Fido2GetAssertionUserInterface("cipherId", false, null, null);

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => userInterface.PickCredentialAsync(new Fido2GetAssertionUserInterfaceCredential[] { CreateCredential("notMatching", Fido2UserVerificationPreference.Discouraged) }));
        }

        [Fact]
        public async Task PickCredentialAsync_ReturnPrePickedCredential_CredentialsMatch()
        {
            // Arrange
            var userInterface = new Fido2GetAssertionUserInterface("cipherId", false, null, null);

            // Act
            var result = await userInterface.PickCredentialAsync(new Fido2GetAssertionUserInterfaceCredential[]
            {
                CreateCredential("cipherId", Fido2UserVerificationPreference.Discouraged),
                CreateCredential("cipherId2", Fido2UserVerificationPreference.Required)
            });

            // Assert
            Assert.Equal("cipherId", result.CipherId);
            Assert.False(result.UserVerified);
        }

        [Fact]
        public async Task PickCredentialAsync_CallsUserVerificationCallback_UserIsAlreadyVerified()
        {
            // Arrange
            var called = false;
            var userInterface = new Fido2GetAssertionUserInterface("cipherId", false, null, (_, __) =>
            {
                called = true;
                return Task.FromResult(true);
            });

            // Act
            var result = await userInterface.PickCredentialAsync(new Fido2GetAssertionUserInterfaceCredential[]
            {
                CreateCredential("cipherId", Fido2UserVerificationPreference.Required),
                CreateCredential("cipherId2", Fido2UserVerificationPreference.Discouraged)
            });

            // Assert
            Assert.Equal("cipherId", result.CipherId);
            Assert.True(result.UserVerified);
            Assert.True(called);
        }

        [Fact]
        public async Task PickCredentialAsync_DoesNotCallUserVerificationCallback_UserVerificationIsAlreadyPerformed()
        {
            // Arrange
            var called = false;
            var userInterface = new Fido2GetAssertionUserInterface("cipherId2", true, null, (_, __) =>
            {
                called = true;
                return Task.FromResult(true);
            });

            // Act
            var result = await userInterface.PickCredentialAsync(new Fido2GetAssertionUserInterfaceCredential[]
            {
                CreateCredential("cipherId", Fido2UserVerificationPreference.Required),
                CreateCredential("cipherId2", Fido2UserVerificationPreference.Discouraged)
            });

            // Assert
            Assert.Equal("cipherId2", result.CipherId);
            Assert.True(result.UserVerified);
            Assert.False(called);
        }

        [Fact]
        public async Task PickCredentialAsync_DoesNotCallUserVerificationCallback_UserVerificationIsNotRequired()
        {
            // Arrange
            var called = false;
            var userInterface = new Fido2GetAssertionUserInterface("cipherId2", false, null, (_, __) =>
            {
                called = true;
                return Task.FromResult(true);
            });

            // Act
            var result = await userInterface.PickCredentialAsync(new Fido2GetAssertionUserInterfaceCredential[]
            {
                CreateCredential("cipherId", Fido2UserVerificationPreference.Required),
                CreateCredential("cipherId2", Fido2UserVerificationPreference.Discouraged)
            });

            // Assert
            Assert.Equal("cipherId2", result.CipherId);
            Assert.False(result.UserVerified);
            Assert.False(called);
        }

        [Fact]
        public async Task EnsureUnlockedVaultAsync_CallsCallback()
        {
            // Arrange
            var called = false;
            var callback = () => { called = true; return Task.CompletedTask; };
            var userInterface = new Fido2GetAssertionUserInterface("cipherId", false, callback, null);

            // Act
            await userInterface.EnsureUnlockedVaultAsync();

            // Assert
            Assert.True(called);
        }

        private Fido2GetAssertionUserInterfaceCredential CreateCredential(string cipherId, Fido2UserVerificationPreference userVerificationPreference)
        {
            return new Fido2GetAssertionUserInterfaceCredential
            {
                CipherId = cipherId,
                UserVerificationPreference = userVerificationPreference
            };
        }
    }
}
