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
            LinkedId = f.LinkedId;
        }

        public string Name { get; set; }
        public string Value { get; set; }
        public FieldType Type { get; set; }
        public string MaskedValue => Value != null ? "••••••••" : null;
        public bool NewField { get; set; }
        public LinkedIdType? LinkedId { get; set; }
        public bool BoolValue => bool.TryParse(Value, out var b) && b;
    }
}
