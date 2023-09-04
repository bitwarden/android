using System;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Forms;
using Xamarin.Forms.PlatformConfiguration;
using Xamarin.Forms.PlatformConfiguration.iOSSpecific;

namespace Bit.App.Pages
{
    public class BaseContentPage : ContentPage
    {
        private IStateService _stateService;
        private IDeviceActionService _deviceActionService;

        protected int ShowModalAnimationDelay = 400;
        protected int ShowPageAnimationDelay = 100;

        public BaseContentPage()
        {
            if (Device.RuntimePlatform == Device.iOS)
            {
                On<iOS>().SetUseSafeArea(true);
                On<iOS>().SetModalPresentationStyle(UIModalPresentationStyle.FullScreen);
            }
        }

        public DateTime? LastPageAction { get; set; }

        public bool IsThemeDirty { get; set; }

        protected async override void OnAppearing()
        {
            base.OnAppearing();

            if (IsThemeDirty)
            {
                UpdateOnThemeChanged();
            }

            await SaveActivityAsync();
        }

        public bool DoOnce(Action action = null, int milliseconds = 1000)
        {
            if (LastPageAction.HasValue && (DateTime.UtcNow - LastPageAction.Value).TotalMilliseconds < milliseconds)
            {
                // Last action occurred recently.
                return false;
            }
            LastPageAction = DateTime.UtcNow;
            action?.Invoke();
            return true;
        }

        public virtual Task UpdateOnThemeChanged()
        {
            IsThemeDirty = false;
            return Task.CompletedTask;
        }

        protected void SetActivityIndicator(ContentView targetView = null)
        {
            var indicator = new ActivityIndicator
            {
                IsRunning = true,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center,
                Color = ThemeManager.GetResourceColor("PrimaryColor"),
            };
            if (targetView != null)
            {
                targetView.Content = indicator;
            }
            else
            {
                Content = indicator;
            }
        }

        protected async Task LoadOnAppearedAsync(View sourceView, bool fromModal, Func<Task> workFunction,
            ContentView targetView = null)
        {
            async Task DoWorkAsync()
            {
                await workFunction.Invoke();
                if (sourceView != null)
                {
                    if (targetView != null)
                    {
                        targetView.Content = sourceView;
                    }
                    else
                    {
                        Content = sourceView;
                    }
                }
            }
            if (Device.RuntimePlatform == Device.iOS)
            {
                await DoWorkAsync();
                return;
            }
            await Task.Run(async () =>
            {
                await Task.Delay(fromModal ? ShowModalAnimationDelay : ShowPageAnimationDelay);
                Device.BeginInvokeOnMainThread(async () => await DoWorkAsync());
            });
        }

        protected void RequestFocus(InputView input)
        {
            Task.Run(async () =>
            {
                await Task.Delay(ShowModalAnimationDelay);
                Device.BeginInvokeOnMainThread(() => input.Focus());
            });
        }

        protected async Task<bool> ShowAccountSwitcherAsync()
        {
            return await _stateService.GetActiveUserIdAsync() != null;
        }

        protected async Task<AvatarImageSource> GetAvatarImageSourceAsync(bool useCurrentActiveAccount = true)
        {
            if (useCurrentActiveAccount)
            {
                var user = await _stateService.GetActiveUserCustomDataAsync(a => (a?.Profile?.UserId, a?.Profile?.Name, a?.Profile?.Email, a?.Profile?.AvatarColor));
                return new AvatarImageSource(user.UserId, user.Name, user.Email, user.AvatarColor);
            }
            return new AvatarImageSource();
        }

        private void SetServices()
        {
            if (_stateService == null)
            {
                _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            }
            if (_deviceActionService == null)
            {
                _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            }
        }

        private async Task SaveActivityAsync()
        {
            SetServices();
            if (await _stateService.GetActiveUserIdAsync() == null)
            {
                // Fresh install and/or all users logged out won't have an active user, skip saving last active time
                return;
            }

            await _stateService.SetLastActiveTimeAsync(_deviceActionService.GetActiveTime());
        }
    }
}
