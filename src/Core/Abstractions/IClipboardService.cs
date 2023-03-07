using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IClipboardService
    {
        /// <summary>
        /// Copies the <paramref name="text"/> to the Clipboard.
        /// If <paramref name="expiresInMs"/> is set > 0 then the Clipboard will be cleared after this time in milliseconds.
        /// if less than 0 then it takes the configuration that the user set in Options.
        /// If <paramref name="isSensitive"/> is true the sensitive flag is passed to the clipdata to obfuscate the
        /// clipboard text in the popup (Android 13+ only)
        /// </summary>
        /// <param name="text">Text to be copied to the Clipboard</param>
        /// <param name="expiresInMs">Expiration time in milliseconds of the copied text</param>
        /// <param name="isSensitive">Flag to mark copied text as sensitive</param>
        Task CopyTextAsync(string text, int expiresInMs = -1, bool isSensitive = true);
    }
}
