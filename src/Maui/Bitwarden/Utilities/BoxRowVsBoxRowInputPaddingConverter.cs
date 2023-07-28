using System;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Utilities
{
    public class BooleanToBoxRowInputPaddingConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            if (targetType == typeof(Thickness))
            {
                return ((bool?)value).GetValueOrDefault() ? new Thickness(0, 10, 0, 0) : new Thickness(0, 10);
            }
            throw new InvalidOperationException("The target must be a thickness.");
        }

        public object ConvertBack(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            throw new NotSupportedException();
        }
    }
}
