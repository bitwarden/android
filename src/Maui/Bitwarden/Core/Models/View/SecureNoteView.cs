using System.Collections.Generic;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;

namespace Bit.Core.Models.View
{
    public class SecureNoteView : ItemView
    {
        public SecureNoteView() { }

        public SecureNoteView(SecureNote n)
        {
            Type = n.Type;
        }

        public SecureNoteType Type { get; set; }
        public override string SubTitle => null;
        public override List<KeyValuePair<string, LinkedIdType>> LinkedFieldOptions => null;
    }
}
