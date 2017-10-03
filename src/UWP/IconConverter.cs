using System;
using Windows.UI.Xaml.Data;

namespace Bit.UWP
{
    public class IconConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, string language)
        {
            if(value != null && value is Xamarin.Forms.FileImageSource)
            {
                return ((Xamarin.Forms.FileImageSource)value).File;
            }

            return null;
        }

        public object ConvertBack(object value, Type targetType, object parameter, string language)
        {
            throw new NotImplementedException();
        }
    }
}
