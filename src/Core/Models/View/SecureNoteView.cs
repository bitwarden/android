using Bit.Core.Enums;
using Bit.Core.Models.Domain;

namespace Bit.Core.Models.View
{
    public class SecureNoteView : View
    {
        public SecureNoteView() { }

        public SecureNoteView(SecureNote n)
        {
            Type = n.Type;
        }

        public SecureNoteType Type { get; set; }
        public string SubTitle => null;
    }
}
