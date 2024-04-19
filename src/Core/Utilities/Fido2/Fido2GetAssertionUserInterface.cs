using Bit.Core.Abstractions;

namespace Bit.Core.Utilities.Fido2
{
    /// <summary>
    /// This implementation is used when all interactions are handled by the operating system.
    /// Most often the user has already picked a credential by the time the Authenticator is called,
    /// so this class just returns those values.
    /// 
    /// This class has no corresponding attestation variant, because that operation requires that the 
    /// user interacts with the app directly.
    /// </summary>
    public class Fido2GetAssertionUserInterface : IFido2GetAssertionUserInterface
    {
        protected string _cipherId;
        protected bool _userVerified = false;
        protected Func<Task> _ensureUnlockedVaultCallback;
        protected Func<bool> _hasVaultBeenUnlockedInThisTransaction;
        protected Func<string, Fido2UserVerificationPreference, Task<bool>> _verifyUserCallback;

        public Fido2GetAssertionUserInterface()
        {
        }

        /// <param name="cipherId">The cipherId for the credential that the user has already picker</param>
        /// <param name="userVerified">True if the user has already been verified by the operating system</param>
        public Fido2GetAssertionUserInterface(string cipherId,
            bool userVerified,
            Func<Task> ensureUnlockedVaultCallback,
            Func<bool> hasVaultBeenUnlockedInThisTransaction,
            Func<string, Fido2UserVerificationPreference, Task<bool>> verifyUserCallback)
        {
            Init(cipherId, userVerified, ensureUnlockedVaultCallback, hasVaultBeenUnlockedInThisTransaction, verifyUserCallback);
        }

        protected void Init(string cipherId,
            bool userVerified,
            Func<Task> ensureUnlockedVaultCallback,
            Func<bool> hasVaultBeenUnlockedInThisTransaction,
            Func<string, Fido2UserVerificationPreference, Task<bool>> verifyUserCallback)
        {
            _cipherId = cipherId;
            _userVerified = userVerified;
            _ensureUnlockedVaultCallback = ensureUnlockedVaultCallback;
            _hasVaultBeenUnlockedInThisTransaction = hasVaultBeenUnlockedInThisTransaction;
            _verifyUserCallback = verifyUserCallback;
        }

        public bool HasVaultBeenUnlockedInThisTransaction { get; private set; }

        public async Task<(string CipherId, bool UserVerified)> PickCredentialAsync(Fido2GetAssertionUserInterfaceCredential[] credentials)
        {
            if (credentials.Length == 0 || !credentials.Any(c => c.CipherId == _cipherId))
            {
                throw new NotAllowedError();
            }

            var credential = credentials.First(c => c.CipherId == _cipherId);
            var verified = _userVerified || await _verifyUserCallback(_cipherId, credential.UserVerificationPreference);

            return (CipherId: _cipherId, UserVerified: verified);
        }

        public async Task EnsureUnlockedVaultAsync() 
        {
            await _ensureUnlockedVaultCallback();

            HasVaultBeenUnlockedInThisTransaction = _hasVaultBeenUnlockedInThisTransaction();
        }
    }
}
