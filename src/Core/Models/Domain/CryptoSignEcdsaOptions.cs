namespace Bit.Core.Models.Domain
{
    public struct CryptoSignEcdsaOptions : ICryptoSignOptions
    {
        public enum DsaSignatureFormat : byte {
            IeeeP1363FixedFieldConcatenation = 0,
            Rfc3279DerSequence = 1
        }

        public CryptoEcdsaAlgorithm Algorithm { get; set; }
        public DsaSignatureFormat SignatureFormat { get; set; }
    }
}
