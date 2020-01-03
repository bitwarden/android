using System;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    /**
     * Helper class to format a password with numeric encoding to separate
     * normal text from numbers and special characters.
     */
    class PasswordFormatter
    {
        /**
         * This enum is used for the state machine when building the colorized
         * password string.
         */
        private enum CharType
        {
            None,
            Normal,
            Number,
            Special
        }

        public static string FormatPassword(string password)
        {
            if(password == null)
            {
                return string.Empty;
            }

            // First two digits of returned hex code contains the alpha,
            // which is not supported in HTML color, so we need to cut those out.
            var numberColor = $"<span style=\"color:#{((Color)Application.Current.Resources["PasswordNumberColor"]).ToHex().Substring(2)}\">";
            var specialColor = $"<span style=\"color:#{((Color)Application.Current.Resources["PasswordSpecialColor"]).ToHex().Substring(2)}\">";
            var result = string.Empty;

            // Start with an otherwise uncovered case so we will definitely enter the "something changed"
            // state.
            var currentType = CharType.None;

            foreach(var c in password)
            {
                // First, identify what the current char is.
                CharType charType;
                if(char.IsLetter(c))
                {
                    charType = CharType.Normal;
                }
                else if(char.IsDigit(c))
                {
                    charType = CharType.Number;
                }
                else
                {
                    charType = CharType.Special;
                }

                // If the char type changed, build a new span to append the text to.
                if(charType != currentType)
                {
                    // Close off previous span.
                    if (currentType != CharType.None && currentType != CharType.Normal)
                    {
                        result += "</span>";
                    }
                    
                    currentType = charType;

                    // Switch the color if it is not a normal text. Otherwise leave the
                    // default value.
                    switch(currentType)
                    {
                        // Apply color style to span.
                        case CharType.Number:
                            result += numberColor;
                            break;
                        case CharType.Special:
                            result += specialColor;
                            break;
                    }
                }
                result += c;
            }

            // Close off last span.
            if (currentType != CharType.None && currentType != CharType.Normal)
            {
                result += "</span>";
            }

            return result;
        }
    }
}
