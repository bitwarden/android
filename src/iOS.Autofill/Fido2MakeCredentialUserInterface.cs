using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Utilities.Fido2;
using Bit.iOS.Autofill.Models;

namespace Bit.iOS.Autofill
{
    public class Fido2MakeCredentialUserInterface : IFido2MakeCredentialUserInterface
    {
        private readonly Func<Task> _ensureUnlockedVaultCallback;
        private readonly Func<bool> _hasVaultBeenUnlockedInThisTransaction;
        private readonly Context _context;
        private readonly Action _onConfirmingNewCredential;
        private readonly Func<string, Fido2UserVerificationPreference, Task<bool>> _verifyUserCallback;

        public Fido2MakeCredentialUserInterface(Func<Task> ensureUnlockedVaultCallback,
            Func<bool> hasVaultBeenUnlockedInThisTransaction,
            Context context,
            Action onConfirmingNewCredential,
            Func<string, Fido2UserVerificationPreference, Task<bool>> verifyUserCallback)
        {
            _ensureUnlockedVaultCallback = ensureUnlockedVaultCallback;
            _hasVaultBeenUnlockedInThisTransaction = hasVaultBeenUnlockedInThisTransaction;
            _context = context;
            _onConfirmingNewCredential = onConfirmingNewCredential;
            _verifyUserCallback = verifyUserCallback;
        }

        public bool HasVaultBeenUnlockedInThisTransaction { get; private set; }

        public async Task<(string CipherId, bool UserVerified)> ConfirmNewCredentialAsync(Fido2ConfirmNewCredentialParams confirmNewCredentialParams)
        {
            _context.PickCredentialForFido2CreationTcs?.TrySetCanceled();
            _context.PickCredentialForFido2CreationTcs = new TaskCompletionSource<(string, bool?)>();
            _context.PasskeyCreationParams = confirmNewCredentialParams;

            _onConfirmingNewCredential();

            var (cipherId, isUserVerified) = await _context.PickCredentialForFido2CreationTcs.Task;

            var verified = isUserVerified ?? await _verifyUserCallback(cipherId, confirmNewCredentialParams.UserVerificationPreference);

            return (cipherId, verified);
        }

        // iOS doesn't seem to provide the ExcludeCredentialDescriptorList so nothing to do here currently.
        public Task InformExcludedCredentialAsync(string[] existingCipherIds) => Task.CompletedTask;

        public async Task EnsureUnlockedVaultAsync()
        {
            await _ensureUnlockedVaultCallback();

            HasVaultBeenUnlockedInThisTransaction = _hasVaultBeenUnlockedInThisTransaction();
        }
    }
}
