using Bit.App.Enums;
using Bit.App.Models.Api;

namespace Bit.App.Models
{
    public class Field
    {
        public Field(FieldDataModel model)
        {
            Type = model.Type;
            Name = new CipherString(model.Name);
            Value = new CipherString(model.Value);
        }

        public FieldType Type { get; set; }
        public CipherString Name { get; set; }
        public CipherString Value { get; set; }
    }
}
