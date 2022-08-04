namespace Bit.Core.Enums
{
    public enum UsernameEmailType : byte
    {
        Random = 0,
        Website = 1,
    }

    public static class UsernameEmailTypeExtensions
    {
        public static string GetString(this UsernameEmailType me)
        {
            switch (me)
            {
                case UsernameEmailType.Random:
                    return "Random";
                case UsernameEmailType.Website:
                    return "Website";
                default:
                    return string.Empty;
            }
        }
    }
}
