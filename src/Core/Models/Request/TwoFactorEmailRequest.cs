namespace Bit.Core.Models.Request
{
    public class TwoFactorEmailRequest
    {
        public string Email { get; set; }
        public string MasterPasswordHash { get; set; }
        public string DeviceIdentifier { get; set; }
        public string SsoEmail2FaSessionToken { get; set; }
    }
}
