using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System;
using System.Threading.Tasks;
using Xamarin.Forms;
using Xamarin.Forms.PlatformConfiguration;
using Xamarin.Forms.PlatformConfiguration.iOSSpecific;

namespace Bit.App.Pages
{
    public class BaseContentPage : ContentPage
    {
        private IStorageService _storageService;

        protected int ShowModalAnimationDelay = 400;
        protected int ShowPageAnimationDelay = 100;

        public BaseContentPage()
        {
            if (Device.RuntimePlatform == Device.iOS)
            {
                On<iOS>().SetModalPresentationStyle(UIModalPresentationStyle.FullScreen);
            }
        }

        public DateTime? LastPageAction { get; set; }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            SaveActivity();
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

        protected void SetActivityIndicator(ContentView targetView = null)
        {
            var indicator = new ActivityIndicator
            {
                IsRunning = true,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center
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

        private void SetStorageService()
        {
            if (_storageService == null)
            {
                _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            }
        }

        private void SaveActivity()
        {
            SetStorageService();
            _storageService.SaveAsync(Constants.LastActiveKey, DateTime.UtcNow);
        }
    }
}
