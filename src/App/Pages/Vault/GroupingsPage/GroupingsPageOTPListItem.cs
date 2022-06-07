using System;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class GroupingsPageTOTPListItem : ExtendedViewModel, IGroupingsPageListItem
    {
        //private string _totpCode;
        private readonly ITotpService _totpService;

        //public CipherView Cipher { get; set; }
        //public CipherType? Type { get; set; }
        //public int interval { get; set; }
        //public long TotpSec { get; set; }
        //private DateTime? _totpInterval = null;

        private CipherView _cipher;
            
        private bool _websiteIconsEnabled;
        private string _iconImageSource = string.Empty;

        public int interval { get; set; }
        private string _totpSec;

        private string _totpCode;
        private string _totpCodeFormatted = "938 928";


        public GroupingsPageTOTPListItem(CipherView cipherView, bool websiteIconsEnabled)
        {
            _totpService = ServiceContainer.Resolve<ITotpService>("totpService");

            Cipher = cipherView;
            WebsiteIconsEnabled = websiteIconsEnabled;
            interval = _totpService.GetTimeInterval(Cipher.Login.Totp);
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

        public async Task TotpTickAsync()
        {
            var epoc = CoreHelpers.EpocUtcNow() / 1000;
            var mod = epoc % interval;
            var totpSec = interval - mod;
            TotpSec = totpSec.ToString();
            //TotpLow = totpSec < 7;
            if (mod == 0)
            {
                await TotpUpdateCodeAsync();
            }

        }

        public async Task TotpUpdateCodeAsync()
        {
            _totpCode = await _totpService.GetCodeAsync(Cipher.Login.Totp);
            if (_totpCode != null)
            {
                if (_totpCode.Length > 4)
                {
                    var half = (int)Math.Floor(_totpCode.Length / 2M);
                    TotpCodeFormatted = string.Format("{0} {1}", _totpCode.Substring(0, half),
                        _totpCode.Substring(half));
                }
                else
                {
                    TotpCodeFormatted = _totpCode;
                }
            }
            else
            {
                TotpCodeFormatted = null;
            }
        }
    }
}
