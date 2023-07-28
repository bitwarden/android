using System;
using Bit.Core.Enums;

namespace Bit.Core.Models.Response
{
    public class SendFileUploadDataResponse
    {
        public string Url { get; set; }
        public FileUploadType FileUploadType { get; set; }
        public SendResponse SendResponse { get; set; }
    }
}
