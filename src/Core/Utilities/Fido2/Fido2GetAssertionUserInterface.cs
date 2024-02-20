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
        private readonly string _cipherId;
        private readonly bool _userVerified = false;
        private readonly Func<Task> _ensureUnlockedVaultCallback;

        /// <param name="cipherId">The cipherId for the credential that the user has already picker</param>
        /// <param name="userVerified">True if the user has already been verified by the operating system</param>
        public Fido2GetAssertionUserInterface(string cipherId, bool userVerified, Func<Task> ensureUnlockedVaultCallback)
        {
            _cipherId = cipherId;
            _userVerified = userVerified;
            _ensureUnlockedVaultCallback = ensureUnlockedVaultCallback;
        }

        public Task<(string CipherId, bool UserVerified)> PickCredentialAsync(string[] cipherIds, bool userVerification) 
        {
            if (cipherIds.Length == 0 || !cipherIds.Contains(_cipherId))
            {
                throw new NotAllowedError();
            }

            return Task.FromResult((CipherId: _cipherId, UserVerified: _userVerified));
        }

        public Task EnsureUnlockedVaultAsync() 
        {
            return _ensureUnlockedVaultCallback();
        }
    }
}
