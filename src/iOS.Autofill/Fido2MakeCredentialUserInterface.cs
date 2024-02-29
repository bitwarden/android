using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.iOS.Autofill.Models;

namespace Bit.iOS.Autofill
{
    public class Fido2MakeCredentialUserInterface : IFido2MakeCredentialUserInterface
    {
        private readonly Func<Task> _ensureUnlockedVaultCallback;
        private readonly Context _context;
        private readonly Action _onConfirmingNewCredential;
        private readonly Func<string, Task<bool>> _verifyUserCallback;

        public Fido2MakeCredentialUserInterface(Func<Task> ensureUnlockedVaultCallback,
            Context context,
            Action onConfirmingNewCredential,
            Func<string, Task<bool>> verifyUserCallback)
        {
            _ensureUnlockedVaultCallback = ensureUnlockedVaultCallback;
            _context = context;
            _onConfirmingNewCredential = onConfirmingNewCredential;
            _verifyUserCallback = verifyUserCallback;
        }

        public async Task<(string CipherId, bool UserVerified)> ConfirmNewCredentialAsync(Fido2ConfirmNewCredentialParams confirmNewCredentialParams)
        {
            _context.PickCredentialForFido2CreationTcs?.SetCanceled();
            _context.PickCredentialForFido2CreationTcs = new TaskCompletionSource<(string, bool?)>();
            _context.PasskeyCreationParams = confirmNewCredentialParams;

            _onConfirmingNewCredential();

            var (cipherId, isUserVerified) = await _context.PickCredentialForFido2CreationTcs.Task;

            var verified = isUserVerified ?? (confirmNewCredentialParams.UserVerification && await _verifyUserCallback(cipherId));

            return (cipherId, verified);
        }

        // iOS doesn't seem to provide the ExcludeCredentialDescriptorList so nothing to do here currently.
        public Task InformExcludedCredentialAsync(string[] existingCipherIds) => Task.CompletedTask;

        public Task EnsureUnlockedVaultAsync() => _ensureUnlockedVaultCallback();
    }
}
