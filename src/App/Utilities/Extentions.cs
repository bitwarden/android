using System;
using Bit.App.Abstractions;
using Bit.App.Models;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App
{
    public static class Extentions
    {
        public static CipherString Encrypt(this string s)
        {
            if(s == null)
            {
                throw new ArgumentNullException(nameof(s));
            }

            var cryptoService = Resolver.Resolve<ICryptoService>();
            return cryptoService.Encrypt(s);
        }

        public static bool IsPortrait(this Page page)
        {
            return page.Width < page.Height;
        }

        public static bool IsLandscape(this Page page)
        {
            return !page.IsPortrait();
        }

        public static void FocusWithDelay(this Entry entry, int delay = 500)
        {
            if(Device.OS == TargetPlatform.Android)
            {
                System.Threading.Tasks.Task.Run(async () =>
                {
                    await System.Threading.Tasks.Task.Delay(delay);
                    Device.BeginInvokeOnMainThread(() => entry.Focus());
                });
            }
            else
            {
                entry.Focus();
            }
        }
    }
}
