namespace Bit.Core.Abstractions 
{
    public interface IFido2GetAssertionUserInterface : IFido2UserInterface
    {
        /// <summary>
        /// Ask the user to pick a credential from a list of existing credentials.
        /// </summary>
        /// <param name="pickCredentialParams">The IDs of the credentials that the user can pick from, and if the user must be verified before completing the operation</param>
        /// <returns>The ID of the cipher that contains the credentials the user picked, and if the user was verified before completing the operation</returns>
        Task<(string CipherId, bool UserVerified)> PickCredentialAsync(string[] cipherIds, bool userVerification);
    }
}
