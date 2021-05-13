using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.App.Controls
{
    public class CipherViewCellViewModel : ExtendedViewModel
    {
        private CipherView _cipher;
        private bool _websiteIconsEnabled;

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
            get => WebsiteIconsEnabled && !string.IsNullOrWhiteSpace(Cipher.Login?.Uri) &&
                   Cipher.Login.Uri.StartsWith("http");
        }
    }
}
