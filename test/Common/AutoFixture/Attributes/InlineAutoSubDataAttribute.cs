using AutoFixture.AutoNSubstitute;

namespace Bit.Test.Common.AutoFixture.Attributes
{
    public class InlineAutoSubDataAttribute : InlineCustomAutoDataAttribute
    {
        public InlineAutoSubDataAttribute(params object[] values) : base(new[] { typeof(AutoNSubstituteCustomization) }, values)
        { }
    }
}
