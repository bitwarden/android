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
            await Assert.ThrowsAsync<NotAllowedError>(() => userInterface.PickCredentialAsync([CreateCredential("notMatching", false)]));
        }

        [Fact]
        public async Task PickCredentialAsync_ReturnPrePickedCredential_CredentialsMatch()
        {
            // Arrange
            var userInterface = new Fido2GetAssertionUserInterface("cipherId", false, null, null);

            // Act
            var result = await userInterface.PickCredentialAsync([CreateCredential("cipherId", false), CreateCredential("cipherId2", true)]);

            // Assert
            Assert.Equal("cipherId", result.CipherId);
            Assert.False(result.UserVerified);
        }

        [Fact]
        public async Task PickCredentialAsync_CallsUserVerificationCallback_UserIsAlreadyVerified()
        {
            // Arrange
            var called = false;
            var callback = () => { called = true; return Task.FromResult(true); };
            var userInterface = new Fido2GetAssertionUserInterface("cipherId", false, null, callback);

            // Act
            var result = await userInterface.PickCredentialAsync([CreateCredential("cipherId", true), CreateCredential("cipherId2", false)]);

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
            var callback = () => { called = true; return Task.FromResult(true); };
            var userInterface = new Fido2GetAssertionUserInterface("cipherId2", true, null, callback);

            // Act
            var result = await userInterface.PickCredentialAsync([CreateCredential("cipherId", true), CreateCredential("cipherId2", false)]);

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
            var callback = () => { called = true; return Task.FromResult(true); };
            var userInterface = new Fido2GetAssertionUserInterface("cipherId2", false, null, callback);

            // Act
            var result = await userInterface.PickCredentialAsync([CreateCredential("cipherId", true), CreateCredential("cipherId2", false)]);

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

        private Fido2GetAssertionUserInterfaceCredential CreateCredential(string cipherId, bool requireUserVerification)
        {
            return new Fido2GetAssertionUserInterfaceCredential
            {
                CipherId = cipherId,
                RequireUserVerification = requireUserVerification
            };
        }
    }
}
