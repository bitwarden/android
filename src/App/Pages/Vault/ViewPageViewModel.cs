using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class ViewPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly ICipherService _cipherService;
        private readonly IUserService _userService;
        private readonly ITotpService _totpService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private CipherView _cipher;
        private List<ViewFieldViewModel> _fields;
        private bool _canAccessPremium;
        private string _totpCode;
        private string _totpCodeFormatted;
        private string _totpSec;
        private bool _totpLow;
        private DateTime? _totpInterval = null;

        public ViewPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _userService = ServiceContainer.Resolve<IUserService>("userService");
            _totpService = ServiceContainer.Resolve<ITotpService>("totpService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            CopyCommand = new Command<string>((id) => CopyAsync(id, null));
            CopyUriCommand = new Command<LoginUriView>(CopyUriAsync);
            LaunchUriCommand = new Command<LoginUriView>(LaunchUriAsync);

            PageTitle = AppResources.ViewItem;
        }

        public Command CopyCommand { get; set; }
        public Command CopyUriCommand { get; set; }
        public Command LaunchUriCommand { get; set; }
        public string CipherId { get; set; }
        public CipherView Cipher
        {
            get => _cipher;
            set => SetProperty(ref _cipher, value,
                additionalPropertyNames: new string[]
                {
                    nameof(IsLogin),
                    nameof(IsIdentity),
                    nameof(IsCard),
                    nameof(IsSecureNote),
                    nameof(ShowUris),
                    nameof(ShowFields),
                    nameof(ShowNotes),
                    nameof(ShowTotp)
                });
        }
        public List<ViewFieldViewModel> Fields
        {
            get => _fields;
            set => SetProperty(ref _fields, value);
        }
        public bool CanAccessPremium
        {
            get => _canAccessPremium;
            set => SetProperty(ref _canAccessPremium, value);
        }
        public bool IsLogin => _cipher?.Type == Core.Enums.CipherType.Login;
        public bool IsIdentity => _cipher?.Type == Core.Enums.CipherType.Identity;
        public bool IsCard => _cipher?.Type == Core.Enums.CipherType.Card;
        public bool IsSecureNote => _cipher?.Type == Core.Enums.CipherType.SecureNote;
        public bool ShowUris => IsLogin && _cipher.Login.HasUris;
        public bool ShowNotes => !string.IsNullOrWhiteSpace(_cipher.Notes);
        public bool ShowFields => _cipher.HasFields;
        public bool ShowTotp => !string.IsNullOrWhiteSpace(_cipher.Login.Totp) &&
            !string.IsNullOrWhiteSpace(TotpCodeFormatted);
        public string TotpCodeFormatted
        {
            get => _totpCodeFormatted;
            set => SetProperty(ref _totpCodeFormatted, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowTotp)
                });
        }
        public string TotpSec
        {
            get => _totpSec;
            set => SetProperty(ref _totpSec, value);
        }
        public bool TotpLow
        {
            get => _totpLow;
            set
            {
                SetProperty(ref _totpLow, value);
                Page.Resources["textTotp"] = Application.Current.Resources[value ? "text-danger" : "text-default"];
            }
        }

        public async Task LoadAsync()
        {
            CleanUp();
            var cipher = await _cipherService.GetAsync(CipherId);
            Cipher = await cipher.DecryptAsync();
            CanAccessPremium = await _userService.CanAccessPremiumAsync();
            Fields = Cipher.Fields?.Select(f => new ViewFieldViewModel(f)).ToList();

            if(Cipher.Type == Core.Enums.CipherType.Login && !string.IsNullOrWhiteSpace(Cipher.Login.Totp) &&
                (Cipher.OrganizationUseTotp || CanAccessPremium))
            {
                await TotpUpdateCodeAsync();
                var interval = _totpService.GetTimeInterval(Cipher.Login.Totp);
                await TotpTickAsync(interval);
                _totpInterval = DateTime.UtcNow;
                Device.StartTimer(new TimeSpan(0, 0, 1), () =>
                {
                    if(_totpInterval == null)
                    {
                        return false;
                    }
                    var task = TotpTickAsync(interval);
                    return true;
                });
            }
        }

        public void CleanUp()
        {
            _totpInterval = null;
        }

        private async Task TotpUpdateCodeAsync()
        {
            if(Cipher == null || Cipher.Type != Core.Enums.CipherType.Login || Cipher.Login.Totp == null)
            {
                _totpInterval = null;
                return;
            }
            _totpCode = await _totpService.GetCodeAsync(Cipher.Login.Totp);
            if(_totpCode != null)
            {
                if(_totpCode.Length > 4)
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
                _totpInterval = null;
            }
        }

        private async Task TotpTickAsync(int intervalSeconds)
        {
            var epoc = CoreHelpers.EpocUtcNow() / 1000;
            var mod = epoc % intervalSeconds;
            var totpSec = intervalSeconds - mod;
            TotpSec = totpSec.ToString();
            TotpLow = totpSec < 7;
            if(mod == 0)
            {
                await TotpUpdateCodeAsync();
            }
        }

        private async void CopyAsync(string id, string text = null)
        {
            string name = null;
            if(id == "LoginUsername")
            {
                text = Cipher.Login.Username;
                name = AppResources.Username;
            }
            else if(id == "LoginPassword")
            {
                text = Cipher.Login.Password;
                name = AppResources.Password;
            }
            else if(id == "LoginTotp")
            {
                text = _totpCode;
                name = AppResources.VerificationCodeTotp;
            }
            else if(id == "LoginUri")
            {
                name = AppResources.URI;
            }

            if(text != null)
            {
                await _platformUtilsService.CopyToClipboardAsync(text);
                if(!string.IsNullOrWhiteSpace(name))
                {
                    _platformUtilsService.ShowToast("info", null, string.Format(AppResources.ValueHasBeenCopied, name));
                }
            }
        }

        private void CopyUriAsync(LoginUriView uri)
        {
            CopyAsync("LoginUri", uri.Uri);
        }

        private void LaunchUriAsync(LoginUriView uri)
        {
            if(uri.CanLaunch)
            {
                _platformUtilsService.LaunchUri(uri.LaunchUri);
            }
        }
    }
}
