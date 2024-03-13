/// <summary>
/// Represents the metadata of a discoverable credential for a FIDO2 authenticator.
/// See: https://www.w3.org/TR/webauthn-3/#sctn-op-silent-discovery
/// </summary>
public class Fido2AuthenticatorDiscoverableCredentialMetadata
{
    public string Type { get; set; }

    public byte[] Id { get; set; }

    public string RpId { get; set; }

    public byte[] UserHandle { get; set; }

    public string UserName { get; set; }

    public string CipherId { get; set; }
}
