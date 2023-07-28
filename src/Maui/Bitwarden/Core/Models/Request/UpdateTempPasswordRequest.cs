namespace Bit.Core.Models.Request
{
    public class UpdateTempPasswordRequest
    {
        public string NewMasterPasswordHash { get; set; }
        public string MasterPasswordHint { get; set; }
        public string Key { get; set; }
    }
}
