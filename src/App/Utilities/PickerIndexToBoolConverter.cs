using System;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public class PickerIndexToBoolConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            try
            {
                var i = GetParameter(parameter);

                var v = (int)value;
                return v == i;
            }
            catch (Exception e)
            {
                throw e;
            }
        }

        public object ConvertBack(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            throw new NotSupportedException();
        }

        int GetParameter(object parameter)
        {
            if (parameter is int)
                return (int)parameter;

            else if (parameter is string)
                return int.Parse((string)parameter);

            return 1;
        }
    }
}
