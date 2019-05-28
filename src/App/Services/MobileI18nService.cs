using Bit.App.Resources;
using Bit.Core.Abstractions;
using System;
using System.Globalization;
using System.Reflection;
using System.Resources;
using System.Threading;

namespace Bit.App.Services
{
    public class MobileI18nService : II18nService
    {
        private const string ResourceId = "Bit.App.Resources.AppResources";

        private static readonly Lazy<ResourceManager> _resourceManager = new Lazy<ResourceManager>(() =>
            new ResourceManager(ResourceId, IntrospectionExtensions.GetTypeInfo(typeof(MobileI18nService)).Assembly));

        private readonly CultureInfo _defaultCulture = new CultureInfo("en-US");
        private bool _inited;
        private StringComparer _stringComparer;

        public MobileI18nService(CultureInfo systemCulture)
        {
            Culture = systemCulture;
        }

        public CultureInfo Culture { get; set; }
        public StringComparer StringComparer
        {
            get
            {
                if(_stringComparer == null)
                {
                    _stringComparer = StringComparer.Create(Culture, false);
                }
                return _stringComparer;
            }
        }

        public void Init(CultureInfo culture = null)
        {
            if(_inited)
            {
                throw new Exception("I18n already inited.");
            }
            _inited = true;
            if(culture != null)
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
            if(string.IsNullOrWhiteSpace(id))
            {
                return string.Empty;
            }
            var result = _resourceManager.Value.GetString(id, Culture);
            if(result == null)
            {
                result = _resourceManager.Value.GetString(id, _defaultCulture);
                if(result == null)
                {
                    result = $"{{{id}}}";
                }
            }
            if(p1 == null && p2 == null && p3 == null)
            {
                return result;
            }
            else if(p2 == null && p3 == null)
            {
                return string.Format(result, p1);
            }
            else if(p3 == null)
            {
                return string.Format(result, p1, p2);
            }
            return string.Format(result, p1, p2, p3);
        }
    }
}
