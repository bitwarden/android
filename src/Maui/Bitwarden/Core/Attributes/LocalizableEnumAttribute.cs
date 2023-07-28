using System;
namespace Bit.Core.Attributes
{
    [AttributeUsage(AttributeTargets.Field | AttributeTargets.Property, AllowMultiple = false)]
    public class LocalizableEnumAttribute : Attribute
    {
        public LocalizableEnumAttribute(string key)
        {
            Key = key;
        }

        public string Key { get; }
    }
}
