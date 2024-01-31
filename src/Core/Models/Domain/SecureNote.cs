using System.Threading.Tasks;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.View;

namespace Bit.Core.Models.Domain
{
    public class SecureNote : Domain
    {
        public SecureNote() { }

        public SecureNote(SecureNoteData obj, bool alreadyEncrypted = false)
        {
            Type = obj.Type;
        }

        public SecureNoteType Type { get; set; }

        public Task<SecureNoteView> DecryptAsync(string orgId, SymmetricCryptoKey key = null)
        {
            return Task.FromResult(new SecureNoteView(this));
        }

        public SecureNoteData ToSecureNoteData()
        {
            return new SecureNoteData
            {
                Type = Type
            };
        }
    }
}
