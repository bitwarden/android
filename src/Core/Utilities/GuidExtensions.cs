namespace Bit.Core.Utilities
{
    public static class GuidExtensions
    {
        public static string GuidToStandardFormat(this byte[] bytes)
        {
            return new Guid(bytes).ToString();
        }

        public static byte[] GuidToRawFormat(this string guid)
        {
            return Guid.Parse(guid).ToByteArray();
        }
    }
}
