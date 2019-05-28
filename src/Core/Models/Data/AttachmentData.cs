using Bit.Core.Models.Response;

namespace Bit.Core.Models.Data
{
    public class AttachmentData : Data
    {
        public AttachmentData() { }

        public AttachmentData(AttachmentResponse response)
        {
            Id = response.Id;
            Url = response.Url;
            FileName = response.FileName;
            Key = response.Key;
            Size = response.Size;
            SizeName = response.SizeName;
        }

        public string Id { get; set; }
        public string Url { get; set; }
        public string FileName { get; set; }
        public string Key { get; set; }
        public string Size { get; set; }
        public string SizeName { get; set; }
    }
}
