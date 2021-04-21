using Bit.Core.Abstractions;
using Bit.Core.Models.Data;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.Core.Models.Domain
{
    public class Attachment : Domain
    {
        private HashSet<string> _map = new HashSet<string>
        {
            "Id",
            "Url",
            "SizeName",
            "FileName",
            "Key"
        };

        public Attachment() { }

        public Attachment(AttachmentData obj, bool alreadyEncrypted = false)
        {
            Size = obj.Size;
            BuildDomainModel(this, obj, _map, alreadyEncrypted, new HashSet<string> { "Id", "Url", "SizeName" });
        }

        public string Id { get; set; }
        public string Url { get; set; }
        public string Size { get; set; }
        public string SizeName { get; set; }
        public EncString Key { get; set; }
        public EncString FileName { get; set; }

        public async Task<AttachmentView> DecryptAsync(string orgId)
        {
            var view = await DecryptObjAsync(new AttachmentView(this), this, new HashSet<string>
            {
                "FileName"
            }, orgId);
            
            if (Key != null)
            {
                var cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
                try
                {
                    var orgKey = await cryptoService.GetOrgKeyAsync(orgId);
                    var decValue = await cryptoService.DecryptToBytesAsync(Key, orgKey);
                    view.Key = new SymmetricCryptoKey(decValue);
                }
                catch
                {
                    // TODO: error?
                }
            }
            return view;
        }

        public AttachmentData ToAttachmentData()
        {
            var a = new AttachmentData();
            a.Size = Size;
            BuildDataModel(this, a, _map, new HashSet<string> { "Id", "Url", "SizeName" });
            return a;
        }
    }
}
