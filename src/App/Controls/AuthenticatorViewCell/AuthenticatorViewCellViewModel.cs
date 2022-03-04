using System.Threading.Tasks;
using Bit.App.Utilities;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class AuthenticatorViewCellViewModel : ExtendedViewModel
    {
        private CipherView _cipher;
        private string _totpCodeFormatted = "938 928";
        private string _totpSec;
        private bool _websiteIconsEnabled;
        private string _iconImageSource = string.Empty;

        public AuthenticatorViewCellViewModel(CipherView cipherView, bool websiteIconsEnabled)
        {
            Cipher = cipherView;
            WebsiteIconsEnabled = websiteIconsEnabled;
        }

        public Command CopyCommand { get; set; }

        public CipherView Cipher
        {
            get => _cipher;
            set => SetProperty(ref _cipher, value);
        }

        public string TotpCodeFormatted
        {
            get => _totpCodeFormatted;
            set => SetProperty(ref _totpCodeFormatted, value);
        }

        public string TotpSec
        {
            get => _totpSec;
            set => SetProperty(ref _totpSec, value);
        }

        public bool WebsiteIconsEnabled
        {
            get => _websiteIconsEnabled;
            set => SetProperty(ref _websiteIconsEnabled, value);
        }

        public bool ShowIconImage
        {
            get => WebsiteIconsEnabled
                && !string.IsNullOrWhiteSpace(Cipher.Login?.Uri)
                && IconImageSource != null;
        }

        public string IconImageSource
        {
            get
            {
                if (_iconImageSource == string.Empty) // default value since icon source can return null
                {
                    _iconImageSource = IconImageHelper.GetLoginIconImage(Cipher);
                }
                return _iconImageSource;
            }

        }

        public void UpdateTotpSec(long totpSec)
        {
            _totpSec = totpSec.ToString();
        }

        //private async Task TotpUpdateCodeAsync()
        //{
        //    if (Cipher == null || Cipher.Type != Core.Enums.CipherType.Login || Cipher.Login.Totp == null)
        //    {
        //        _totpInterval = null;
        //        return;
        //    }
        //    _totpCode = await _totpService.GetCodeAsync(Cipher.Login.Totp);
        //    if (_totpCode != null)
        //    {
        //        if (_totpCode.Length > 4)
        //        {
        //            var half = (int)Math.Floor(_totpCode.Length / 2M);
        //            TotpCodeFormatted = string.Format("{0} {1}", _totpCode.Substring(0, half),
        //                _totpCode.Substring(half));
        //        }
        //        else
        //        {
        //            TotpCodeFormatted = _totpCode;
        //        }
        //    }
        //    else
        //    {
        //        TotpCodeFormatted = null;
        //        _totpInterval = null;
        //    }
        //}
    }
}

