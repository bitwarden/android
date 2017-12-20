using Bit.App.Enums;

namespace Bit.App.Models
{
    public class AppOptions
    {
        public bool MyVaultTile { get; set; }
        public bool FromAutofillFramework { get; set; }
        public CipherType? FillType { get; set; }
        public string Uri { get; set; }
        public CipherType? SaveType { get; set; }
        public string SaveName { get; set; }
        public string SaveUsername { get; set; }
        public string SavePassword { get; set; }
        public string SaveCardName { get; set; }
        public string SaveCardNumber { get; set; }
        public string SaveCardExpMonth { get; set; }
        public string SaveCardExpYear { get; set; }
        public string SaveCardCode { get; set; }
    }
}
