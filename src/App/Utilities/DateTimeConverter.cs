using System;
using Bit.App.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public class DateTimeConverter : IValueConverter
    {
        public string Format { get; set; } = "{0} {1}";

        private readonly ILocalizeService _localizeService;

        public DateTimeConverter()
        {
            _localizeService = ServiceContainer.Resolve<ILocalizeService>("localizeService");
        }

        public object Convert(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            if (targetType != typeof(string))
            {
                throw new InvalidOperationException("The target must be a string.");
            }
            if (value == null)
            {
                return string.Empty;
            }
            var d = ((DateTime)value).ToLocalTime();
            return string.Format(Format,
                _localizeService.GetLocaleShortDate(d),
                _localizeService.GetLocaleShortTime(d));
        }

        public object ConvertBack(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            throw new NotSupportedException();
        }
    }
}
