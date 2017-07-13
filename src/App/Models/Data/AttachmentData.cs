using SQLite;
using Bit.App.Abstractions;
using Bit.App.Models.Api;

namespace Bit.App.Models.Data
{
    [Table("Attachment")]
    public class AttachmentData : IDataObject<string>
    {
        public AttachmentData()
        { }

        public AttachmentData(Attachment attachment, string loginId)
        {
            Id = attachment.Id;
            LoginId = loginId;
            Url = attachment.Url;
            FileName = attachment.FileName?.EncryptedString;
            Size = attachment.Size.ToString();
            SizeName = attachment.SizeName;
        }

        public AttachmentData(AttachmentResponse response, string loginId)
        {
            Id = response.Id;
            LoginId = loginId;
            Url = response.Url;
            FileName = response.FileName;
            Size = response.Size;
            SizeName = response.SizeName;
        }

        [PrimaryKey]
        public string Id { get; set; }
        [Indexed]
        public string LoginId { get; set; }
        public string Url { get; set; }
        public string FileName { get; set; }
        public string Size { get; set; }
        public string SizeName { get; set; }

        public Attachment ToAttachment()
        {
            return new Attachment(this);
        }
    }
}
