namespace Bit.Core.Utilities.Fido2
{
#nullable enable

    /// <summary>
    /// Extra parameters for creating a new credential.
    /// </summary>
    public struct Fido2ExtraCreateCredentialParams
    {
        public Fido2ExtraCreateCredentialParams(byte[]? clientDataHash = null, string? androidPackageName = null)
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
