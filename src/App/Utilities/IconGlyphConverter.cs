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
                    icon = "\uf24a"; // fa-sticky-note-o
                    break;
                case CipherType.Card:
                    icon = "\uf09d"; // fa-credit-card
                    break;
                case CipherType.Identity:
                    icon = "\uf2c3"; // fa-id-card-o
                    break;
                default:
                    break;
            }
            return icon;
        }

        string GetLoginIconGlyph(CipherView cipher)
        {
            var icon = "\uf0ac"; // fa-globe
            if (cipher.Login.Uri != null)
            {
                var hostnameUri = cipher.Login.Uri;
                if (hostnameUri.StartsWith(Constants.AndroidAppProtocol))
                {
                    icon = "\uf17b"; // fa-android
                }
                else if (hostnameUri.StartsWith(Constants.iOSAppProtocol))
                {
                    icon = "\uf179"; // fa-apple
                }
            }
            return icon;
        }
    }
}
