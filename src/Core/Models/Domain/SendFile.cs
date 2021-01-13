using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Models.Data;
using Bit.Core.Models.View;

namespace Bit.Core.Models.Domain
{
    public class SendFile : Domain
    {
        public string Id { get; set; }
        public string Url { get; set; }
        public string Size { get; set; }
        public string SizeName { get; set; }
        public CipherString FileName { get; set; }

        public SendFile() { }

        public SendFile(SendFileData file, bool alreadyEncrypted = false) : base()
        {
            Size = file.Size;
            BuildDomainModel(this, file, new HashSet<string> { "Id", "Url", "SizeName", "FileName" }, alreadyEncrypted, new HashSet<string> { "Id", "Url", "SizeName" });
        }

        public Task<SendFileView> DecryptAsync(SymmetricCryptoKey key)
            => DecryptObjAsync(new SendFileView(), this, new HashSet<string> { "FileName" }, null, key);
    }
}
