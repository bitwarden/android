using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.Data;
using Bit.Core.Models.View;
using Bit.Core.Services;
using Bit.Core.Utilities;

namespace Bit.Core.Models.Domain
{
    public class Attachment : Domain
    {
        private HashSet<string> _map = new HashSet<string>
        {
            nameof(Id),
            nameof(Url),
            nameof(SizeName),
            nameof(FileName),
            nameof(Key)
        };

        public Attachment() { }

        public Attachment(AttachmentData obj, bool alreadyEncrypted = false)
        {
            Size = obj.Size;
            BuildDomainModel(this, obj, _map, alreadyEncrypted, new HashSet<string> { nameof(Id), nameof(Url), nameof(SizeName) });
        }

        public string Id { get; set; }
        public string Url { get; set; }
        public string Size { get; set; }
        public string SizeName { get; set; }
        public EncString Key { get; set; }
        public EncString FileName { get; set; }

        public async Task<AttachmentView> DecryptAsync(string orgId, SymmetricCryptoKey key = null)
        {
            var view = await DecryptObjAsync(new AttachmentView(this), this, new HashSet<string>
            {
                nameof(FileName)
            }, orgId, key);

            if (Key != null)
            {
                try
                {
                    var cryptoService = ServiceContainer.Resolve<ICryptoService>();

                    var decryptKey = key ?? await cryptoService.GetOrgKeyAsync(orgId);
                    var decValue = await cryptoService.DecryptToBytesAsync(Key, decryptKey);
                    view.Key = new SymmetricCryptoKey(decValue);
                }
                catch (Exception ex)
                {
                    LoggerHelper.LogEvenIfCantBeResolved(ex);
                }
            }
            return view;
        }

        public AttachmentData ToAttachmentData()
        {
            var a = new AttachmentData();
            a.Size = Size;
            BuildDataModel(this, a, _map, new HashSet<string> { nameof(Id), nameof(Url), nameof(SizeName) });
            return a;
        }
    }
}
