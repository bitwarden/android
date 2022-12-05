namespace Bit.Core.Enums
{
    public enum WatchState : byte
    {
        Valid = 0,
        NeedLogin,
        NeedPremium,
        NeedSetup,
        Need2FAItem,
        Syncing
        //NeedUnlock
    }
}
