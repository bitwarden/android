using System;
using System.Diagnostics;
using System.Globalization;
using Bit.App.Abstractions;
using Bit.App.Models;
using Foundation;

namespace Bit.iOS.Core.Services
{
    public class LocalizeService : ILocalizeService
    {
        public CultureInfo GetCurrentCultureInfo()
        {
            var netLanguage = "en";
            if (NSLocale.PreferredLanguages.Length > 0)
            {
                var pref = NSLocale.PreferredLanguages[0];

                netLanguage = iOSToDotnetLanguage(pref);
            }

            // This gets called a lot - try/catch can be expensive so consider caching or something
            CultureInfo ci = null;
            try
            {
                ci = new CultureInfo(netLanguage);
            }
            catch (CultureNotFoundException e1)
            {
                // iOS locale not valid .NET culture (eg. "en-ES" : English in Spain)
                // fallback to first characters, in this case "en"
                try
                {
                    var fallback = ToDotnetFallbackLanguage(new PlatformCulture(netLanguage));
                    Debug.WriteLine(netLanguage + " failed, trying " + fallback + " (" + e1.Message + ")");
                    ci = new CultureInfo(fallback);
                }
                catch (CultureNotFoundException e2)
                {
                    // iOS language not valid .NET culture, falling back to English
                    Debug.WriteLine(netLanguage + " couldn't be set, using 'en' (" + e2.Message + ")");
                    ci = new CultureInfo("en");
                }
            }

            return ci;
        }

        private string iOSToDotnetLanguage(string iOSLanguage)
        {
            Debug.WriteLine("iOS Language:" + iOSLanguage);
            var netLanguage = iOSLanguage;
            if (iOSLanguage.StartsWith("zh-Hant") || iOSLanguage.StartsWith("zh-HK"))
            {
                netLanguage = "zh-Hant";
            }
            else if (iOSLanguage.StartsWith("zh"))
            {
                netLanguage = "zh-Hans";
            }
            else
            {
                // Certain languages need to be converted to CultureInfo equivalent
                switch (iOSLanguage)
                {
                    case "ms-MY": // "Malaysian (Malaysia)" not supported .NET culture
                    case "ms-SG": // "Malaysian (Singapore)" not supported .NET culture
                        netLanguage = "ms"; // closest supported
                        break;
                    case "gsw-CH": // "Schwiizertüütsch (Swiss German)" not supported .NET culture
                        netLanguage = "de-CH"; // closest supported
                        break;
                    case "pt":
                        netLanguage = "pt-BR"; // iOS default (pt) = Portuguese (Brasil)
                        break;
                        // add more application-specific cases here (if required)
                        // ONLY use cultures that have been tested and known to work
                }
            }

            Debug.WriteLine(".NET Language/Locale:" + netLanguage);
            return netLanguage;
        }

        private string ToDotnetFallbackLanguage(PlatformCulture platCulture)
        {
            Debug.WriteLine(".NET Fallback Language:" + platCulture.LanguageCode);
            // Use the first part of the identifier (two chars, usually);
            var netLanguage = platCulture.LanguageCode;
            switch (platCulture.LanguageCode)
            {
                case "pt":
                    netLanguage = "pt-BR"; // fallback to Portuguese (Brasil)
                    break;
                case "gsw":
                    netLanguage = "de-CH"; // equivalent to German (Switzerland) for this app
                    break;
                    // add more application-specific cases here (if required)
                    // ONLY use cultures that have been tested and known to work
            }
            Debug.WriteLine(".NET Fallback Language/Locale:" + netLanguage + " (application-specific)");
            return netLanguage;
        }

        public string GetLocaleShortDate(DateTime? date)
        {
            using (var df = new NSDateFormatter())
            {
                df.Locale = NSLocale.CurrentLocale;
                df.DateStyle = NSDateFormatterStyle.Short;
                return df.StringFor((NSDate)date);
            }
        }

        public string GetLocaleShortTime(DateTime? time)
        {
            using (var df = new NSDateFormatter())
            {
                df.Locale = NSLocale.CurrentLocale;
                df.TimeStyle = NSDateFormatterStyle.Short;
                return df.StringFor((NSDate)time);
            }
        }
    }
}