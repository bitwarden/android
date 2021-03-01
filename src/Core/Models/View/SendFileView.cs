using System.Dynamic;
using Bit.Core.Models.Domain;

namespace Bit.Core.Models.View
{
    public class SendFileView : View
    {
        public SendFileView() : base() { }

        public SendFileView(SendFile file)
        {
            Id = file.Id;
            Size = file.Size;
            SizeName = file.SizeName;
        }

        public string Id { get; set; }
        public string Size { get; set; }
        public string SizeName { get; set; }
        public string FileName { get; set; }
        public int FileSize => int.TryParse(Size ?? "0", out var sizeInt) ? sizeInt : 0;
    }
}
