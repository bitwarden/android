using System.Diagnostics.CodeAnalysis;
using Bit.App.Pages;

namespace Bit.App.Controls
{
    public abstract class BaseCipherViewCell : ExtendedGrid
    {
        protected virtual CachedImage Icon { get; }

        protected virtual IconLabel IconPlaceholder { get; }

        // HACK: PM-5896 Fix for Background Crash on iOS
        // While loading the cipher icon and the user sent the app to background
        // the app was crashing sometimes when the "LoadingPlaceholder" or "ErrorPlaceholder"
        // were being accessed, thus locked, and as soon the app got suspended by the OS
        // the app would crash because there can't be any lock files by the app when it gets suspended.
        // So, the approach has changed to reuse the IconLabel default icon to use it for these placeholders
        // as well. In order to do that both icon controls change their visibility dynamically here reacting to 
        // CachedImage events and binding context changes.

        protected override void OnBindingContextChanged()
        {
            Icon.Source = null;
            if (BindingContext is CipherItemViewModel cipherItemVM)
            {
                Icon.Source = cipherItemVM.IconImageSource;
                if (!cipherItemVM.IconImageSuccesfullyLoaded)
                {
                    UpdateIconImages(cipherItemVM.ShowIconImage);
                }
            }

            base.OnBindingContextChanged();
        }

        private void UpdateIconImages(bool showIcon)
        {
            MainThread.BeginInvokeOnMainThread(() =>
            {
                if (!showIcon)
                {
                    Icon.IsVisible = false;
                    IconPlaceholder.IsVisible = true;
                    return;
                }

                IconPlaceholder.IsVisible = Icon.IsLoading;
            });
        }

        protected void Icon_Success(object sender, FFImageLoading.Maui.CachedImageEvents.SuccessEventArgs e)
        {
            if (BindingContext is CipherItemViewModel cipherItemVM)
            {
                cipherItemVM.IconImageSuccesfullyLoaded = true;
            }
            MainThread.BeginInvokeOnMainThread(() =>
            {
                Icon.IsVisible = true;
                IconPlaceholder.IsVisible = false;
            });
        }

        protected void Icon_Error(object sender, FFImageLoading.Maui.CachedImageEvents.ErrorEventArgs e)
        {
            if (BindingContext is CipherItemViewModel cipherItemVM)
            {
                cipherItemVM.IconImageSuccesfullyLoaded = false;
            }
            MainThread.BeginInvokeOnMainThread(() =>
            {
                Icon.IsVisible = false;
                IconPlaceholder.IsVisible = true;
            });
        }
    }
}
