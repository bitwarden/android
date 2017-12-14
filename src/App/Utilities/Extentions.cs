using System;
using Bit.App.Abstractions;
using Bit.App.Models;
using Xamarin.Forms;
using XLabs.Ioc;
using System.Threading.Tasks;
using Bit.App.Controls;

namespace Bit.App
{
    public static class Extentions
    {
        public static CipherString Encrypt(this string s, string orgId = null)
        {
            if(s == null)
            {
                throw new ArgumentNullException(nameof(s));
            }

            var cryptoService = Resolver.Resolve<ICryptoService>();

            if(!string.IsNullOrWhiteSpace(orgId))
            {
                return cryptoService.Encrypt(s, cryptoService.GetOrgKey(orgId));
            }

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

        public static void FocusWithDelay(this View view, int delay = 1000, bool forceDelay = false)
        {
            if(Device.RuntimePlatform == Device.Android || forceDelay)
            {
                Task.Run(async () =>
                {
                    await Task.Delay(delay);
                    Device.BeginInvokeOnMainThread(() => view.Focus());
                });
            }
            else
            {
                view.Focus();
            }
        }

        public static async Task PushForDeviceAsync(this INavigation navigation, Page page)
        {
            if (Device.RuntimePlatform != Device.UWP)
            {
                await navigation.PushModalAsync(new ExtendedNavigationPage(page), true);
            }
            else
            {
                await navigation.PushAsync(page, true);
            }
        }

        public static async Task PopForDeviceAsync(this INavigation navigation)
        {
            if(navigation.ModalStack.Count < 1)
            {
                if (navigation.NavigationStack.Count > 0 && Device.RuntimePlatform == Device.UWP)
                {
                    await navigation.PopAsync();
                }
                return;
            }

            await navigation.PopModalAsync(true);
        }

        public static void AdjustMarginsForDevice(this View view)
        {
            if(Device.RuntimePlatform == Device.Android)
            {
                var deviceInfo = Resolver.Resolve<IDeviceInfoService>();
                if(deviceInfo.Version < 21)
                {
                    view.Margin = new Thickness(-12, -5, -12, -6);
                }
                else if(deviceInfo.Version == 21)
                {
                    view.Margin = new Thickness(-4, -2, -4, -11);
                }
                else
                {
                    view.Margin = new Thickness(-4, -7, -4, -11);
                }
            }
        }

        public static void AdjustPaddingForDevice(this Layout view)
        {
            if(Device.RuntimePlatform == Device.Android)
            {
                var deviceInfo = Resolver.Resolve<IDeviceInfoService>();
                if(deviceInfo.Scale == 1) // mdpi
                {
                    view.Padding = new Thickness(22, view.Padding.Top, 22, view.Padding.Bottom);
                }
                else if(deviceInfo.Scale < 2) // hdpi
                {
                    view.Padding = new Thickness(19, view.Padding.Top, 19, view.Padding.Bottom);
                }
                else if(deviceInfo.Scale < 3) // xhdpi
                {
                    view.Padding = new Thickness(17, view.Padding.Top, 17, view.Padding.Bottom);
                }
                else // xxhdpi and xxxhdpi
                {
                    view.Padding = new Thickness(15, view.Padding.Top, 15, view.Padding.Bottom);
                }
            }
        }

        public static bool LastActionWasRecent(this DateTime? lastAction, int milliseconds = 1000)
        {
            if(lastAction.HasValue && (DateTime.UtcNow - lastAction.Value).TotalMilliseconds < milliseconds)
            {
                System.Diagnostics.Debug.WriteLine("Last action occurred recently.");
                return true;
            }

            return false;
        }
    }
}
