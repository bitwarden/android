using Bit.Core.Models.Domain;

namespace Bit.Core.Models.View
{
    public class AttachmentView : View
    {
        public AttachmentView() { }

        public AttachmentView(Attachment a)
        {
            Id = a.Id;
            Url = a.Url;
            Size = a.Size;
            SizeName = a.SizeName;
        }

        public string Id { get; set; }
        public string Url { get; set; }
        public string Size { get; set; }
        public string SizeName { get; set; }
        public string FileName { get; set; }
        public SymmetricCryptoKey Key { get; set; }

        public long FileSize
        {
            get
            {
                if (!string.IsNullOrWhiteSpace(Size) && long.TryParse(Size, out var s))
                {
                    return s;
                }
                return 0;
            }
        }
    }
}
