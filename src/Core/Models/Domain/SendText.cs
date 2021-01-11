
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Models.Data;
using Bit.Core.Models.View;

namespace Bit.Core.Models.Domain
{
    public class SendText : Domain
    {
        public CipherString Text { get; set; }
        public bool Hidden { get; set; }

        public SendText(SendTextData data, bool alreadyEncrypted = false) : base()
        {
            Hidden = data.Hidden;
            BuildDomainModel(this, data, new HashSet<string> { "Text" }, alreadyEncrypted);
        }

        public Task<SendTextView> DecryptAsync(SymmetricCryptoKey key)
            => DecryptObjAsync(new SendTextView(), this, new HashSet<string> { "Text" }, null, key);
    }
}
