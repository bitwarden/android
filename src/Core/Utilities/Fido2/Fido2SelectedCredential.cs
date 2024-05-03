using Bit.Core.Models.View;

public class Fido2SelectedCredential
{
    public byte[] Id { get; set; }

    public byte[] UserHandle { get; set; }

    public CipherView Cipher { get; set; }
}
