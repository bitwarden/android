using System.Collections.Generic;
using Bit.App.Abstractions;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Enums;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class CipherViewModelSwipeableItem : ISwipeableItem<CipherViewCellViewModel>
    {
        readonly Dictionary<CipherType, FontImageSource> _imageCache = new Dictionary<CipherType, FontImageSource>();

        public bool CanSwipe(CipherViewCellViewModel item)
        {
            if (item?.Cipher is null)
            {
                return false;
            }

            return item.Cipher.Type == CipherType.Login
                   ||
                   item.Cipher.Type == CipherType.Card
                   ||
                   item.Cipher.Type == CipherType.SecureNote;
        }

        public Xamarin.Forms.Color GetBackgroundColor(CipherViewCellViewModel item)
        {
            if (item?.Cipher is null)
            {
                return ThemeManager.GetResourceColor("PrimaryColor");
            }

            if (item.Cipher.Type == CipherType.Login
                &&
                string.IsNullOrEmpty(item.Cipher.Login?.Password))
            {
                return ThemeManager.GetResourceColor("SeparatorColor");
            }

            if (item.Cipher.Type == CipherType.Card
                &&
                string.IsNullOrEmpty(item.Cipher.Card?.Number))
            {
                return ThemeManager.GetResourceColor("SeparatorColor");
            }

            if (item.Cipher.Type == CipherType.SecureNote
                &&
                string.IsNullOrEmpty(item.Cipher.Notes))
            {
                return ThemeManager.GetResourceColor("SeparatorColor");
            }

            return ThemeManager.GetResourceColor("PrimaryColor");
        }

        public FontImageSource GetSwipeIcon(CipherViewCellViewModel item)
        {
            if (item?.Cipher is null)
            {
                return null;
            }

            if (!_imageCache.TryGetValue(item.Cipher.Type, out var image))
            {
                image = new IconFontImageSource { Color = ThemeManager.GetResourceColor("BackgroundColor") };
                switch (item.Cipher.Type)
                {
                    case CipherType.Login:
                        image.Glyph = BitwardenIcons.Key;
                        break;
                    case CipherType.Card:
                        image.Glyph = BitwardenIcons.Hashtag;
                        break;
                    case CipherType.SecureNote:
                        image.Glyph = BitwardenIcons.Clone;
                        break;
                    default:
                        return null;
                }
                _imageCache.Add(item.Cipher.Type, image);
            }

            return image;
        }
    }
}
