using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Microsoft.Maui.Controls.PlatformConfiguration;
using Microsoft.Maui.Controls.PlatformConfiguration.iOSSpecific;

namespace Bit.App.Pages
{
    public class BaseContentPage : ContentPage
    {
        private readonly LazyResolve<IStateService> _stateService = new LazyResolve<IStateService>();
        private readonly LazyResolve<IDeviceActionService> _deviceActionService = new LazyResolve<IDeviceActionService>();

        protected int ShowModalAnimationDelay = 400;
        protected int ShowPageAnimationDelay = 100;

        public BaseContentPage()
        {
            if (DeviceInfo.Platform == DevicePlatform.iOS)
            {
                On<iOS>().SetUseSafeArea(true);
                On<iOS>().SetModalPresentationStyle(UIModalPresentationStyle.FullScreen);
            }
        }

        //IsInitialized is used as a workaround to avoid duplicate initialization issues for some pages where OnNavigatedTo is called twice.
        protected bool HasInitialized { get; set; }

        public DateTime? LastPageAction { get; set; }

        public bool IsThemeDirty { get; set; }

        protected override async void OnNavigatedTo(NavigatedToEventArgs args)
        {
            base.OnNavigatedTo(args);

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
                VerticalOptions = LayoutOptions.Center,
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
            if (DeviceInfo.Platform == DevicePlatform.iOS)
            {
                await DoWorkAsync();
                return;
            }
            await Task.Run(async () =>
            {
                await Task.Delay(fromModal ? ShowModalAnimationDelay : ShowPageAnimationDelay);
                MainThread.BeginInvokeOnMainThread(async () => await DoWorkAsync());
            });
        }

        protected void RequestFocus(InputView input)
        {
            Task.Run(async () =>
            {
                await Task.Delay(ShowModalAnimationDelay);
                MainThread.BeginInvokeOnMainThread(() => input.Focus());
            });
        }

        protected async Task<bool> ShowAccountSwitcherAsync()
        {
            return await _stateService.Value.GetActiveUserIdAsync() != null;
        }

        protected async Task<AvatarImageSource> GetAvatarImageSourceAsync(bool useCurrentActiveAccount = true)
        {
            if (useCurrentActiveAccount)
            {
                var user = await _stateService.Value.GetActiveUserCustomDataAsync(a => (a?.Profile?.UserId, a?.Profile?.Name, a?.Profile?.Email, a?.Profile?.AvatarColor));
                return new AvatarImageSource(user.UserId, user.Name, user.Email, user.AvatarColor);
            }
            return new AvatarImageSource();
        }

        private async Task SaveActivityAsync()
        {
            if (await _stateService.Value.GetActiveUserIdAsync() == null)
            {
                // Fresh install and/or all users logged out won't have an active user, skip saving last active time
                return;
            }

            await _stateService.Value.SetLastActiveTimeAsync(_deviceActionService.Value.GetActiveTime());
        }
    }
}
