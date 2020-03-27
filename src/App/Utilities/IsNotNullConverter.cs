using System;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public class IsNotNullConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            if (targetType == typeof(bool))
            {
                return value != null;
            }
            throw new InvalidOperationException("The target must be a boolean.");
        }

        public object ConvertBack(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            throw new NotSupportedException();
        }
    }
}
