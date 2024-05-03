namespace Bit.Core.Utilities.Fido2
{
    public class PublicKeyCredentialAlgorithmDescriptor {
        public byte[] Id {get; set;}
        public string[] Transports;
        public string Type;
        public int Algorithm;
    }
}
