using System.Globalization;
using Bit.App.Utilities;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.App.Controls
{
    public class CipherViewToCipherViewCellViewModelConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is CipherView cipher)
            {
                return new CipherViewCellViewModel(cipher, false);
            }
            return null;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) => throw new NotImplementedException();
    }

    public class CipherViewCellViewModel : ExtendedViewModel
    {
        private CipherView _cipher;
        private bool _websiteIconsEnabled;
        private string _iconImageSource = string.Empty;

        public CipherViewCellViewModel(CipherView cipherView, bool websiteIconsEnabled)
        {
            Cipher = cipherView;
            WebsiteIconsEnabled = websiteIconsEnabled;
        }

        public CipherView Cipher
        {
            get => _cipher;
            set => SetProperty(ref _cipher, value);
        }

        public bool WebsiteIconsEnabled
        {
            get => _websiteIconsEnabled;
            set => SetProperty(ref _websiteIconsEnabled, value);
        }

        public bool ShowIconImage
        {
            get => WebsiteIconsEnabled
                && !string.IsNullOrWhiteSpace(Cipher.LaunchUri)
                && IconImageSource != null;
        }

        public string IconImageSource
        {
            get
            {
                if (_iconImageSource == string.Empty) // default value since icon source can return null
                {
                    _iconImageSource = IconImageHelper.GetIconImage(Cipher);
                }
                return _iconImageSource;
            }

        }
    }
}
