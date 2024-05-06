using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Abstractions
{
    public interface IFido2MakeCredentialConfirmationUserInterface : IFido2MakeCredentialUserInterface
    {
        /// <summary>
        /// Call this method after the user chose where to save the new Fido2 credential.
        /// </summary>
        /// <param name="cipherId">
        /// Cipher ID where to save the new credential.
        /// If <c>null</c> a new default passkey cipher item will be created
        /// </param>
        /// <param name="userVerified">
        /// Whether the user has been verified or not.
        /// If <c>null</c> verification has not taken place yet.
        /// </param>
        void Confirm(string cipherId, bool? userVerified);

        /// <summary>
        /// Call this method after the user chose where to save the new Fido2 credential.
        /// </summary>
        /// <param name="cipherId">
        /// Cipher ID where to save the new credential.
        /// If <c>null</c> a new default passkey cipher item will be created
        /// </param>
        /// <param name="alreadyHasFido2Credential">
        /// If the cipher corresponding to the <paramref name="cipherId"/> already has a Fido2 credential.
        /// </param>
        /// <param name="userVerified">
        /// Whether the user has been verified or not.
        /// If <c>null</c> verification has not taken place yet.
        /// </param>
        Task ConfirmAsync(string cipherId, bool alreadyHasFido2Credential, bool? userVerified);

        /// <summary>
        /// Cancels the current flow to make a credential
        /// </summary>
        void Cancel();

        /// <summary>
        /// Call this if an exception needs to happen on the credential making process
        /// </summary>
        void OnConfirmationException(Exception ex);

        
        /// <summary>
        /// True if we are already confirming a new credential.
        /// </summary>
        bool IsConfirmingNewCredential { get; }

        /// <summary>
        /// Call this after the vault was unlocked so that Fido2 credential creation can proceed.
        /// </summary>
        void ConfirmVaultUnlocked();

        /// <summary>
        /// True if we are waiting for the vault to be unlocked.
        /// </summary>
        bool IsWaitingUnlockVault { get; }

        Fido2UserVerificationOptions? GetCurrentUserVerificationOptions();

        void SetCheckHasVaultBeenUnlockedInThisTransaction(Func<bool> checkHasVaultBeenUnlockedInThisTransaction);
    }
}
