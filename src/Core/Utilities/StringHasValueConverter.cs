using System;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Utilities
{
    public class StringHasValueConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            if (targetType == typeof(bool))
            {
                if (value == null)
                {
                    return false;
                }
                if (value.GetType() == typeof(string))
                {
                    return !string.IsNullOrWhiteSpace((string)value);
                }
            }
            throw new InvalidOperationException("The value must be a string with a boolean target.");
        }

        public object ConvertBack(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            throw new NotSupportedException();
        }
    }
}
