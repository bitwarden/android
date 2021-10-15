using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using System.Collections.Generic;

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
        public override List<KeyValuePair<string, int>> LinkedMetadata => null;
    }
}
