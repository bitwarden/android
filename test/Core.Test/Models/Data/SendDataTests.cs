using Bit.Core.Models.Data;
using Bit.Core.Models.Response;
using Bit.Core.Test.AutoFixture;
using Bit.Test.Common;
using Bit.Test.Common.AutoFixture.Attributes;
using Xunit;

namespace Bit.Core.Test.Models.Data
{
    public class SendDataTests
    {
        [Theory]
        [InlineCustomAutoData(new[] { typeof(TextSendCustomization) })]
        [InlineCustomAutoData(new[] { typeof(FileSendCustomization) })]
        public void SendData_FromSendResponse_Success(string userId, SendResponse response)
        {
            var data = new SendData(response, userId);

            TestHelper.AssertPropertyEqual(response, data, "UserId");
            Assert.Equal(data.UserId, userId);
        }
    }
}
