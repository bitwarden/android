namespace Bit.Core.Enums
{
    public enum SteamGuardServiceError
    {
        None,
        General,
        EmptyResponse,
        CorruptResponse,
        GuardSyncFailed,
        SuccessMissing,
        AllreadyConnectedSteamguard,
        LoginFailedTooOften,
        RSAFailed,
    }
}
