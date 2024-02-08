using System;
using System.Collections.Generic;
using System.Globalization;
using System.Reflection;
using System.Resources;
using System.Threading;
using Bit.Core.Resources.Localization;
using Bit.Core.Abstractions;

namespace Bit.App.Services
{
    public class MobileI18nService : II18nService
    {
        private const string ResourceId = "Bit.Core.Resources.Localization.AppResources";

        private static readonly Lazy<ResourceManager> _resourceManager = new Lazy<ResourceManager>(() =>
            new ResourceManager(ResourceId, IntrospectionExtensions.GetTypeInfo(typeof(MobileI18nService)).Assembly));

        private readonly CultureInfo _defaultCulture = new CultureInfo("en-US");
        private bool _inited;
        private StringComparer _stringComparer;
        private Dictionary<string, string> _localeNames;

        public MobileI18nService(CultureInfo systemCulture)
        {
            Culture = systemCulture;
        }

        public CultureInfo Culture { get; set; }
        public StringComparer StringComparer
        {
            get
            {
                if (_stringComparer == null)
                {
                    _stringComparer = StringComparer.Create(Culture, false);
                }
                return _stringComparer;
            }
        }
        public Dictionary<string, string> LocaleNames
        {
            get
            {
                if (_localeNames == null)
                {
                    _localeNames = new Dictionary<string, string>
                    {
                        ["af"] = "Afrikaans",
                        ["be"] = "Беларуская",
                        ["bg"] = "български",
                        ["ca"] = "català",
                        ["cs"] = "čeština",
                        ["da"] = "Dansk",
                        ["de"] = "Deutsch",
                        ["el"] = "Ελληνικά",
                        ["en"] = "English",
                        ["en-GB"] = "English (British)",
                        ["eo"] = "Esperanto",
                        ["es"] = "Español",
                        ["et"] = "eesti",
                        ["fa"] = "فارسی",
                        ["fi"] = "suomi",
                        ["fr"] = "Français",
                        ["he"] = "עברית",
                        ["hi"] = "हिन्दी",
                        ["hr"] = "hrvatski",
                        ["hu"] = "magyar",
                        ["id"] = "Bahasa Indonesia",
                        ["it"] = "Italiano",
                        ["ja"] = "日本語",
                        ["ko"] = "한국어",
                        ["lv"] = "Latvietis",
                        ["ml"] = "മലയാളം",
                        ["nb"] = "norsk (bokmål)",
                        ["nl"] = "Nederlands",
                        ["pl"] = "Polski",
                        ["pt-BR"] = "Português do Brasil",
                        ["pt-PT"] = "Português",
                        ["ro"] = "română",
                        ["ru"] = "русский",
                        ["sk"] = "slovenčina",
                        ["sv"] = "svenska",
                        ["th"] = "ไทย",
                        ["tr"] = "Türkçe",
                        ["uk"] = "українська",
                        ["vi"] = "Tiếng Việt",
                        ["zh-CN"] = "中文（中国大陆）",
                        ["zh-TW"] = "中文（台灣）"
                    };
                }
                return _localeNames;
            }
        }

        public void Init(CultureInfo culture = null)
        {
            if (_inited)
            {
                throw new Exception("I18n already inited.");
            }
            _inited = true;
            SetCurrentCulture(culture);
        }

        public void SetCurrentCulture(CultureInfo culture)
        {
            if (culture != null)
            {
                Culture = culture;
            }

            AppResources.Culture = Culture;
            Thread.CurrentThread.CurrentCulture = Culture;
            Thread.CurrentThread.CurrentUICulture = Culture;
        }

        public string T(string id, string p1 = null, string p2 = null, string p3 = null)
        {
            return Translate(id, p1, p2, p3);
        }

        public string Translate(string id, string p1 = null, string p2 = null, string p3 = null)
        {
            if (string.IsNullOrWhiteSpace(id))
            {
                return string.Empty;
            }
            var result = _resourceManager.Value.GetString(id, Culture);
            if (result == null)
            {
                result = _resourceManager.Value.GetString(id, _defaultCulture);
                if (result == null)
                {
                    result = $"{{{id}}}";
                }
            }
            if (p1 == null && p2 == null && p3 == null)
            {
                return result;
            }
            else if (p2 == null && p3 == null)
            {
                return string.Format(result, p1);
            }
            else if (p3 == null)
            {
                return string.Format(result, p1, p2);
            }
            return string.Format(result, p1, p2, p3);
        }
    }
}
