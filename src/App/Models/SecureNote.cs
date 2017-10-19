using Bit.App.Enums;
using Bit.App.Models.Data;

namespace Bit.App.Models
{
    public class SecureNote
    {
        public SecureNote(CipherData data)
        {
            Type = data.SecureNoteType.Value;
        }

        public SecureNoteType Type { get; set; }
    }
}
