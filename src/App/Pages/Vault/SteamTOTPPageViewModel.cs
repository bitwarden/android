using System;
using System.ComponentModel.Design;
using Bit.App.Abstractions;
using Bit.App.Services.Steam;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class SteamTOTPPageViewModel : BaseViewModel
    {
        public string Password { get; set; }
        public string Username { get; set; }
        public string Captcha { get; set; }
        public string EmailCode { get; set; }
        public string SMSCode { get; set; }

        private Uri _captchaURI;
        public Uri CaptchaURI { get { return _captchaURI; } set { _captchaURI = value; TriggerPropertyChanged(nameof(CaptchaURI)); } }

        public Command SubmitUsernamePasswordCommand { get; set; }
        public Command SubmitCaptchaCommand { get; set; }
        public Command SubmitEmailCodeCommand { get; set; }
        public Command SubmitSMSCodeCommand { get; set; }
        public Command TogglePasswordCommand { get; set; }

        private bool _needCredentials = true;
        public bool NeedCredentials { get { return _needCredentials; } set { _needCredentials = value; TriggerPropertyChanged(nameof(NeedCredentials)); } }
        private bool _needCaptcha;
        public bool NeedCaptcha { get { return _needCaptcha; } set { _needCaptcha = value; if (value) ShowCaptcha = true; TriggerPropertyChanged(nameof(NeedCaptcha)); } }
        private bool _needEmailCode;
        public bool NeedEmailCode { get { return _needEmailCode; } set { _needEmailCode = value; if (value) ShowEmailCode = true; TriggerPropertyChanged(nameof(NeedEmailCode)); } }
        private bool _needSMSCode;
        public bool NeedSMSCode { get { return _needSMSCode; } set { _needSMSCode = value; if (value) ShowSMSCode = true; TriggerPropertyChanged(nameof(NeedSMSCode)); } }

        private bool _showCaptcha;
        public bool ShowCaptcha { get { return _showCaptcha; } set { _showCaptcha = value; TriggerPropertyChanged(nameof(ShowCaptcha)); } }
        private bool _showEmailCode;
        public bool ShowEmailCode { get { return _showEmailCode; } set { _showEmailCode = value; TriggerPropertyChanged(nameof(ShowEmailCode)); } }
        private bool _showSMSCode;
        public bool ShowSMSCode { get { return _showSMSCode; } set { _showSMSCode = value; TriggerPropertyChanged(nameof(ShowSMSCode)); } }

        public string ShowPasswordIcon => ShowPassword ? "" : "";

        private bool _showPassword;
        public bool ShowPassword
        {
            get => _showPassword;
            set => SetProperty(ref _showPassword, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowPasswordIcon)
                });
        }


        private string _status = "test";
        public string Status { get { return _status; } set { _status = value; TriggerPropertyChanged(nameof(Status)); } }

        private ISteamGuardService steamGuardService = new SteamGuardService();
 
        public Action<string, string> SteamLinkedCallback;

        public SteamTOTPPageViewModel()
        {
            SubmitUsernamePasswordCommand = new Command(SubmitUsernamePassword);
            SubmitCaptchaCommand = new Command(SubmitCaptcha);
            SubmitEmailCodeCommand = new Command(SubmitEmailCode);
            SubmitSMSCodeCommand = new Command(SubmitSMSCode);
            TogglePasswordCommand = new Command(TogglePassword);
        }

        public async void SubmitUsernamePassword()
        {
            if(!string.IsNullOrEmpty(Username) && !string.IsNullOrEmpty(Password))
            {
                NeedCredentials = false;
                switch(await steamGuardService.SubmitUsernamePassword(Username, Password))
                {
                    case SteamGuardServiceResponse.NeedCaptcha:
                        CaptchaURI = new Uri("https://steamcommunity.com/public/captcha.php?gid=" + steamGuardService.CaptchaGID);
                        NeedCaptcha = true;
                        break;
                    case SteamGuardServiceResponse.NeedEmailCode:
                        NeedEmailCode = true;
                        break;
                }
            }
        }

        public async void SubmitCaptcha()
        {
            if (!string.IsNullOrEmpty(Captcha))
            {
                NeedCaptcha = false;
                switch (await steamGuardService.SubmitCaptcha(Captcha))
                {
                    case SteamGuardServiceResponse.NeedCaptcha:
                        CaptchaURI = new Uri("https://steamcommunity.com/public/captcha.php?gid=" + steamGuardService.CaptchaGID);
                        NeedCaptcha = true;
                        break;
                    case SteamGuardServiceResponse.NeedEmailCode:
                        NeedEmailCode = true;
                        break;
                }
            }
        }

        public async void SubmitEmailCode()
        {
            if (!string.IsNullOrEmpty(EmailCode))
            {
                NeedEmailCode = false;
                switch (await steamGuardService.SubmitEmailCode(EmailCode))
                {
                    case SteamGuardServiceResponse.NeedSMSCode:
                        steamGuardService.RequestSMSCode();
                        NeedSMSCode = true;
                        break;
                    case SteamGuardServiceResponse.NeedEmailCode:
                        NeedEmailCode = true;
                        break;
                }
            }
        }

        public async void SubmitSMSCode()
        {
            if (!string.IsNullOrEmpty(SMSCode))
            {
                NeedSMSCode = false;
                switch (await steamGuardService.SubmitSMSCode(SMSCode))
                {
                    case SteamGuardServiceResponse.NeedSMSCode:
                        NeedSMSCode = true;
                        break;
                    case SteamGuardServiceResponse.Okay:
                        SteamLinkedCallback(steamGuardService.TOTPSecret, steamGuardService.RecoveryCode);
                        break;
                }
            }
        }

        public void TogglePassword()
        {
            ShowPassword = !ShowPassword;
        }
    }
}
