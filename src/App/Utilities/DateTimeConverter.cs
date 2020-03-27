using System;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public class DateTimeConverter : IValueConverter
    {
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
            return string.Format("{0} {1}", d.ToShortDateString(), d.ToShortTimeString());
        }

        public object ConvertBack(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            throw new NotSupportedException();
        }
    }
}
