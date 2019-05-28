namespace Bit.Core.Models.Request
{
    public class TwoFactorEmailRequest
    {
        public string Email { get; set; }
        public string MasterPasswordHash { get; set; }
    }
}
