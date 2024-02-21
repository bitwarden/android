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

        public Fido2MakeCredentialUserInterface(Func<Task> ensureUnlockedVaultCallback, Context context, Action onConfirmingNewCredential)
        {
            _ensureUnlockedVaultCallback = ensureUnlockedVaultCallback;
            _context = context;
            _onConfirmingNewCredential = onConfirmingNewCredential;
        }

        public async Task<(string CipherId, bool UserVerified)> ConfirmNewCredentialAsync(Fido2ConfirmNewCredentialParams confirmNewCredentialParams)
        {
            _context.ConfirmNewCredentialTcs?.SetCanceled();
            _context.ConfirmNewCredentialTcs = new TaskCompletionSource<(string CipherId, bool UserVerified)>();

            _onConfirmingNewCredential();

            return await _context.ConfirmNewCredentialTcs.Task;
        }

        // iOS doesn't seem to provide the ExcludeCredentialDescriptorList so nothing to do here currently.
        public Task InformExcludedCredentialAsync(string[] existingCipherIds) => Task.CompletedTask;

        public Task EnsureUnlockedVaultAsync() => _ensureUnlockedVaultCallback();
    }
}
