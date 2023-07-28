using System;

namespace Bit.Core.Models.Request
{
    public class AttachmentRequest
    {
        public string FileName { get; set; }
        public string Key { get; set; }
        public long FileSize { get; set; }
    }
}
