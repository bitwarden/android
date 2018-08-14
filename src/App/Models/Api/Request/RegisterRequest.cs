using Bit.App.Enums;

namespace Bit.App.Models.Api
{
    public class RegisterRequest
    {
        public string Name { get; set; }
        public string Email { get; set; }
        public string MasterPasswordHash { get; set; }
        public string MasterPasswordHint { get; set; }
        public string Key { get; set; }
        public KdfType Kdf { get; set; }
        public int KdfIterations { get; set; }
    }
}
