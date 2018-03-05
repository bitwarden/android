using Bit.App.Models.Api;

namespace Bit.App.Models.Data
{
    public class FieldDataModel
    {
        public FieldDataModel() { }

        public FieldDataModel(FieldType f)
        {
            Type = f.Type;
            Name = f.Name;
            Value = f.Value;
        }

        public Enums.FieldType Type { get; set; }
        public string Name { get; set; }
        public string Value { get; set; }
    }
}
