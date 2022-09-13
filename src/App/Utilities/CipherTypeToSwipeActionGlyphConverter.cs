using System;
using System.Globalization;
using Bit.App.Controls;
using Bit.Core;
using Bit.Core.Enums;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public class CipherTypeToSwipeActionGlyphConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            var fontImageSource = new IconFontImageSource();
            if (value is CipherType cipherType)
            {
                if (Application.Current.Resources.TryGetValue("TextColor", out var textColor))
                {
                    fontImageSource.Color = (Color)textColor;
                }

                switch (cipherType)
                {
                    case CipherType.Login:
                        fontImageSource.Glyph = BitwardenIcons.Key;
                        break;
                    case CipherType.Card:
                        fontImageSource.Glyph = BitwardenIcons.Hashtag;
                        break;
                    case CipherType.SecureNote:
                        fontImageSource.Glyph = BitwardenIcons.Clone;
                        break;
                }
            }
            return fontImageSource;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) => throw new NotImplementedException();
    }
}
