using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IClipboardService
    {
        /// <summary>
        /// Copies the <paramref name="text"/> to the Clipboard.
        /// If <paramref name="expiresInMs"/> is set > 0 then the Clipboard will be cleared after this time in milliseconds.
        /// if less than 0 then it takes the configuration that the user set in Options.
        /// </summary>
        /// <param name="text">Text to be copied to the Clipboard</param>
        /// <param name="expiresInMs">Expiration time in milliseconds of the copied text</param>
        Task CopyTextAsync(string text, int expiresInMs = -1);
    }
}
