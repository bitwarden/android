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

        private bool _needCredentials = true;
        public bool NeedCredentials { get { return _needCredentials; } set { _needCredentials = value; TriggerPropertyChanged(nameof(NeedCredentials)); } }
        private bool _needCaptcha = true;
        public bool NeedCaptcha { get { return _needCaptcha; } set { _needCaptcha = value; TriggerPropertyChanged(nameof(NeedCaptcha)); } }
        private bool _needEmailCode = true;
        public bool NeedEmailCode { get { return _needEmailCode; } set { _needEmailCode = value; TriggerPropertyChanged(nameof(NeedEmailCode)); } }
        private bool _needSMSCode = true;
        public bool NeedSMSCode { get { return _needSMSCode; } set { _needSMSCode = value; TriggerPropertyChanged(nameof(NeedSMSCode)); } }

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
    }
}
