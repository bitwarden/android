namespace Bit.Core.Models.Domain
{
    public enum ForcePasswordResetReason
    {
        /// <summary>
        /// Occurs when an organization admin forces a user to reset their password.
        /// </summary>
        AdminForcePasswordReset,
        
        /// <summary>
        /// Occurs when a user logs in with a master password that does not meet an organization's master password
        /// policy that is enforced on login.
        /// </summary>
        WeakMasterPasswordOnLogin
    }
}
