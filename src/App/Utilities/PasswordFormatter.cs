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
         * This enum is used for the statemachine when building the colorized
         * password string.
         */
        private enum CharType
        {
            None,
            Normal,
            Number,
            Special
        }

        public static FormattedString FormatPassword(String password)
        {
            var result = new FormattedString();

            // Start off with an empty span to prevent possible NPEs. Due to the way the statemachine
            // works, this will actually always be replaced by a new span anyway.
            var currentSpan = new Span();
            // Start with an otherwise uncovered case so we will definitely enter the "something changed"
            // state.
            var currentType = CharType.None;

            foreach (var c in password)
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
                    currentSpan = new Span();
                    result.Spans.Add(currentSpan);
                    currentType = charType;

                    // Switch the color if it is not a normal text. Otherwise leave the
                    // default value.
                    switch (currentType)
                    {
                        case CharType.Number:
                        {
                            currentSpan.TextColor = Color.DodgerBlue;
                            break;
                        }
                        case CharType.Special:
                        {
                            currentSpan.TextColor = Color.Firebrick;
                            break;
                        }
                    }
                }

                currentSpan.Text += c;
            }

            return result;
        }
    }
}
