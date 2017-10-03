using Bit.App.Abstractions;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Globalization;
using Windows.Globalization;

namespace Bit.UWP.Services
{
    public class LocalizeService : ILocalizeService
    {
        public CultureInfo GetCurrentCultureInfo()
        {
            return CultureInfo.CurrentCulture;
        }

        public void SetLocale(CultureInfo locale)
        {
            CultureInfo.CurrentCulture = locale;
            CultureInfo.CurrentUICulture = locale;
            CultureInfo.DefaultThreadCurrentCulture = locale;
            CultureInfo.DefaultThreadCurrentUICulture = locale;

            ApplicationLanguages.PrimaryLanguageOverride = locale.TwoLetterISOLanguageName;

        }
    }
}
