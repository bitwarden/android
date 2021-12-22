using Bit.Core.Enums;
using Bit.Core.Models.Api;

namespace Bit.Core.Models.Data
{
    public class FieldData : Data
    {
        public FieldData() { }

        public FieldData(FieldApi data)
        {
            Type = data.Type;
            Name = data.Name;
            Value = data.Value;
            LinkedId = data.LinkedId;
        }

        public FieldType Type { get; set; }
        public string Name { get; set; }
        public string Value { get; set; }
        public LinkedIdType? LinkedId { get; set; }
    }
}
