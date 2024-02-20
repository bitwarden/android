using System.Threading.Tasks;
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
            var userInterface = new Fido2GetAssertionUserInterface("cipherId", false, null);

            // Act & Assert
            await Assert.ThrowsAsync<NotAllowedError>(() => userInterface.PickCredentialAsync(["notMatching"], false));
        }

        [Fact]
        public async Task PickCredentialAsync_ReturnPrePickedCredential_CredentialsMatch()
        {
            // Arrange
            var userInterface = new Fido2GetAssertionUserInterface("cipherId", false, null);

            // Act
            var result = await userInterface.PickCredentialAsync(["cipherId", "cipherId2"], true);

            // Assert
            Assert.Equal("cipherId", result.CipherId);
            Assert.False(result.UserVerified);
        }

        [Fact]
        public async Task EnsureUnlockedVaultAsync_CallsCallback()
        {
            // Arrange
            var called = false;
            var callback = () => { called = true; return Task.CompletedTask; };
            var userInterface = new Fido2GetAssertionUserInterface("cipherId", false, callback);

            // Act
            await userInterface.EnsureUnlockedVaultAsync();

            // Assert
            Assert.True(called);
        }
    }
}
