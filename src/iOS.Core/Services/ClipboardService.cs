using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Foundation;
using MobileCoreServices;
using UIKit;
using Xamarin.Forms;

namespace Bit.iOS.Core.Services
{
    public class ClipboardService : IClipboardService
    {
        private readonly IStorageService _storageService;

        public ClipboardService(IStorageService storageService)
        {
            _storageService = storageService;
        }

        public async Task CopyTextAsync(string text, int expiresInMs = -1)
        {
            int clearSeconds = -1;
            if (expiresInMs < 0)
            {
                clearSeconds = await _storageService.GetAsync<int?>(Bit.Core.Constants.ClearClipboardKey) ?? -1;
            }
            else
            {
                clearSeconds = expiresInMs * 1000;
            }

            var dictArr = new NSDictionary<NSString, NSObject>[1];
            dictArr[0] = new NSDictionary<NSString, NSObject>(new NSString(UTType.UTF8PlainText), new NSString(text));
            Device.BeginInvokeOnMainThread(() => UIPasteboard.General.SetItems(dictArr, new UIPasteboardOptions
            {
                LocalOnly = true,
                ExpirationDate = clearSeconds > 0 ? NSDate.FromTimeIntervalSinceNow(clearSeconds) : null
            }));
        }
    }
}
