using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Abstractions 
{
    public struct Fido2ConfirmNewCredentialParams
    {
        ///<summary>
        /// The name of the credential.
        ///</summary>
        public string CredentialName { get; set; }

        ///<summary>
        /// The name of the user.
        ///</summary>
        public string UserName { get; set; }

        /// <summary>
        /// The preference to whether or not the user must be verified before completing the operation.
        /// </summary>
        public Fido2UserVerificationPreference UserVerificationPreference { get; set; }

        /// <summary>
        /// The relying party identifier
        /// </summary>
        public string RpId { get; set; }
    }

    public interface IFido2MakeCredentialUserInterface : IFido2UserInterface
    {
        /// <summary>
        /// Inform the user that the operation was cancelled because their vault contains excluded credentials.
        /// </summary>
        /// <param name="existingCipherIds">The IDs of the excluded credentials.</param>
        /// <returns>When user has confirmed the message</returns>
        Task InformExcludedCredentialAsync(string[] existingCipherIds);

        /// <summary>
        /// Ask the user to confirm the creation of a new credential.
        /// </summary>
        /// <param name="confirmNewCredentialParams">The parameters to use when asking the user to confirm the creation of a new credential.</param>
        /// <returns>The ID of the cipher where the new credential should be saved, and if the user was verified before completing the operation</returns>
        Task<(string CipherId, bool UserVerified)> ConfirmNewCredentialAsync(Fido2ConfirmNewCredentialParams confirmNewCredentialParams);
    }
}
