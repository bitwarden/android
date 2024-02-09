using System;
using System.Globalization;

namespace Bit.App.Abstractions
{
    public interface ILocalizeService
    {
        CultureInfo GetCurrentCultureInfo();

        /// <summary>
        /// Format date using device locale.
        /// Needed for iOS as it provides locales unsupported in .Net
        /// </summary>
        string GetLocaleShortDate(DateTime? date);

        /// <summary>
        /// Format time using device locale.
        /// Needed for iOS as it provides locales unsupported in .Net
        /// </summary>
        string GetLocaleShortTime(DateTime? time);
    }
}
