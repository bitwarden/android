using System;
using System.Collections.Generic;
using System.Text;
using Bit.Core.Enums;

namespace Bit.App.Abstractions
{
    public interface ISteamGuardService
    { 
        string TOTPSecret { get; }
        string RecoveryCode { get; }
        string CaptchaGID { get; }
        SteamGuardServiceResponse SubmitUsernamePassword(string username, string password);
        SteamGuardServiceResponse SubmitCaptcha(string captcha);
        SteamGuardServiceResponse SubmitEmailCode(string code);
        SteamGuardServiceResponse SubmitSMSCode(string code);
        void RequestSMSCode();
    }
}
