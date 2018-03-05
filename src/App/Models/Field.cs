using Bit.App.Enums;
using Bit.App.Models.Data;

namespace Bit.App.Models
{
    public class Field
    {
        public Field() { }

        public Field(FieldDataModel model)
        {
            Type = model.Type;
            Name = model.Name != null ? new CipherString(model.Name) : null;
            Value = model.Value != null ? new CipherString(model.Value) : null;
        }

        public FieldType Type { get; set; }
        public CipherString Name { get; set; }
        public CipherString Value { get; set; }
    }
}
