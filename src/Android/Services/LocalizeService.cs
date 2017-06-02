using System;
using System.Threading;
using System.Globalization;
using Bit.App.Models;

namespace Bit.Android.Services
{
    public class LocalizeService : App.Abstractions.ILocalizeService
    {
        public void SetLocale(CultureInfo ci)
        {
            Thread.CurrentThread.CurrentCulture = ci;
            Thread.CurrentThread.CurrentUICulture = ci;
            Console.WriteLine("CurrentCulture set: " + ci.Name);
        }

        public CultureInfo GetCurrentCultureInfo()
        {
            var netLanguage = "en";
            var androidLocale = Java.Util.Locale.Default;
            netLanguage = AndroidToDotnetLanguage(androidLocale.ToString().Replace("_", "-"));

            // this gets called a lot - try/catch can be expensive so consider caching or something
            CultureInfo ci = null;
            try
            {
                ci = new CultureInfo(netLanguage);
            }
            catch(CultureNotFoundException e1)
            {
                // iOS locale not valid .NET culture (eg. "en-ES" : English in Spain)
                // fallback to first characters, in this case "en"
                try
                {
                    var fallback = ToDotnetFallbackLanguage(new PlatformCulture(netLanguage));
                    Console.WriteLine(netLanguage + " failed, trying " + fallback + " (" + e1.Message + ")");
                    ci = new CultureInfo(fallback);
                }
                catch(CultureNotFoundException e2)
                {
                    // iOS language not valid .NET culture, falling back to English
                    Console.WriteLine(netLanguage + " couldn't be set, using 'en' (" + e2.Message + ")");
                    ci = new CultureInfo("en");
                }
            }

            return ci;
        }

        private string AndroidToDotnetLanguage(string androidLanguage)
        {
            Console.WriteLine("Android Language:" + androidLanguage);
            var netLanguage = androidLanguage;

            if(androidLanguage.StartsWith("zh"))
            {
                if(androidLanguage.Contains("Hant"))
                {
                    netLanguage = "zh-Hant";
                }
                else
                {
                    netLanguage = "zh-Hans";
                }
            }
            else if(androidLanguage.StartsWith("pt"))
            {
                // only Portuguese Europe for now
                netLanguage = "pt-PT";
            }
            else
            {
                // certain languages need to be converted to CultureInfo equivalent
                switch(androidLanguage)
                {
                    case "ms-BN": // "Malaysian (Brunei)" not supported .NET culture
                    case "ms-MY": // "Malaysian (Malaysia)" not supported .NET culture
                    case "ms-SG": // "Malaysian (Singapore)" not supported .NET culture
                        netLanguage = "ms"; // closest supported
                        break;
                    case "in-ID": // "Indonesian (Indonesia)" has different code in  .NET 
                        netLanguage = "id-ID"; // correct code for .NET
                        break;
                    case "gsw-CH": // "Schwiizertüütsch (Swiss German)" not supported .NET culture
                        netLanguage = "de-CH"; // closest supported
                        break;
                        // add more application-specific cases here (if required)
                        // ONLY use cultures that have been tested and known to work
                }
            }

            Console.WriteLine(".NET Language/Locale:" + netLanguage);
            return netLanguage;
        }

        private string ToDotnetFallbackLanguage(PlatformCulture platCulture)
        {
            Console.WriteLine(".NET Fallback Language:" + platCulture.LanguageCode);
            var netLanguage = platCulture.LanguageCode; // use the first part of the identifier (two chars, usually);

            switch(platCulture.LanguageCode)
            {
                case "gsw":
                    netLanguage = "de-CH"; // equivalent to German (Switzerland) for this app
                    break;
                    // add more application-specific cases here (if required)
                    // ONLY use cultures that have been tested and known to work
            }

            Console.WriteLine(".NET Fallback Language/Locale:" + netLanguage + " (application-specific)");
            return netLanguage;
        }


    }
}