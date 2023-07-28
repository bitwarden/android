using Bit.Core.Enums;
using Bit.Core.Models.Api;

namespace Bit.Core.Models.Data
{
    public class SecureNoteData : Data
    {
        public SecureNoteData() { }

        public SecureNoteData(SecureNoteApi data)
        {
            Type = data.Type;
        }

        public SecureNoteType Type { get; set; }
    }
}
