using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Bit.Core.Enums;

namespace Bit.App.Abstractions
{
    public interface ISteamGuardService
    { 
        string TOTPSecret { get; }
        string RecoveryCode { get; }
        string CaptchaGID { get; }
        SteamGuardServiceError Error { get; }
        Task<SteamGuardServiceResponse> SubmitUsernamePassword(string username, string password);
        Task<SteamGuardServiceResponse> SubmitCaptcha(string captcha);
        Task<SteamGuardServiceResponse> SubmitEmailCode(string code);
        Task<SteamGuardServiceResponse> SubmitSMSCode(string code);
        void RequestSMSCode();
    }
}
