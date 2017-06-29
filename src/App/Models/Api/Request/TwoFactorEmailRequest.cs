namespace Bit.App.Models.Api
{
    public class TwoFactorEmailRequest
    {
        public string Email { get; set; }
        public string MasterPasswordHash { get; set; }
    }
}
