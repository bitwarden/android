using Bit.Core.Enums;

namespace Bit.Core.Models.Request
{
    public class SetPasswordRequest
    {
        public string MasterPasswordHash { get; set; }
        public string Key { get; set; }
        public string MasterPasswordHint { get; set; }
        public KeysRequest? Keys { get; set; }
        public KdfType Kdf { get; set; }
        public int KdfIterations { get; set; }
        public int? KdfMemory { get; set; }
        public int? KdfParallelism { get; set; }
        public string OrgIdentifier { get; set; }
    }
}
