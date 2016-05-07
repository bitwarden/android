using Android.Content;
using Bit.App.Abstractions;
using Xamarin.Forms;

namespace Bit.Android.Services
{
    public class ClipboardService : IClipboardService
    {
        public void CopyToClipboard(string text)
        {
            var clipboardManager = (ClipboardManager)Forms.Context.GetSystemService(Context.ClipboardService);
            clipboardManager.Text = text;
        }
    }
}
