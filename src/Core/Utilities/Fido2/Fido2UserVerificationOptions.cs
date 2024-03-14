namespace Bit.Core.Utilities.Fido2
{
    public readonly struct Fido2UserVerificationOptions
    {
        public Fido2UserVerificationOptions(bool shouldCheckMasterPasswordReprompt,
            Fido2UserVerificationPreference userVerificationPreference,
            bool hasVaultBeenUnlockedInTransaction,
            string rpId = null,
            Func<Task> onNeedUITask = null)
        {
            ShouldCheckMasterPasswordReprompt = shouldCheckMasterPasswordReprompt;
            UserVerificationPreference = userVerificationPreference;
            HasVaultBeenUnlockedInTransaction = hasVaultBeenUnlockedInTransaction;
            RpId = rpId;
            OnNeedUITask = onNeedUITask;
        }

        public bool ShouldCheckMasterPasswordReprompt { get; }
        public Fido2UserVerificationPreference UserVerificationPreference { get; }
        public bool HasVaultBeenUnlockedInTransaction { get; }
        public string RpId { get; }
        public Func<Task> OnNeedUITask { get; }
    }
}
