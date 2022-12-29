using System;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    /// <summary>
    /// It localizes an enum value by using the <see cref="Core.Attributes.LocalizableEnumAttribute"/>
    /// </summary>
    public class LocalizableEnumConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            return EnumHelper.GetLocalizedValue(value, value.GetType());
        }

        public object ConvertBack(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            throw new NotSupportedException();
        }
    }
}
