using Bit.Core.Enums;

namespace Bit.Core.Models.Response
{
    public class PreloginResponse
    {
        public KdfType Kdf { get; set; }
        public int KdfIterations { get; set; }
        public int? KdfMemory { get; set; }
        public int? KdfParallelism { get; set; }
    }
}
