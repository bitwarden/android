using System;
using Bit.App.Abstractions;
using Bit.App.Services;
using NSubstitute;
using Xunit;

namespace Bit.App.Test
{
    public class CryptoServiceTests
    {
        [Fact]
        public void EncryptDecrypt()
        {
            var value = "hi";
            Assert.Equal(EncryptDecryptValue(value), value);
        }

        [Fact]
        public void EncryptDecryptLongValue()
        {
            var value = "This is a really long value that should encrypt and decrypt just fine too.";
            Assert.Equal(EncryptDecryptValue(value), value);
        }

        private string EncryptDecryptValue(string value)
        {
            var storageService = Substitute.For<ISecureStorageService>();
            var keyService = Substitute.For<IKeyDerivationService>();
            storageService.Retrieve("key").Returns(
                Convert.FromBase64String("QpSYI5k0bLQXEygUEHn4wMII3ERatuWDFBszk7JAhbQ="));

            var service = new CryptoService(storageService, keyService);
            var encryptedHi = service.Encrypt(value);
            return service.Decrypt(encryptedHi);
        }
    }
}
