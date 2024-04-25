namespace Bit.Core.Abstractions
{
    public interface IFido2UserInterface
    {
        /// <summary>
        /// Whether the vault has been unlocked during this transaction
        /// </summary>
        bool HasVaultBeenUnlockedInThisTransaction { get; }

        /// <summary>
        /// Make sure that the vault is unlocked.
        /// This should open a window and ask the user to login or unlock the vault if necessary.
        /// </summary>
        /// <returns>When vault has been unlocked.</returns>
        Task EnsureUnlockedVaultAsync();
    }
}
