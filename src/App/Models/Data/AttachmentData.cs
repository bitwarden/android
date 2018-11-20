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

        public AttachmentData(Attachment attachment, string cipherId)
        {
            Id = attachment.Id;
            LoginId = cipherId;
            Url = attachment.Url;
            FileName = attachment.FileName?.EncryptedString;
            Key = attachment.Key?.EncryptedString;
            Size = attachment.Size.ToString();
            SizeName = attachment.SizeName;
        }

        public AttachmentData(AttachmentResponse response, string cipherId)
        {
            Id = response.Id;
            LoginId = cipherId;
            Url = response.Url;
            FileName = response.FileName;
            Key = response.Key;
            Size = response.Size;
            SizeName = response.SizeName;
        }

        [PrimaryKey]
        public string Id { get; set; }
        // Really should be called CipherId
        [Indexed]
        public string LoginId { get; set; }
        public string Url { get; set; }
        public string FileName { get; set; }
        public string Key { get; set; }
        public string Size { get; set; }
        public string SizeName { get; set; }

        public Attachment ToAttachment()
        {
            return new Attachment(this);
        }
    }
}
