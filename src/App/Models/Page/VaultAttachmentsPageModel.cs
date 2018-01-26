using System.Collections.Generic;

namespace Bit.App.Models.Page
{
    public class VaultAttachmentsPageModel
    {
        public class Attachment : List<Attachment>
        {
            public string Id { get; set; }
            public string Name { get; set; }
            public string SizeName { get; set; }
            public long Size { get; set; }
            public string Url { get; set; }

            public Attachment(Models.Attachment attachment, string orgId)
            {
                Id = attachment.Id;
                Name = attachment.FileName?.Decrypt(orgId);
                SizeName = attachment.SizeName;
                Size = attachment.Size;
                Url = attachment.Url;
            }
        }
    }
}
