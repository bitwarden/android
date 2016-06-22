namespace Bit.App.Models.Api
{
    public class TokenRequest
    {
        public string Email { get; set; }
        public string MasterPasswordHash { get; set; }
        public DeviceRequest Device { get; set; }
    }
}
