namespace Bit.Core.Enums
{
    public enum UsernameType : byte
    {
        PlusAddressedEmail = 0,
        CatchAllEmail = 1,
        ForwardedEmailAlias = 2,
        RandomWord = 3,
    }

    public static class UsernameTypeExtensions
    {
        public static string GetString(this UsernameType me)
        {
            switch (me)
            {
                case UsernameType.PlusAddressedEmail:
                    return "Plus Addressed Email";
                case UsernameType.CatchAllEmail:
                    return "Catch-all Email";
                case UsernameType.ForwardedEmailAlias:
                    return "Forwarded Email Alias";
                case UsernameType.RandomWord:
                    return "Random Word";
                default:
                    return string.Empty;
            }
        }
    }
}
