namespace Bit.Core.Enums
{
    public enum ForwardedEmailServiceType
    {
        AnonAddy = 0,
        FirefoxRelay = 1,
        SimpleLogin = 2,
    }

    public static class ForwardedEmailServiceTypeExtensions
    {
        public static string GetString(this ForwardedEmailServiceType me)
        {
            switch (me)
            {
                case ForwardedEmailServiceType.AnonAddy:
                    return "AnonAddy";
                case ForwardedEmailServiceType.FirefoxRelay:
                    return "Firefox Relay";
                case ForwardedEmailServiceType.SimpleLogin:
                    return "SimpleLogin";
                default:
                    return string.Empty;
            }
        }
    }
}
