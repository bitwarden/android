using System;
using System.Globalization;
using System.Threading;
using Foundation;
using Bit.App.Abstractions;
using Bit.App.Models;

namespace Bit.iOS.Core.Services
{
    public class LocalizeService : ILocalizeService
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
            if(NSLocale.PreferredLanguages.Length > 0)
            {
                var pref = NSLocale.PreferredLanguages[0];

                netLanguage = iOSToDotnetLanguage(pref);
            }

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

        private string iOSToDotnetLanguage(string iOSLanguage)
        {
            Console.WriteLine("iOS Language:" + iOSLanguage);
            var netLanguage = iOSLanguage;

            if(iOSLanguage.StartsWith("zh-Hant"))
            {
                netLanguage = "zh-Hant";
            }
            else if(iOSLanguage.StartsWith("zh"))
            {
                netLanguage = "zh-Hans";
            }
            else if(iOSLanguage.StartsWith("pt"))
            {
                // only Portuguese Europe for now
                netLanguage = "pt-PT";
            }
            else
            {
                //certain languages need to be converted to CultureInfo equivalent
                switch(iOSLanguage)
                {
                    case "ms-MY": // "Malaysian (Malaysia)" not supported .NET culture
                    case "ms-SG": // "Malaysian (Singapore)" not supported .NET culture
                        netLanguage = "ms"; // closest supported
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
                case "pt":
                    netLanguage = "pt-PT"; // fallback to Portuguese (Portugal)
                    break;
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
