using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Bit.App.Utilities
{
    [ContentProperty("Text")]
    public class TranslateExtension : IMarkupExtension
    {
        private II18nService _i18nService;

        public TranslateExtension()
        {
            _i18nService = ServiceContainer.Resolve<II18nService>("i18nService");
        }

        public string Id { get; set; }
        public string P1 { get; set; }
        public string P2 { get; set; }
        public string P3 { get; set; }

        public object ProvideValue(IServiceProvider serviceProvider)
        {
            return _i18nService.T(Id, P1, P2, P3);
        }
    }
}
