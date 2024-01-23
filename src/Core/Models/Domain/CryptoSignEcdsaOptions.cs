namespace Bit.Core.Models.Domain
{
    public struct CryptoSignEcdsaOptions : ICryptoSignOptions
    {
        public enum EcdsaAlgorithm : byte {
            EcdsaP256Sha256 = 0,
        }

        public enum DsaSignatureFormat : byte {
            IeeeP1363FixedFieldConcatenation = 0,
            Rfc3279DerSequence = 1
        }

        public EcdsaAlgorithm Algorithm { get; set; }
        public DsaSignatureFormat SignatureFormat { get; set; }
    }
}
