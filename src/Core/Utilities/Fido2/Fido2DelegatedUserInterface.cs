using Bit.Core.Abstractions;

namespace Bit.Core.Utilities.Fido2
{
    /// <summary>
    /// This implementation is used when all interactions are delegated to the operating system.
    /// Most often these decisions have already been made by the time the Authenticator is called.
    /// 
    /// This is only supported for assertion operations. Attestation requires the user to interact 
    /// with the app directly.
    /// </summary>
    public class Fido2DelegatedUserInterface : IFido2UserInterface
    {
        private string _cipherId = null;
        private bool _userVerified = false;
        private Func<Task> _ensureUnlockedVaultAsyncCallback;

        /// <summary>
        /// Indicates that the user has already picked a credential from a list of existing credentials.
        /// Picking a credential also assumes user presence.
        /// </summary>
        public Fido2DelegatedUserInterface UserPickedCredential(string cipherId)
        {
            _cipherId = cipherId;
            return this;
        }

        /// <summary>
        /// Indicates that the user was verified by the OS, e.g. by a fingerprint or face scan.
        /// </summary>
        public Fido2DelegatedUserInterface UserIsVerified()
        {
            _userVerified = true;
            return this;
        }

        public Fido2DelegatedUserInterface WithEnsureUnlockedVaultAsyncCallback(Func<Task> callback) 
        {
            _ensureUnlockedVaultAsyncCallback = callback;
            return this;
        }

        public Task<Fido2PickCredentialResult> PickCredentialAsync(Fido2PickCredentialParams parameters)
        {
            return Task.FromResult(new Fido2PickCredentialResult
            {
                CipherId = _cipherId,
                UserVerified = _userVerified
            });
        }

        public Task EnsureUnlockedVaultAsync()
        {
            if (_ensureUnlockedVaultAsyncCallback != null)
            {
                return _ensureUnlockedVaultAsyncCallback();
            }

            throw new Exception("No callback provided to ensure the vault is unlocked");
        }
        
        public Task InformExcludedCredential(string[] existingCipherIds) => throw new NotImplementedException();
        public Task<Fido2ConfirmNewCredentialResult> ConfirmNewCredentialAsync(Fido2ConfirmNewCredentialParams parameters) => throw new NotImplementedException();
    }
}
