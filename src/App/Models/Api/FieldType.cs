namespace Bit.App.Models.Api
{
    public class FieldType
    {
        public FieldType() { }

        public FieldType(Field field)
        {
            Type = field.Type;
            Name = field.Name?.EncryptedString;
            Value = field.Value?.EncryptedString;
        }

        public Enums.FieldType Type { get; set; }
        public string Name { get; set; }
        public string Value { get; set; }
    }
}
