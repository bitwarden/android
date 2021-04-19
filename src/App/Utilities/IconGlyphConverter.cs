using System;
using System.Globalization;
using Bit.Core;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public class IconGlyphConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            var cipher = value as CipherView;
            return GetIcon(cipher);
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }

        private string GetIcon(CipherView cipher)
        {
            string icon = null;
            switch (cipher.Type)
            {
                case CipherType.Login:
                    icon = GetLoginIconGlyph(cipher);
                    break;
                case CipherType.SecureNote:
                    icon = "";
                    break;
                case CipherType.Card:
                    icon = "";
                    break;
                case CipherType.Identity:
                    icon = "";
                    break;
                default:
                    break;
            }
            return icon;
        }

        string GetLoginIconGlyph(CipherView cipher)
        {
            string icon = "";
            if (cipher.Login.Uri != null)
            {
                var hostnameUri = cipher.Login.Uri;
                if (hostnameUri.StartsWith(Constants.AndroidAppProtocol))
                {
                    icon = "";
                }
                else if (hostnameUri.StartsWith(Constants.iOSAppProtocol))
                {
                    icon = "";
                }
            }
            return icon;
        }
    }
}
