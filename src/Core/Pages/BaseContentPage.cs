using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
#if IOS
using Microsoft.Maui.Controls.PlatformConfiguration;
using Microsoft.Maui.Controls.PlatformConfiguration.iOSSpecific;
#endif

namespace Bit.App.Pages
{
    public class BaseContentPage : ContentPage
    {
        private readonly LazyResolve<IStateService> _stateService = new LazyResolve<IStateService>();
        private readonly LazyResolve<IDeviceActionService> _deviceActionService = new LazyResolve<IDeviceActionService>();
        private readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>();

        protected int ShowModalAnimationDelay = 400;
        protected int ShowPageAnimationDelay = 100;

        /// <summary>
        /// Used as a workaround to avoid duplicate initialization issues for some pages where OnNavigatedTo is called twice.
        /// </summary>
        private bool _hasInitedOnNavigatedTo;

        public BaseContentPage()
        {
#if IOS
            On<iOS>().SetUseSafeArea(true);
            On<iOS>().SetModalPresentationStyle(UIModalPresentationStyle.FullScreen);
#endif
        }

        public DateTime? LastPageAction { get; set; }

        public bool IsThemeDirty { get; set; }

        /// <summary>
        /// This flag is used to see if check is needed to avoid duplicate calls of <see cref="OnNavigatedTo(NavigatedToEventArgs)"/>
        /// Usually on modal navigation to the current page this flag should be <c>true</c>
        /// Also this flag is added instead of directly checking for all pages to avoid potential issues on the app
        /// and focusing only on the places where it's actually needed.
        /// </summary>
        /// <remarks>
        /// This should be removed once MAUI fixes the issue of duplicate call to the method.
        /// </remarks>
        protected virtual bool ShouldCheckToPreventOnNavigatedToCalledTwice => false;

        protected override async void OnNavigatedTo(NavigatedToEventArgs args)
        {
            try
            {
                base.OnNavigatedTo(args);

                if (IsThemeDirty)
                {
                    try
                    {
                        await UpdateOnThemeChanged();
                    }
                    catch (Exception ex)
                    {
                        Core.Services.LoggerHelper.LogEvenIfCantBeResolved(ex);
                        // Don't rethrow on theme changed so the user can still continue on the app.
                    }
                }

                await SaveActivityAsync();

                if (ShouldCheckToPreventOnNavigatedToCalledTwice && _hasInitedOnNavigatedTo)
                {
                    return;
                }
                _hasInitedOnNavigatedTo = true;

                await InitOnNavigatedToAsync();
            }
            catch (Exception ex)
            {
                Core.Services.LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw;
            }
        }

        protected virtual Task InitOnNavigatedToAsync() => Task.CompletedTask;

        protected override void OnNavigatedFrom(NavigatedFromEventArgs args)
        {
            base.OnNavigatedFrom(args);
            _hasInitedOnNavigatedTo = false;
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
                try
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
                catch (Exception ex)
                {
                    _logger.Value.Exception(ex);
                    throw;
                }
            }

#if IOS
            await DoWorkAsync();
#else
            await Task.Run(async () =>
            {
                await Task.Delay(fromModal ? ShowModalAnimationDelay : ShowPageAnimationDelay);
                MainThread.BeginInvokeOnMainThread(async () => await DoWorkAsync());
            });
#endif
        }

        protected void RequestFocus(InputView input)
        {
            Task.Run(async () =>
            {
                try
                {
                    await Task.Delay(ShowModalAnimationDelay);
                    MainThread.BeginInvokeOnMainThread(() => input.Focus());
                }
                catch (Exception ex)
                {
                    _logger.Value.Exception(ex);
                }
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
