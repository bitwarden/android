namespace Bit.Core.Utilities.Fido2
{
    public class PublicKeyCredentialDescriptor {
        public byte[] Id {get; set;}
        public string[] Transports;
        public string Type;
    }
}

