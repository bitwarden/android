using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities.Fido2;
using Bit.iOS.Autofill.Models;

namespace Bit.iOS.Autofill.Utilities
{
    public class Fido2GetAssertionFromListUserInterface : IFido2GetAssertionUserInterface
    {
        private readonly Context _context;
        private readonly Func<Task> _ensureUnlockedVaultCallback;
        private readonly Func<bool> _hasVaultBeenUnlockedInThisTransaction;
        private readonly Func<string, Fido2UserVerificationPreference, Task<bool>> _verifyUserCallback;
        private readonly Action<List<string>> _onAllowedFido2Credentials;

        /// <summary>
        /// Implementation to perform the interactions with the UI directly from a list.
        /// </summary>
        /// <param name="context">Current context</param>
        /// <param name="ensureUnlockedVaultCallback">Call to ensure the vault is unlocekd</param>
        /// <param name="hasVaultBeenUnlockedInThisTransaction">Check if vault has been unlocked in this transaction</param>
        /// <param name="verifyUserCallback">Call to perform user verification to a given cipherId and preference</param>
        /// <param name="onAllowedFido2Credentials">Action to be performed on allowed Fido2 credentials, each one is a cipherId</param>
        public Fido2GetAssertionFromListUserInterface(Context context,
            Func<Task> ensureUnlockedVaultCallback,
            Func<bool> hasVaultBeenUnlockedInThisTransaction,
            Func<string, Fido2UserVerificationPreference, Task<bool>> verifyUserCallback,
            Action<List<string>> onAllowedFido2Credentials)
        {
            _context = context;
            _ensureUnlockedVaultCallback = ensureUnlockedVaultCallback;
            _hasVaultBeenUnlockedInThisTransaction = hasVaultBeenUnlockedInThisTransaction;
            _verifyUserCallback = verifyUserCallback;
            _onAllowedFido2Credentials = onAllowedFido2Credentials;
        }

        public bool HasVaultBeenUnlockedInThisTransaction { get; private set; }

        public async Task<(string CipherId, bool UserVerified)> PickCredentialAsync(Fido2GetAssertionUserInterfaceCredential[] credentials)
        {
            if (credentials is null || credentials.Length == 0)
            {
                throw new NotAllowedError();
            }

            HasVaultBeenUnlockedInThisTransaction = _hasVaultBeenUnlockedInThisTransaction();

            _onAllowedFido2Credentials?.Invoke(credentials.Select(c => c.CipherId).ToList() ?? new List<string>());

            _context.PickCredentialForFido2GetAssertionFromListTcs?.TrySetCanceled();
            _context.PickCredentialForFido2GetAssertionFromListTcs = new TaskCompletionSource<string>();

            var cipherId = await _context.PickCredentialForFido2GetAssertionFromListTcs.Task;

            var credential = credentials.First(c => c.CipherId == cipherId);

            var verified = await _verifyUserCallback(cipherId, credential.UserVerificationPreference);

            return (CipherId: cipherId, UserVerified: verified);
        }

        public async Task EnsureUnlockedVaultAsync()
        {
            await _ensureUnlockedVaultCallback();

            HasVaultBeenUnlockedInThisTransaction = _hasVaultBeenUnlockedInThisTransaction();
        }
    }
}

