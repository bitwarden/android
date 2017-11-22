using Bit.App.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Controls
{
    public class ExtendedContentPage : ContentPage
    {
        private ISyncService _syncService;
        private IGoogleAnalyticsService _googleAnalyticsService;
        private ILockService _lockService;
        private IDeviceActionService _deviceActionService;
        private bool _syncIndicator;
        private bool _updateActivity;

        public ExtendedContentPage(bool syncIndicator = false, bool updateActivity = true)
        {
            _syncIndicator = syncIndicator;
            _updateActivity = updateActivity;
            _syncService = Resolver.Resolve<ISyncService>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            _lockService = Resolver.Resolve<ILockService>();
            _deviceActionService = Resolver.Resolve<IDeviceActionService>();

            BackgroundColor = Color.FromHex("efeff4");
        }

        protected override void OnAppearing()
        {
            if(_syncIndicator)
            {
                MessagingCenter.Subscribe<ISyncService, bool>(_syncService, "SyncCompleted",
                    (sender, success) => Device.BeginInvokeOnMainThread(() => IsBusy = _syncService.SyncInProgress));
                MessagingCenter.Subscribe<ISyncService>(_syncService, "SyncStarted",
                    (sender) => Device.BeginInvokeOnMainThread(() => IsBusy = _syncService.SyncInProgress));
            }

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
                MessagingCenter.Unsubscribe<ISyncService, bool>(_syncService, "SyncCompleted");
                MessagingCenter.Unsubscribe<ISyncService>(_syncService, "SyncStarted");
            }

            if(_syncIndicator)
            {
                IsBusy = false;
            }

            if(_updateActivity)
            {
                _lockService.UpdateLastActivity();
            }

            base.OnDisappearing();
            _deviceActionService.DismissKeyboard();
        }
    }
}
