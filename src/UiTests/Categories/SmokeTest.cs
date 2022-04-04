using System;
using NUnit.Framework;

namespace Bit.UITests.Categories
{
    [AttributeUsage(AttributeTargets.Method)]
#pragma warning disable SA1649 // File name should match first type name
    public class SmokeTestAttribute : CategoryAttribute
#pragma warning restore SA1649 // File name should match first type name
    {
    }
}
