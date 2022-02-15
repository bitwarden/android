using Bit.Core.Models.Request.IdentityToken;

namespace Bit.Core.Models.Domain
{
    public class LogInCredentials
    {
        public LogInCredentials()
        {
        }
    }

    public class PasswordLogInCredentials : LogInCredentials
    {
        public string Email { get; set; }
        public string MasterPassword { get; set; }
        public string CaptchaToken { get; set; }
        public TokenRequestTwoFactor TwoFactor { get; set; }

        public PasswordLogInCredentials(string email, string masterPassword, string captchaToken, TokenRequestTwoFactor twoFactor)
        {
            Email = email;
            MasterPassword = masterPassword;
            CaptchaToken = captchaToken;
            TwoFactor = twoFactor;
        }
    }

    public class SsoLogInCredentials : LogInCredentials
    {
        public string Code { get; set; }
        public string CodeVerifier { get; set; }
        public string RedirectUri { get; set; }
        public string OrgId { get; set; }
        public TokenRequestTwoFactor TwoFactor { get; set; }

        public SsoLogInCredentials(string code, string codeVerifier, string redirectUri, TokenRequestTwoFactor twoFactor) {
            Code = code;
            CodeVerifier = codeVerifier;
            RedirectUri = redirectUri;
            TwoFactor = twoFactor;
        }
    }
}
