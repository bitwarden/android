using System;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Utilities
{
    public class InverseBoolConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            if (targetType == typeof(bool))
            {
                return !((bool?)value).GetValueOrDefault();
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
