using AutoFixture.AutoNSubstitute;

namespace Bit.Test.Common.AutoFixture.Attributes
{
    public class AutoSubstitutionData : CustomAutoDataAttribute
    {
        public AutoSubstitutionData() : base(typeof(AutoNSubstituteCustomization))
        { }
    }
}
