namespace Bit.App.Models.Api
{
    public class TokenTwoFactorRequest
    {
        public string Code { get; set; }
        public string Provider { get; set; }
        public DeviceRequest Device { get; set; }
    }
}
