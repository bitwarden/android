using System;
using System.Globalization;
using Bit.Core;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

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
                    icon = BitwardenIcons.FileText;
                    break;
                case SendType.File:
                    icon = BitwardenIcons.File;
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
