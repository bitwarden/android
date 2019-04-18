using Bit.Core.Enums;

namespace Bit.Core.Models.Domain
{
    public class TwoFactorProvider
    {
        public TwoFactorProviderType Type { get; set; }
        public string Name { get; set; }
        public string Description { get; set; }
        public int Priority { get; set; }
        public int Sort { get; set; }
        public bool Premium { get; set; }
    }
}
