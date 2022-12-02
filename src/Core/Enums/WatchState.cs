namespace Bit.Core.Enums
{
    public enum WatchState : byte
    {
        Valid = 0,
        NeedLogin,
        //NeedUnlock,
        NeedPremium,
        NeedSetup,
        Need2FAItem,
        Syncing
    }
}
