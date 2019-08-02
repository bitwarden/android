using System;
using System.Collections.Generic;
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


        private string _status = "Please enter your credentials";
        public string Status { get { return _status; } set { _status = value; TriggerPropertyChanged(nameof(Status)); } }

        private ISteamGuardService steamGuardService = new SteamGuardService();
 
        public Action<string, string> SteamLinkedCallback;

        private string statusWait = "Please wait, connecting to Steam servers...";
        private string statusWrongCredential = "Wrong Credentials, please check your username and password!";
        private string statusNeedCaptcha = "Please enter the Captcha";
        private string statusWrongCaptcha = "Please try again, the Captcha you entered seems to be incorrect";
        private string statusNeedEmailCode = "Please enter the email code you received";
        private string statusWrongEmailCode = "Please enter the correct email code. The code supplied was rejected!";
        private string statusNeedSMSCode = "Please enter the SMS code you received";
        private string statusWrongSMSCode = "Please enter the correct SMS code. The code supplied was rejected!";

        private string statusErrorAllreadyConnectedSteamguard = "It seems like you have allready connected SteamGuard to an other device. Please disconnect it and try again.";
        private string statusErrorCorruptRespone = "The Steam server replied with a response we couldn't parse. Maybe try again later.";
        private string statusErrorEmptyResponse = "The Steam server replied nothing. Maybe try again later.";
        private string statusErrorGeneral = "Something unhandled went wrong. Try again";
        private string statusErrorGuardSyncFailed = "We failed to sync the SteamGuard code with the Steam server. Try checking if your systems time is set correctly.";
        private string statusErrorNone = "We failed but we didn't. This should not occur";
        private string statusErrorSuccessMissing = "Despite nothing wrong being detected, the sign in failed";
        private string statusErrorRSAFailed = "We couldn't encrypt your password to send it to Steam. Try restarting the application";
        private string statusErrorLoginFailedTooOften = "You tried signing in to Steam unsucessfully too often. Try again later or from an other network";

        public SteamTOTPPageViewModel()
        {
            SubmitUsernamePasswordCommand = new Command(SubmitUsernamePassword);
            SubmitCaptchaCommand = new Command(SubmitCaptcha);
            SubmitEmailCodeCommand = new Command(SubmitEmailCode);
            SubmitSMSCodeCommand = new Command(SubmitSMSCode);
            TogglePasswordCommand = new Command(TogglePassword);
        }

        private void HandleServiceResponseError()
        {
            switch (steamGuardService.Error)
            {
                case SteamGuardServiceError.AllreadyConnectedSteamguard:
                    Status = statusErrorAllreadyConnectedSteamguard;
                    break;
                case SteamGuardServiceError.CorruptResponse:
                    Status = statusErrorCorruptRespone;
                    break;
                case SteamGuardServiceError.EmptyResponse:
                    Status = statusErrorEmptyResponse;
                    break;
                case SteamGuardServiceError.General:
                    Status = statusErrorGeneral;
                    break;
                case SteamGuardServiceError.GuardSyncFailed:
                    Status = statusErrorGuardSyncFailed;
                    break;
                case SteamGuardServiceError.None:
                    Status = statusErrorNone;
                    break;
                case SteamGuardServiceError.SuccessMissing:
                    Status = statusErrorSuccessMissing;
                    break;
                case SteamGuardServiceError.LoginFailedTooOften:
                    Status = statusErrorLoginFailedTooOften;
                    break;
                case SteamGuardServiceError.RSAFailed:
                    Status = statusErrorRSAFailed;
                    break;
            }
        }

        public async void SubmitUsernamePassword()
        {
            if(!string.IsNullOrEmpty(Username) && !string.IsNullOrEmpty(Password))
            {
                NeedCredentials = false;
                Status = statusWait;
                switch(await steamGuardService.SubmitUsernamePassword(Username, Password))
                {
                    case SteamGuardServiceResponse.NeedCaptcha:
                        CaptchaURI = new Uri("https://steamcommunity.com/public/captcha.php?gid=" + steamGuardService.CaptchaGID);
                        Status = statusNeedEmailCode;
                        NeedCaptcha = true;
                        break;
                    case SteamGuardServiceResponse.NeedEmailCode:
                        Status = statusNeedEmailCode;
                        NeedEmailCode = true;
                        break;
                    case SteamGuardServiceResponse.WrongCredentials:
                        Status = statusWrongCredential;
                        NeedCredentials = true;
                        break;
                    case SteamGuardServiceResponse.Error:
                        HandleServiceResponseError();
                        break;
                }
            }
        }

        public async void SubmitCaptcha()
        {
            if (!string.IsNullOrEmpty(Captcha))
            {
                NeedCaptcha = false;
                Status = statusWait;
                switch (await steamGuardService.SubmitCaptcha(Captcha))
                {
                    case SteamGuardServiceResponse.WrongCaptcha:
                        CaptchaURI = new Uri("https://steamcommunity.com/public/captcha.php?gid=" + steamGuardService.CaptchaGID);
                        NeedCaptcha = true;
                        Status = statusWrongCaptcha;
                        break;
                    case SteamGuardServiceResponse.NeedEmailCode:
                        NeedEmailCode = true;
                        Status = statusNeedEmailCode;
                        break;
                    case SteamGuardServiceResponse.Error:
                        HandleServiceResponseError();
                        break;
                }
            }
        }

        public async void SubmitEmailCode()
        {
            if (!string.IsNullOrEmpty(EmailCode))
            {
                NeedEmailCode = false;
                Status = statusWait;
                switch (await steamGuardService.SubmitEmailCode(EmailCode))
                {
                    case SteamGuardServiceResponse.NeedSMSCode:
                        steamGuardService.RequestSMSCode();
                        NeedSMSCode = true;
                        Status = statusNeedSMSCode;
                        break;
                    case SteamGuardServiceResponse.WrongEmailCode:
                        NeedEmailCode = true;
                        Status = statusWrongEmailCode;
                        break;
                    case SteamGuardServiceResponse.Error:
                        HandleServiceResponseError();
                        break;
                }
            }
        }

        public async void SubmitSMSCode()
        {
            if (!string.IsNullOrEmpty(SMSCode))
            {
                NeedSMSCode = false;
                Status = statusWait;
                switch (await steamGuardService.SubmitSMSCode(SMSCode))
                {
                    case SteamGuardServiceResponse.WrongSMSCode:
                        NeedSMSCode = true;
                        Status = statusWrongSMSCode;
                        break;
                    case SteamGuardServiceResponse.Okay:
                        SteamLinkedCallback(steamGuardService.TOTPSecret, steamGuardService.RecoveryCode);
                        break;
                    case SteamGuardServiceResponse.Error:
                        HandleServiceResponseError();
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
