using Bit.Core.Enums;
using Bit.Core.Models.Domain;

namespace Bit.Core.Models.View
{
    public class FieldView : View
    {
        public FieldView() { }

        public FieldView(Field f)
        {
            Type = f.Type;
        }

        public string Name { get; set; }
        public string Value { get; set; }
        public FieldType Type { get; set; }
        public string MaskedValue => Value != null ? "••••••••" : null;
    }
}
