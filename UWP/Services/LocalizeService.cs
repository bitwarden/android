using Bit.App.Abstractions;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Globalization;

namespace Bit.UWP.Services
{
    public class LocalizeService : ILocalizeService
    {
        public CultureInfo GetCurrentCultureInfo()
        {
            throw new NotImplementedException();
        }

        public void SetLocale(CultureInfo ci)
        {
            throw new NotImplementedException();
        }
    }
}
