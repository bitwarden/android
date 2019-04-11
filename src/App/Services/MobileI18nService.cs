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
        private const string ResourceId = "UsingResxLocalization.Resx.AppResources";

        private static readonly Lazy<ResourceManager> _resourceManager = new Lazy<ResourceManager>(() =>
            new ResourceManager(ResourceId, IntrospectionExtensions.GetTypeInfo(typeof(MobileI18nService)).Assembly));

        private readonly CultureInfo _defaultCulture = new CultureInfo("en-US");
        private bool _inited;

        public MobileI18nService(CultureInfo systemCulture)
        {
            Culture = systemCulture;
        }

        public CultureInfo Culture { get; set; }

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

        public string T(string id, params string[] p)
        {
            return Translate(id, p);
        }

        public string Translate(string id, params string[] p)
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
            return string.Format(result, p);
        }
    }
}
