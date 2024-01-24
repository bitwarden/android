using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Abstractions
{
    /// <summary>
    /// Parameters used to ask the user to pick a credential from a list of existing credentials.
    /// </summary>
    public struct Fido2PickCredentialParams
    {
        /// <summary>
        /// The IDs of the credentials that the user can pick from.
        /// </summary>
        public string[] CipherIds { get; set; }

        /// <summary>
        /// Whether or not the user must be verified before completing the operation.
        /// </summary>
        public bool UserVerification { get; set; }
    }

    /// <summary>
    /// The result of asking the user to pick a credential from a list of existing credentials.
    /// </summary>
    public struct Fido2PickCredentialResult
    {
        /// <summary>
        /// The ID of the cipher that contains the credentials the user picked.
        /// </summary>
        public string CipherId { get; set; }

        /// <summary>
        /// Whether or not the user was verified before completing the operation.
        /// </summary>
        public bool UserVerified { get; set; }
    }

    public interface IFido2UserInterface
    {
        /// <summary>
        /// Ask the user to pick a credential from a list of existing credentials.
        /// </summary>
        /// <param name="pickCredentialParams">The parameters to use when asking the user to pick a credential.</param>
        /// <returns>The ID of the cipher that contains the credentials the user picked.</returns>
        Task<Fido2PickCredentialResult> PickCredentialAsync(Fido2PickCredentialParams pickCredentialParams);

        /// <summary>
        /// Inform the user that the operation was cancelled because their vault contains excluded credentials.
        /// </summary>
        /// <param name="existingCipherIds">The IDs of the excluded credentials.</param>
        /// <returns>When user has confirmed the message</returns>
        Task InformExcludedCredential(string[] existingCipherIds);
    }
}
