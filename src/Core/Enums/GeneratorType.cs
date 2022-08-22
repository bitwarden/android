namespace Bit.Core.Enums
{
    public enum GeneratorType
    {
        Password = 0,
        Username = 1
    }

    public static class GeneratorTypeExtensions
    {
        public static string GetString(this GeneratorType me)
        {
            switch (me)
            {
                case GeneratorType.Password:
                    return "Password";
                case GeneratorType.Username:
                    return "Username";
                default:
                    return string.Empty;
            }
        }
    }
}
