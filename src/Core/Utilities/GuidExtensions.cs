using System.Globalization;
using System.Text.RegularExpressions;

namespace Bit.Core.Utilities
{
    /// <summary>
    /// Extension methods for converting between standard and raw GUID formats.
    /// 
    /// Note: Not optimized for performance. Don't use in performance-critical code.
    /// </summary>
    public static class GuidExtensions
    {
        public static byte[] GuidToRawFormat(this string guidString)
        {
            if (guidString == null)
            {
                throw new ArgumentException("GUID parameter is null", nameof(guidString));
            }

            if (!IsValidGuid(guidString)) {
                throw new FormatException("GUID parameter is invalid");
            }

            var arr = new byte[16];

            arr[0] = byte.Parse(guidString.Substring(0, 2), NumberStyles.HexNumber); // Parse ##......-....-....-....-............
            arr[1] = byte.Parse(guidString.Substring(2, 2), NumberStyles.HexNumber); // Parse ..##....-....-....-....-............
            arr[2] = byte.Parse(guidString.Substring(4, 2), NumberStyles.HexNumber); // Parse ....##..-....-....-....-............
            arr[3] = byte.Parse(guidString.Substring(6, 2), NumberStyles.HexNumber); // Parse ......##-....-....-....-............

            arr[4] = byte.Parse(guidString.Substring(9, 2), NumberStyles.HexNumber); // Parse ........-##..-....-....-............
            arr[5] = byte.Parse(guidString.Substring(11, 2), NumberStyles.HexNumber); // Parse ........-..##-....-....-............

            arr[6] = byte.Parse(guidString.Substring(14, 2), NumberStyles.HexNumber); // Parse ........-....-##..-....-............
            arr[7] = byte.Parse(guidString.Substring(16, 2), NumberStyles.HexNumber); // Parse ........-....-..##-....-............

            arr[8] = byte.Parse(guidString.Substring(19, 2), NumberStyles.HexNumber); // Parse ........-....-....-##..-............
            arr[9] = byte.Parse(guidString.Substring(21, 2), NumberStyles.HexNumber); // Parse ........-....-....-..##-............

            arr[10] = byte.Parse(guidString.Substring(24, 2), NumberStyles.HexNumber); // Parse ........-....-....-....-##..........
            arr[11] = byte.Parse(guidString.Substring(26, 2), NumberStyles.HexNumber); // Parse ........-....-....-....-..##........
            arr[12] = byte.Parse(guidString.Substring(28, 2), NumberStyles.HexNumber); // Parse ........-....-....-....-....##......
            arr[13] = byte.Parse(guidString.Substring(30, 2), NumberStyles.HexNumber); // Parse ........-....-....-....-......##....
            arr[14] = byte.Parse(guidString.Substring(32, 2), NumberStyles.HexNumber); // Parse ........-....-....-....-........##..
            arr[15] = byte.Parse(guidString.Substring(34, 2), NumberStyles.HexNumber); // Parse ........-....-....-....-..........##

            return arr;
        }

        public static string GuidToStandardFormat(this byte[] guidBytes)
        {
            if (guidBytes == null)
            {
                throw new ArgumentException("GUID parameter is null", nameof(guidBytes));
            }

            if (guidBytes.Length != 16)
            {
                throw new ArgumentException("Invalid raw GUID format", nameof(guidBytes));
            }

            return Convert.ToHexString(guidBytes).ToLower().Insert(8, "-").Insert(13, "-").Insert(18, "-").Insert(23, "-" );
        }

        public static bool IsValidGuid(string guid)
        {
            return Regex.IsMatch(guid, @"^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$", RegexOptions.ECMAScript);
        }
    }
}
