using System;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class BaseContentPage : ContentPage
    {
        protected void SetActivityIndicator()
        {
            Content = new ActivityIndicator
            {
                IsRunning = true,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center
            };
        }

        protected async Task LoadOnAppearedAsync(View viewToSet, bool fromModal, Func<Task> workFunction)
        {
            async Task LoadAsync()
            {
                await workFunction.Invoke();
                if(viewToSet != null)
                {
                    Content = viewToSet;
                }
            }
            if(Device.RuntimePlatform == Device.iOS)
            {
                await LoadAsync();
                return;
            }
            await Task.Run(async () =>
            {
                await Task.Delay(fromModal ? 400 : 200);
                Device.BeginInvokeOnMainThread(async () => await LoadAsync());
            });
        }
    }
}
