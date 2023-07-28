using Bit.Core.Enums;

namespace Bit.Core.Models.Api
{
    public class FieldApi
    {
        public FieldType Type { get; set; }
        public string Name { get; set; }
        public string Value { get; set; }
        public LinkedIdType? LinkedId { get; set; }
    }
}
