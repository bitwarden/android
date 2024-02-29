namespace Bit.Core.Utilities.Fido2
{
    public readonly struct Fido2UserVerificationOptions
    {
        public Fido2UserVerificationOptions(bool shouldCheckMasterPasswordReprompt,
            bool isUserVerificationRequired,
            bool hasVaultBeenUnlockedInTransaction,
            string rpId,
            Action onNeedUI = null)
        {
            ShouldCheckMasterPasswordReprompt = shouldCheckMasterPasswordReprompt;
            IsUserVerificationRequired = isUserVerificationRequired;
            HasVaultBeenUnlockedInTransaction = hasVaultBeenUnlockedInTransaction;
            RpId = rpId;
            OnNeedUI = onNeedUI;
        }

        public bool ShouldCheckMasterPasswordReprompt { get; }
        public bool IsUserVerificationRequired { get; }
        public bool HasVaultBeenUnlockedInTransaction { get; }
        public string RpId { get; }
        public Action OnNeedUI { get; }
    }
}
