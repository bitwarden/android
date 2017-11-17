using Bit.App.Enums;

namespace Bit.App.Models
{
    public class AppOptions
    {
        public bool MyVault { get; set; }
        public bool FromAutofillFramework { get; set; }
        public string Uri { get; set; }
        public CipherType? SaveType { get; set; }
        public string SaveName { get; set; }
        public string SaveUsername { get; set; }
        public string SavePassword { get; set; }
    }
}
