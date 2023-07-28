namespace Bit.Core.Models.Request
{
    public class DeleteAccountRequest
    {
        public string MasterPasswordHash { get; set; }

        public string OTP { get; set; }
    }
}
