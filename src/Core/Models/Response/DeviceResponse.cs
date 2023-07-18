using Bit.Core.Enums;

public class DeviceResponse
{
    public string Id { get; set; }
    public string Name { get; set; }
    public string Identifier { get; set; }
    public DeviceType Type { get; set; }
    public string CreationDate { get; set; }
    public string EncryptedUserKey { get; set; }
    public string EncryptedPublicKey { get; set; }
    public string EncryptedPrivateKey { get; set; }
}
