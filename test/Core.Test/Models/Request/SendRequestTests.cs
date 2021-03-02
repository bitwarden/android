using System;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Test.AutoFixture;
using Bit.Core.Utilities;
using Bit.Test.Common;
using Bit.Test.Common.AutoFixture.Attributes;
using Xunit;

namespace Bit.Core.Test.Models.Request
{
    public class SendRequestTests
    {
        [Theory]
        [InlineCustomAutoData(new[] { typeof(TextSendCustomization) }, null)]
        [InlineCustomAutoData(new[] { typeof(FileSendCustomization) }, 100)]
        public void SendRequest_FromSend_Success(long? fileLength, Send send)
        {
            var request = new SendRequest(send, fileLength);

            TestHelper.AssertPropertyEqual(send, request, "Id", "AccessId", "UserId", "Name", "Notes", "File", "Text", "Key", "AccessCount", "RevisionDate");
            Assert.Equal(send.Name?.EncryptedString, request.Name);
            Assert.Equal(send.Notes?.EncryptedString, request.Notes);
            Assert.Equal(fileLength, request.FileLength);

            switch (send.Type)
            {
                case SendType.File:
                    // Only sets filename
                    Assert.Equal(send.File.FileName?.EncryptedString, request.File.FileName);
                    break;
                case SendType.Text:
                    TestHelper.AssertPropertyEqual(send.Text, request?.Text, "Text");
                    Assert.Equal(send.Text.Text?.EncryptedString, request.Text.Text);
                    break;
                default:
                    throw new Exception("Untested Send type");
            }

            ServiceContainer.Reset();
        }
    }
}
