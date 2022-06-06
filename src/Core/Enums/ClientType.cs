namespace Bit.Core.Enums
{
    public enum ClientType : byte
    {
        Web = 1,
        Browser = 2,
        Desktop = 3,
        Mobile = 4,
        Cli = 5,
        DirectoryConnector = 6,
    }

    public static class ClientTypeExtensions
    {
        public static string GetString(this ClientType me)
        {
            switch (me)
            {
                case ClientType.Web:
                    return "web";
                case ClientType.Browser:
                    return "browser";
                case ClientType.Desktop:
                    return "desktop";
                case ClientType.Mobile:
                    return "mobile";
                case ClientType.Cli:
                    return "cli";
                case ClientType.DirectoryConnector:
                    return "connector";
                default:
                    return "";
            }
        }
    }
}
