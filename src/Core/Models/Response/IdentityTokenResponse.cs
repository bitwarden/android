namespace Bit.Core.Models.Response
{
    public class IdentityTokenResponse
    {
        public string AccessToken { get; set; }
        public string ExpiresIn { get; set; }
        public string RefreshToken { get; set; }
        public string TokenType { get; set; }
        public string PrivateKey { get; set; }
        public string Key { get; set; }
        public string TwoFactorToken { get; set; }
    }
}
