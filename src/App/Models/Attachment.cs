using Bit.App.Models.Api;
using Bit.App.Models.Data;

namespace Bit.App.Models
{
    public class Attachment
    {
        public Attachment()
        { }

        public Attachment(AttachmentData data)
        {
            Id = data.Id;
            Url = data.Url;
            FileName = data.FileName != null ? new CipherString(data.FileName) : null;
            SetSize(data.Size);
            SizeName = data.SizeName;
        }

        public Attachment(AttachmentResponse response)
        {
            Id = response.Id;
            Url = response.Url;
            FileName = response.FileName != null ? new CipherString(response.FileName) : null;
            SetSize(response.Size);
            SizeName = response.SizeName;
        }

        public string Id { get; set; }
        public string Url { get; set; }
        public CipherString FileName { get; set; }
        public long Size { get; set; }
        public string SizeName { get; set; }

        public AttachmentData ToAttachmentData(string loginId)
        {
            return new AttachmentData(this, loginId);
        }

        private void SetSize(string sizeString)
        {
            long size;
            if(!long.TryParse(sizeString, out size))
            {
                size = 0;
            }

            Size = size;
        }
    }
}
