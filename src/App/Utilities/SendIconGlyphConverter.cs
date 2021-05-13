using System;
using System.Globalization;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public class SendIconGlyphConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            var send = value as SendView;
            if (send == null)
            {
                return null;
            }
            string icon = null;
            switch (send.Type)
            {
                case SendType.Text:
                    icon = "\uf0f6"; // fa-file-text-o
                    break;
                case SendType.File:
                    icon = "\uf016"; // fa-file-o
                    break;
            }
            return icon;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }
}
