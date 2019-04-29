using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Resources;
using Bit.App.Utilities;
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
        private readonly IAuditService _auditService;
        private CipherView _cipher;
        private List<ViewPageFieldViewModel> _fields;
        private bool _canAccessPremium;
        private bool _showPassword;
        private bool _showCardCode;
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
            _auditService = ServiceContainer.Resolve<IAuditService>("auditService");
            CopyCommand = new Command<string>((id) => CopyAsync(id, null));
            CopyUriCommand = new Command<LoginUriView>(CopyUri);
            CopyFieldCommand = new Command<FieldView>(CopyField);
            LaunchUriCommand = new Command<LoginUriView>(LaunchUri);
            TogglePasswordCommand = new Command(TogglePassword);
            ToggleCardCodeCommand = new Command(ToggleCardCode);
            CheckPasswordCommand = new Command(CheckPasswordAsync);

            PageTitle = AppResources.ViewItem;
        }

        public Command CopyCommand { get; set; }
        public Command CopyUriCommand { get; set; }
        public Command CopyFieldCommand { get; set; }
        public Command LaunchUriCommand { get; set; }
        public Command TogglePasswordCommand { get; set; }
        public Command ToggleCardCodeCommand { get; set; }
        public Command CheckPasswordCommand { get; set; }
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
                    nameof(ShowTotp),
                    nameof(ColoredPassword),
                    nameof(ShowIdentityAddress),
                });
        }
        public List<ViewPageFieldViewModel> Fields
        {
            get => _fields;
            set => SetProperty(ref _fields, value);
        }
        public bool CanAccessPremium
        {
            get => _canAccessPremium;
            set => SetProperty(ref _canAccessPremium, value);
        }
        public bool ShowPassword
        {
            get => _showPassword;
            set => SetProperty(ref _showPassword, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowPasswordIcon)
                });
        }
        public bool ShowCardCode
        {
            get => _showCardCode;
            set => SetProperty(ref _showCardCode, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowCardCodeIcon)
                });
        }
        public bool IsLogin => _cipher?.Type == Core.Enums.CipherType.Login;
        public bool IsIdentity => _cipher?.Type == Core.Enums.CipherType.Identity;
        public bool IsCard => _cipher?.Type == Core.Enums.CipherType.Card;
        public bool IsSecureNote => _cipher?.Type == Core.Enums.CipherType.SecureNote;
        public FormattedString ColoredPassword => PasswordFormatter.FormatPassword(_cipher.Login.Password);
        public bool ShowUris => IsLogin && _cipher.Login.HasUris;
        public bool ShowIdentityAddress => IsIdentity && (
            !string.IsNullOrWhiteSpace(_cipher.Identity.Address1) ||
            !string.IsNullOrWhiteSpace(_cipher.Identity.City) ||
            !string.IsNullOrWhiteSpace(_cipher.Identity.Country));
        public bool ShowFields => _cipher.HasFields;
        public bool ShowTotp => IsLogin && !string.IsNullOrWhiteSpace(_cipher.Login.Totp) &&
            !string.IsNullOrWhiteSpace(TotpCodeFormatted);
        public string ShowPasswordIcon => _showPassword ? "" : "";
        public string ShowCardCodeIcon => _showCardCode ? "" : "";
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
            Fields = Cipher.Fields?.Select(f => new ViewPageFieldViewModel(f)).ToList();

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

        public void TogglePassword()
        {
            ShowPassword = !ShowPassword;
        }

        public void ToggleCardCode()
        {
            ShowCardCode = !ShowCardCode;
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

        private async void CheckPasswordAsync()
        {
            if(string.IsNullOrWhiteSpace(Cipher.Login?.Password))
            {
                return;
            }
            await _deviceActionService.ShowLoadingAsync(AppResources.CheckingPassword);
            var matches = await _auditService.PasswordLeakedAsync(Cipher.Login.Password);
            await _deviceActionService.HideLoadingAsync();
            if(matches > 0)
            {
                await _platformUtilsService.ShowDialogAsync(string.Format(AppResources.PasswordExposed, matches));
            }
            else
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.PasswordSafe);
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
            else if(id == "FieldValue")
            {
                name = AppResources.Value;
            }
            else if(id == "CardNumber")
            {
                text = Cipher.Card.Number;
                name = AppResources.Number;
            }
            else if(id == "CardCode")
            {
                text = Cipher.Card.Code;
                name = AppResources.SecurityCode;
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

        private void CopyUri(LoginUriView uri)
        {
            CopyAsync("LoginUri", uri.Uri);
        }

        private void CopyField(FieldView field)
        {
            CopyAsync("FieldValue", field.Value);
        }

        private void LaunchUri(LoginUriView uri)
        {
            if(uri.CanLaunch)
            {
                _platformUtilsService.LaunchUri(uri.LaunchUri);
            }
        }
    }

    public class ViewPageFieldViewModel : BaseViewModel
    {
        private FieldView _field;
        private bool _showHiddenValue;

        public ViewPageFieldViewModel(FieldView field)
        {
            Field = field;
            ToggleHiddenValueCommand = new Command(ToggleHiddenValue);
        }

        public FieldView Field
        {
            get => _field;
            set => SetProperty(ref _field, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ValueText),
                    nameof(IsBooleanType),
                    nameof(IsHiddenType),
                    nameof(IsTextType),
                    nameof(ShowCopyButton),
                });
        }

        public bool ShowHiddenValue
        {
            get => _showHiddenValue;
            set => SetProperty(ref _showHiddenValue, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowHiddenValueIcon)
                });
        }

        public Command ToggleHiddenValueCommand { get; set; }

        public string ValueText => IsBooleanType ? (_field.Value == "true" ? "" : "") : _field.Value;
        public string ShowHiddenValueIcon => _showHiddenValue ? "" : "";
        public bool IsTextType => _field.Type == Core.Enums.FieldType.Text;
        public bool IsBooleanType => _field.Type == Core.Enums.FieldType.Boolean;
        public bool IsHiddenType => _field.Type == Core.Enums.FieldType.Hidden;
        public bool ShowCopyButton => _field.Type != Core.Enums.FieldType.Boolean &&
            !string.IsNullOrWhiteSpace(_field.Value);

        public void ToggleHiddenValue()
        {
            ShowHiddenValue = !ShowHiddenValue;
        }
    }
}
