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
        private ILockService _lockService;
        private bool _syncIndicator;
        private bool _updateActivity;

        public ExtendedContentPage(bool syncIndicator = false, bool updateActivity = true)
        {
            _syncIndicator = syncIndicator;
            _updateActivity = updateActivity;
            _syncService = Resolver.Resolve<ISyncService>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            _lockService = Resolver.Resolve<ILockService>();

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

            _googleAnalyticsService.TrackPage(GetType().Name);
            base.OnAppearing();
        }

        protected override void OnDisappearing()
        {
            if(_syncIndicator)
            {
                IsBusy = false;
            }

            if(_updateActivity)
            {
                _lockService.UpdateLastActivity();
            }

            base.OnDisappearing();
            MessagingCenter.Send(Application.Current, "DismissKeyboard");
        }
    }
}
