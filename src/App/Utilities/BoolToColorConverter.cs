using System;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public class BoolEnablementToTextColorConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            if (targetType == typeof(Color) && value is bool valueBool)
            {
                return valueBool ? ThemeManager.GetResourceColor("TextColor") :
                    ThemeManager.GetResourceColor("MutedColor");
            }
            throw new InvalidOperationException("The value must be a boolean with a Color target.");
        }

        public object ConvertBack(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            throw new NotSupportedException();
        }
    }
}

