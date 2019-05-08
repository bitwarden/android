using System;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class BaseContentPage : ContentPage
    {
        protected int AndroidShowModalAnimationDelay = 400;
        protected int AndroidShowPageAnimationDelay = 100;

        public DateTime? LastPageAction { get; set; }

        public bool DoOnce(Action action = null, int milliseconds = 1000)
        {
            if(LastPageAction.HasValue && (DateTime.UtcNow - LastPageAction.Value).TotalMilliseconds < milliseconds)
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
            if(targetView != null)
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
                if(sourceView != null)
                {
                    if(targetView != null)
                    {
                        targetView.Content = sourceView;
                    }
                    else
                    {
                        Content = sourceView;
                    }
                }
            }
            if(Device.RuntimePlatform == Device.iOS)
            {
                await DoWorkAsync();
                return;
            }
            await Task.Run(async () =>
            {
                await Task.Delay(fromModal ? AndroidShowModalAnimationDelay : AndroidShowPageAnimationDelay);
                Device.BeginInvokeOnMainThread(async () => await DoWorkAsync());
            });
        }

        protected void RequestFocus(InputView input)
        {
            if(Device.RuntimePlatform == Device.iOS)
            {
                input.Focus();
                return;
            }
            Task.Run(async () =>
            {
                await Task.Delay(AndroidShowModalAnimationDelay);
                Device.BeginInvokeOnMainThread(() => input.Focus());
            });
        }
    }
}
