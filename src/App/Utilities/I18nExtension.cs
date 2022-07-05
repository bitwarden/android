using System;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Bit.App.Utilities
{
    [ContentProperty("Id")]
    public class I18nExtension : IMarkupExtension
    {
        private II18nService _i18nService;

        public I18nExtension()
        {
            _i18nService = ServiceContainer.Resolve<II18nService>("i18nService");
        }

        public string Id { get; set; }
        public string P1 { get; set; }
        public string P2 { get; set; }
        public string P3 { get; set; }
        public bool Header { get; set; }

        public object ProvideValue(IServiceProvider serviceProvider)
        {
            var val = _i18nService.T(Id, P1, P2, P3);
            /*
            if (Header && Device.RuntimePlatform == Device.iOS)
            {
                return val.ToUpper();
            }
            */
            return val;
        }
    }
}
