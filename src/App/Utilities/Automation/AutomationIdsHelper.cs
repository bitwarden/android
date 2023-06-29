using System;
using System.Globalization;

namespace Bit.App.Utilities.Automation
{
    public static class AutomationIdsHelper
    {
        public static string ToEnglishTitleCase(string name)
        {
            return new CultureInfo("en-US", false)
                    .TextInfo
                    .ToTitleCase(name)
                    .Replace(" ", String.Empty)
                    .Replace("-", String.Empty);
        }

        public static string AddSuffixFor(string text, SuffixType type)
        {
            return $"{text}{Enum.GetName(typeof(SuffixType), type)}";
        }
    }
}

