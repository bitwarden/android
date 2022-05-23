using System;
using System.Linq;

namespace Bit.Test.Common.AutoFixture.Attributes
{
    public class SutAutoDataAttribute : CustomAutoDataAttribute
    {
        public SutAutoDataAttribute(params Type[] iCustomizationTypes) : base(
            iCustomizationTypes.Append(typeof(SutProviderCustomization)).ToArray())
        { }
    }
}
