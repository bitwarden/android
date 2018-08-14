using Bit.App.Enums;

namespace Bit.App.Models.Api
{
    public class PreloginResponse
    {
        public KdfType Kdf { get; set; }
        public int KdfIterations { get; set; }
    }
}
