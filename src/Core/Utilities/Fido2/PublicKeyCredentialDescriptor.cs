namespace Bit.Core.Utilities.Fido2
{
    public class PublicKeyCredentialDescriptor {
        public byte[] Id { get; set; }
        public string[] Transports { get; set; }
        public string Type { get; set; }
    }
}

