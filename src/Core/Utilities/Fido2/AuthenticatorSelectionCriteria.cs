namespace Bit.Core.Utilities.Fido2
{
    #nullable enable
    /// <summary>
    /// The Relying Party's requirements of the authenticator used in the creation of the credential.
    /// </summary>
    public class AuthenticatorSelectionCriteria
    {
        public bool? RequireResidentKey { get; set; }
        public string? ResidentKey { get; set; }
        public string UserVerification { get; set; } = "preferred";

        /// <summary>
        /// This member is intended for use by Relying Parties that wish to select the appropriate authenticators to participate in the create() operation.
        /// </summary>
        // public AuthenticatorAttachment? AuthenticatorAttachment { get; set; } // not used
    }
}
