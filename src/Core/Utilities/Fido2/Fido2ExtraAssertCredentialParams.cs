namespace Bit.Core.Utilities.Fido2
{
    #nullable enable

    /// <summary>
    /// Extra parameters for asserting a credential.
    /// </summary>
    public class Fido2ExtraAssertCredentialParams
    {
        public Fido2ExtraAssertCredentialParams(byte[]? clientDataHash = null, string? androidPackageName = null)
        {
            ClientDataHash = clientDataHash;
            AndroidPackageName = androidPackageName;
        }

        /// <summary>
        /// The hash of the serialized client data.
        /// </summary>
        public byte[]? ClientDataHash { get; }

        /// <summary>
        /// The Android package name.
        /// </summary>
        public string? AndroidPackageName { get; }
    }
}
