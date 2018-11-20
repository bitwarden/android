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
            Key = data.Key != null ? new CipherString(data.Key) : null;
            SetSize(data.Size);
            SizeName = data.SizeName;
        }

        public Attachment(AttachmentResponse response)
        {
            Id = response.Id;
            Url = response.Url;
            FileName = response.FileName != null ? new CipherString(response.FileName) : null;
            Key = response.Key != null ? new CipherString(response.Key) : null;
            SetSize(response.Size);
            SizeName = response.SizeName;
        }

        public string Id { get; set; }
        public string Url { get; set; }
        public CipherString FileName { get; set; }
        public CipherString Key { get; set; }
        public long Size { get; set; }
        public string SizeName { get; set; }

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
