using System;

namespace Bit.iOS.Autofill.Utilities
{
    /// <summary>
    /// Custom exception to be thrown when we need UI from the extension.
    /// This is likely to be thrown when initiating on "...WithoutUserInteraction(...)" and doing some logic that
    /// requires user interaction.
    /// </summary>
    public class InvalidOperationNeedsUIException : InvalidOperationException
    {
        public InvalidOperationNeedsUIException()
        {
        }

        public InvalidOperationNeedsUIException(string message) : base(message)
        {
        }

        public InvalidOperationNeedsUIException(string message, Exception innerException) : base(message, innerException)
        {
        }
    }
}
