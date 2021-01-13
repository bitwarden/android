using AutoFixture.AutoNSubstitute;

namespace Bit.Test.Common.AutoFixture.Attributes
{
    /// <summary>
    /// Note, due to a deficiency in AutoFixture, This attribute cannot be used to generate more than one test case.
    /// See https://github.com/AutoFixture/AutoFixture/pull/1164 for more details on a future fix.
    /// </summary>
    public class MemberAutoDataWithSutProviderAttribute : MemberAutoDataWithCustomizersAttribute
    {
        public MemberAutoDataWithSutProviderAttribute(string memberName) : base(memberName, typeof(SutProviderCustomization), typeof(AutoNSubstituteCustomization))
        { }
    }
}
