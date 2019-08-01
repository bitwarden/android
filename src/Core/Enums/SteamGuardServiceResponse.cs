namespace Bit.Core.Enums
{
    public enum SteamGuardServiceResponse
    {
        Error,
        Okay,

        WrongCredentials,
        WrongSMSCode,
        WrongCaptcha,
        WrongEmailCode,

        NeedCaptcha,
        NeedEmailCode,
        NeedSMSCode,
    }
}
