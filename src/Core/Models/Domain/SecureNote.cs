using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.View;
using System.Threading.Tasks;

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

        public Task<SecureNoteView> DecryptAsync(string orgId)
        {
            return Task.FromResult(new SecureNoteView(this));
        }

        public SecureNoteData ToLoginUriData()
        {
            return new SecureNoteData
            {
                Type = Type
            };
        }
    }
}
