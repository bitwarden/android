using Bit.App.Abstractions;
using Plugin.Settings.Abstractions;
using System;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Controls
{
    public class ExtendedContentPage : ContentPage
    {
        private ISyncService _syncService;
        private IGoogleAnalyticsService _googleAnalyticsService;
        private ISettings _settings;
        private bool _syncIndicator;
        private bool _updateActivity;

        public ExtendedContentPage(bool syncIndicator = false, bool updateActivity = true)
        {
            _syncIndicator = syncIndicator;
            _updateActivity = updateActivity;
            _syncService = Resolver.Resolve<ISyncService>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            _settings = Resolver.Resolve<ISettings>();

            BackgroundColor = Color.FromHex("efeff4");

            if(_syncIndicator)
            {
                MessagingCenter.Subscribe<Application, bool>(Application.Current, "SyncCompleted", (sender, success) =>
                {
                    Device.BeginInvokeOnMainThread(() => IsBusy = _syncService.SyncInProgress);
                });

                MessagingCenter.Subscribe<Application>(Application.Current, "SyncStarted", (sender) =>
                {
                    Device.BeginInvokeOnMainThread(() => IsBusy = _syncService.SyncInProgress);
                });
            }
        }

        protected override void OnAppearing()
        {
            if(_syncIndicator)
            {
                IsBusy = _syncService.SyncInProgress;
            }

            if(_updateActivity)
            {
                _settings.AddOrUpdateValue(Constants.LastActivityDate, DateTime.UtcNow);
            }

            _googleAnalyticsService.TrackPage(GetType().Name);
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
