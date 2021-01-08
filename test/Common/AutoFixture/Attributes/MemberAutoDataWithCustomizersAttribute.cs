using System;
using Xunit;
using Xunit.Sdk;
using AutoFixture.Xunit2;
using AutoFixture;

namespace Bit.Test.Common.AutoFixture.Attributes
{
    /// <summary>
    /// Note, due to a deficiency in AutoFixture, This attribute cannot be used to generate more than one test case.
    /// See https://github.com/AutoFixture/AutoFixture/pull/1164 for more details on a future fix.
    /// </summary>
    public class MemberAutoDataWithCustomizersAttribute : MemberAutoDataAttribute
    {
        public MemberAutoDataWithCustomizersAttribute(string memberName, params Type[] iCustomizationTypes) : base(new CustomAutoDataAttribute(iCustomizationTypes), memberName)
        { }

        public MemberAutoDataWithCustomizersAttribute(string memberName, params ICustomization[] customizations) : base(new CustomAutoDataAttribute(customizations), memberName)
        { }
    }
}
