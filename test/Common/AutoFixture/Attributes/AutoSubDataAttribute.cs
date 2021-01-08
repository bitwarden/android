using AutoFixture.AutoNSubstitute;

namespace Bit.Test.Common.AutoFixture.Attributes
{
    public class AutoSubDataAttribute : CustomAutoDataAttribute
    {
        public AutoSubDataAttribute() : base(typeof(AutoNSubstituteCustomization))
        { }
    }
}
