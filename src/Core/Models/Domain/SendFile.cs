using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Models.Data;
using Bit.Core.Models.View;

namespace Bit.Core.Models.Domain
{
    public class SendFile : Domain
    {
        public string Id { get; set; }
        public string Size { get; set; }
        public string SizeName { get; set; }
        public EncString FileName { get; set; }

        public SendFile() : base() { }

        public SendFile(SendFileData file, bool alreadyEncrypted = false) : base()
        {
            Size = file.Size;
            BuildDomainModel(this, file, new HashSet<string> { "Id", "SizeName", "FileName" }, alreadyEncrypted, new HashSet<string> { "Id", "SizeName" });
        }

        public Task<SendFileView> DecryptAsync(SymmetricCryptoKey key) =>
            DecryptObjAsync(new SendFileView(this), this, new HashSet<string> { "FileName" }, null, key);
    }
}
