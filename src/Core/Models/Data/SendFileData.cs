using System.Drawing;
using Bit.Core.Models.Api;

namespace Bit.Core.Models.Data
{
    public class SendFileData : Data
    {
        public SendFileData() { }

        public SendFileData(SendFileApi data)
        {
            Id = data.Id;
            FileName = data.FileName;
            Key = data.Key;
            Size = data.Size;
            SizeName = data.SizeName;
        }

        public string Id { get; set; }
        public string FileName { get; set; }
        public string Key { get; set; }
        public string Size { get; set; }
        public string SizeName { get; set; }
    }
}
