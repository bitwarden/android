using Bit.Core.Utilities.Fido2.Extensions;

namespace Bit.Core.Utilities.Fido2
{
#nullable enable

    /// <summary>
    /// Parameters for creating a new credential.
    /// </summary>
    public class Fido2ClientCreateCredentialParams
    {
        /// <summary>
        /// The Relaying Parties origin, see: https://html.spec.whatwg.org/multipage/browsers.html#concept-origin
        /// </summary>
        public required string Origin { get; set; }

        /// <summary>
        /// A value which is true if and only if the caller’s environment settings object is same-origin with its ancestors.
        /// It is false if caller is cross-origin.
        /// </summary>
        public bool SameOriginWithAncestors { get; set; }

        /// <summary>
        /// The Relying Party's preference for attestation conveyance
        /// </summary>
        public string? Attestation { get; set; } = "none";

        /// <summary>
        /// The Relying Party's requirements of the authenticator used in the creation of the credential.
        /// </summary>
        public AuthenticatorSelectionCriteria? AuthenticatorSelection { get; set; }

        /// <summary>
        /// Challenge intended to be used for generating the newly created credential's attestation object.
        /// </summary>
        public required byte[] Challenge { get; set; } // base64url encoded

        /// <summary>
        /// This member is intended for use by Relying Parties that wish to limit the creation of multiple credentials for
        /// the same account on a single authenticator. The client is requested to return an error if the new credential would
        /// be created on an authenticator that also contains one of the credentials enumerated in this parameter.
        /// </summary>
        public PublicKeyCredentialDescriptor[]? ExcludeCredentials { get; set; }

        /// <summary>
        /// This member contains additional parameters requesting additional processing by the client and authenticator.
        /// </summary>
        public Fido2CreateCredentialExtensionsParams? Extensions { get; set; }

        /// <summary>
        /// This member contains information about the desired properties of the credential to be created.
        /// The sequence is ordered from most preferred to least preferred.
        /// The client makes a best-effort to create the most preferred credential that it can.
        /// </summary>
        public required PublicKeyCredentialParameters[] PubKeyCredParams { get; set; }

        /// <summary>
        /// Data about the Relying Party responsible for the request.
        /// </summary>
        public required PublicKeyCredentialRpEntity Rp { get; set; }

        /// <summary>
        /// Data about the user account for which the Relying Party is requesting attestation.
        /// </summary>
        public required PublicKeyCredentialUserEntity User { get; set; }

        /// <summary>
        /// This member specifies a time, in milliseconds, that the caller is willing to wait for the call to complete.
        /// This is treated as a hint, and MAY be overridden by the client.
        /// </summary>
        /// <remarks>
        /// This is not currently supported.
        /// </remarks>
        public int? Timeout { get; set; }
    }
}
