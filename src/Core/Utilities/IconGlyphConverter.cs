using System;
using System.Globalization;
using Bit.App.Pages;
using Bit.Core.Models.View;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Utilities
{
    public class IconGlyphConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is CipherItemViewModel cipherItemViewModel)
            {
                return cipherItemViewModel.Cipher?.GetIcon(cipherItemViewModel.UsePasskeyIconAsPlaceholderFallback);
            }

            if (value is CipherView cipher)
            {
                return cipher.GetIcon();
            }

            if (value is bool boolVal
                &&
                parameter is BooleanGlyphType boolGlyphType)
            {
                return IconGlyphExtensions.GetBooleanIconGlyph(boolVal, boolGlyphType);
            }

            return null;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }
}
