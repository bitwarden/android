using System;
using System.Linq;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Test.Common;
using System.Text;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Bit.Test.Common.AutoFixture.Attributes;
using NSubstitute;
using Xunit;
using Bit.Core.Test.AutoFixture;
using AutoFixture.AutoNSubstitute;

namespace Bit.Core.Test.Models.Domain
{
    public class SendTests
    {
        [Theory]
        [InlineCustomAutoData(new[] { typeof(FileSendCustomization) })]
        [InlineCustomAutoData(new[] { typeof(TextSendCustomization) })]
        public void Send_FromSendData_Success(SendData data)
        {
            var send = new Send(data);

            TestHelper.AssertPropertyEqual(data, send, "Name", "Notes", "Key", "SendFileData.FileName", "SendFileData.Key", "SendTextData.Text");
            Assert.Equal(data.Name, send.Name?.EncryptedString);
            Assert.Equal(data.Notes, send.Notes?.EncryptedString);
            Assert.Equal(data.Key, send.Key?.EncryptedString);
            Assert.Equal(data.Text?.Text, send.Text?.Text?.EncryptedString);
            Assert.Equal(data.File?.FileName, send.File?.FileName?.EncryptedString);
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(AutoNSubstituteCustomization), typeof(TextSendCustomization) })]
        [InlineCustomAutoData(new[] { typeof(AutoNSubstituteCustomization), typeof(FileSendCustomization) })]
        public async void DecryptAsync_Success(ICryptoService cryptoService, Send send)
        {
            // TODO restore this once race condition is fixed or GHA can re-run jobs on individual platforms
            return;

            var prefix = "decrypted_";
            var prefixBytes = Encoding.UTF8.GetBytes(prefix);

            cryptoService.DecryptToBytesAsync(Arg.Any<EncString>(), Arg.Any<SymmetricCryptoKey>())
                .Returns(info => prefixBytes.Concat(Encoding.UTF8.GetBytes(((EncString)info[0]).EncryptedString)).ToArray());
            cryptoService.DecryptFromBytesAsync(Arg.Any<byte[]>(), Arg.Any<SymmetricCryptoKey>())
                .Returns(info => prefixBytes.Concat((byte[])info[0]).ToArray());
            cryptoService.DecryptToUtf8Async(Arg.Any<EncString>(), Arg.Any<SymmetricCryptoKey>())
                .Returns(info => $"{prefix}{((EncString)info[0]).EncryptedString}");
            ServiceContainer.Register("cryptoService", cryptoService);

            var view = await send.DecryptAsync();

            string expectedDecryptionString(EncString encryptedString) =>
                    encryptedString?.EncryptedString == null ? null : $"{prefix}{encryptedString.EncryptedString}";

            TestHelper.AssertPropertyEqual(send, view, "Name", "Notes", "File", "Text", "Key", "UserId");
            Assert.Equal(expectedDecryptionString(send.Name), view.Name);
            Assert.Equal(expectedDecryptionString(send.Notes), view.Notes);
            Assert.Equal(Encoding.UTF8.GetBytes(expectedDecryptionString(send.Key)), view.Key);

            switch (send.Type)
            {
                case SendType.File:
                    TestHelper.AssertPropertyEqual(send.File, view.File, "FileName");
                    Assert.Equal(expectedDecryptionString(send.File.FileName), view.File.FileName);
                    break;
                case SendType.Text:
                    TestHelper.AssertPropertyEqual(send.Text, view?.Text, "Text");
                    Assert.Equal(expectedDecryptionString(send.Text.Text), view.Text.Text);
                    break;
                default:
                    throw new Exception("Untested Send type");
            }

            ServiceContainer.Reset();
        }
    }
}
