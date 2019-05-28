using Bit.Core.Enums;

namespace Bit.Core.Models.Response
{
    public class PreloginResponse
    {
        public KdfType Kdf { get; set; }
        public int KdfIterations { get; set; }
    }
}
