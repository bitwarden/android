using Bit.Core.Enums;

namespace Bit.Core.Models.Response
{
    public class AttachmentUploadDataResponse
    {
        public string AttachmentId { get; set; }
        public FileUploadType FileUploadType { get; set; }
        public CipherResponse CipherResponse { get; set; }
        public string Url { get; set; }
    }
}
