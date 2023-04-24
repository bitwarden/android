namespace Bit.Core.Models.Request
{
    public class PasswordRequest
    {
        public string MasterPasswordHash { get; set; }
        public string NewMasterPasswordHash { get; set; }
        public string MasterPasswordHint { get; set; }
        public string Key { get; set; }
    }
}
