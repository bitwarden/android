using Bit.App.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Controls
{
    public class ExtendedContentPage : ContentPage
    {
        private ISyncService _syncService;
        private bool _syncIndicator;

        public ExtendedContentPage(bool syncIndicator = false)
        {
            _syncIndicator = syncIndicator;
            _syncService = Resolver.Resolve<ISyncService>();

            BackgroundColor = Color.FromHex("efeff4");

            if(_syncIndicator)
            {
                MessagingCenter.Subscribe<Application, bool>(Application.Current, "SyncCompleted", (sender, success) =>
                {
                    IsBusy = _syncService.SyncInProgress;
                });

                MessagingCenter.Subscribe<Application>(Application.Current, "SyncStarted", (sender) =>
                {
                    IsBusy = _syncService.SyncInProgress;
                });
            }
        }

        protected override void OnAppearing()
        {
            if(_syncIndicator)
            {
                IsBusy = _syncService.SyncInProgress;
            }

            var googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            googleAnalyticsService.TrackPage(GetType().Name);
            base.OnAppearing();
        }

        protected override void OnDisappearing()
        {
            if(_syncIndicator)
            {
                IsBusy = false;
            }

            base.OnDisappearing();
        }
    }
}
