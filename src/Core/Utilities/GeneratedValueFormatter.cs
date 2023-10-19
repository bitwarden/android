using System.Web;

namespace Bit.App.Utilities
{
    /**
     * Helper class to format a password/username with numeric encoding to separate
     * normal text from numbers and special characters.
     */
    class GeneratedValueFormatter
    {
        /**
         * This enum is used for the state machine when building the colorized
         * password/username string.
         */
        private enum CharType
        {
            None,
            Normal,
            Number,
            Special
        }

        public static string Format(string generatedValue)
        {
            if (generatedValue == null)
            {
                return string.Empty;
            }

            // First two digits of returned hex code contains the alpha,
            // which is not supported in HTML color, so we need to cut those out.
            var normalColor = $"<span style=\"color:#{ThemeManager.GetResourceColor("TextColor").ToHex().Substring(3)}\">";
            var numberColor = $"<span style=\"color:#{ThemeManager.GetResourceColor("PasswordNumberColor").ToHex().Substring(3)}\">";
            var specialColor = $"<span style=\"color:#{ThemeManager.GetResourceColor("PasswordSpecialColor").ToHex().Substring(3)}\">";
            var result = string.Empty;

            // iOS won't hide the zero-width space char without these div attrs, but Android won't respect
            // display:inline-block and adds a newline after the password/username.  Hence, only iOS gets the div.
            if (DeviceInfo.Platform == DevicePlatform.iOS)
            {
                result += "<div style=\"display:inline-block; align-items:center; justify-content:center; text-align:center; word-break:break-all; white-space:pre-wrap; min-width:0\">";
            }

            // Start with an otherwise uncovered case so we will definitely enter the "something changed"
            // state.
            var currentType = CharType.None;

            foreach (var c in generatedValue)
            {
                // First, identify what the current char is.
                CharType charType;
                if (char.IsLetter(c))
                {
                    charType = CharType.Normal;
                }
                else if (char.IsDigit(c))
                {
                    charType = CharType.Number;
                }
                else
                {
                    charType = CharType.Special;
                }

                // If the char type changed, build a new span to append the text to.
                if (charType != currentType)
                {
                    // Close off previous span.
                    if (currentType != CharType.None)
                    {
                        result += "</span>";
                    }

                    currentType = charType;

                    // Switch the color if it is not a normal text. Otherwise leave the
                    // default value.
                    switch (currentType)
                    {
                        // Apply color style to span.
                        case CharType.Normal:
                            result += normalColor;
                            break;
                        case CharType.Number:
                            result += numberColor;
                            break;
                        case CharType.Special:
                            result += specialColor;
                            break;
                    }
                }

                if (currentType == CharType.Special)
                {
                    result += HttpUtility.HtmlEncode(c);
                }
                else
                {
                    result += c;
                }

                // Add zero-width space after every char so per-char wrapping works consistently 
                result += "&#8203;";
            }

            // Close off last span.
            if (currentType != CharType.None)
            {
                result += "</span>";
            }

            // Close off iOS div
            if (DeviceInfo.Platform == DevicePlatform.iOS)
            {
                result += "</div>";
            }

            return result;
        }
    }
}
