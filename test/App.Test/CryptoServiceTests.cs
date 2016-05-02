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
        public void MakeKeyFromPasswordBase64()
        {
            var service = new CryptoService(Substitute.For<ISecureStorageService>());
            var key = service.MakeKeyFromPasswordBase64("123456", "salt");
            Assert.Equal(key, GetKey());
        }

        [Fact]
        public void HashPasswordBase64()
        {
            var service = new CryptoService(Substitute.For<ISecureStorageService>());
            var key = Convert.FromBase64String(GetKey());
            var hash = service.HashPasswordBase64(key, "123456");
            Assert.Equal(hash, "7Bsl4ponrsFu0jGl4yMeLZp5tKqx6g4tLrXhMszIsjQ=");
        }

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
            var storage = Substitute.For<ISecureStorageService>();
            storage.Retrieve("key").Returns(Convert.FromBase64String(GetKey()));

            var service = new CryptoService(storage);
            var encryptedHi = service.Encrypt(value);
            return service.Decrypt(encryptedHi);
        }

        private string GetKey()
        {
            return "QpSYI5k0bLQXEygUEHn4wMII3ERatuWDFBszk7JAhbQ=";
        }
    }
}
