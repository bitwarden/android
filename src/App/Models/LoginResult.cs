namespace Bit.App.Models
{
    public class LoginResult
    {
        public bool Success { get; set; }
        public string ErrorMessage { get; set; }
    }

    public class FullLoginResult : LoginResult
    {
        public bool TwoFactorRequired { get; set; }
        public CryptoKey Key { get; set; }
        public string MasterPasswordHash { get; set; }
    }
}
