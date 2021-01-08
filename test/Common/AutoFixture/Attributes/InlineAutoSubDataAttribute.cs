using AutoFixture.AutoNSubstitute;

namespace Bit.Test.Common.AutoFixture.Attributes
{
    public class InlineAutoSubstitutionData : InlineCustomAutoDataAttribute
    {
        public InlineAutoSubstitutionData(params object[] values) : base(new[] { typeof(AutoNSubstituteCustomization) }, values)
        { }
    }
}
