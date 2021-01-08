using System;
using Xunit;
using Xunit.Sdk;
using AutoFixture.Xunit2;
using AutoFixture;
using System.Linq;
using AutoFixture.AutoNSubstitute;

namespace Bit.Test.Common.AutoFixture.Attributes
{
    public class SutAutoDataAttribute : CustomAutoDataAttribute
    {
        public SutAutoDataAttribute(params Type[] iCustomizationTypes) : base(
            iCustomizationTypes.Append(typeof(SutProviderCustomization)).ToArray())
        { }
    }
    public class InlineSutAutoDataAttribute : InlineCustomAutoDataAttribute
    {
        public InlineSutAutoDataAttribute(Type[] iCustomizationTypes, params object[] values) : base(
            iCustomizationTypes.Append(typeof(SutProviderCustomization)).ToArray(), values)
        { }

        public InlineSutAutoDataAttribute(ICustomization[] customizations, params object[] values) : base(
            customizations.Append(new SutProviderCustomization()).ToArray(), values)
        { }
    }

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
