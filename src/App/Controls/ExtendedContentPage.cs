using Bit.App.Abstractions;
using Bit.App.Pages;
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
        private IAuthService _authService;
        private bool _syncIndicator;
        private bool _updateActivity;
        private bool _requireAuth;

        public ExtendedContentPage(bool syncIndicator = false, bool updateActivity = true, bool requireAuth = true)
        {
            _syncIndicator = syncIndicator;
            _updateActivity = updateActivity;
            _requireAuth = requireAuth;
            _syncService = Resolver.Resolve<ISyncService>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            _lockService = Resolver.Resolve<ILockService>();
            _deviceActionService = Resolver.Resolve<IDeviceActionService>();
            _authService = Resolver.Resolve<IAuthService>();

            BackgroundColor = Color.FromHex("efeff4");
        }

        protected async override void OnAppearing()
        {
            if(_requireAuth && !_authService.IsAuthenticated)
            {
                Device.BeginInvokeOnMainThread(
                    () => Application.Current.MainPage = new ExtendedNavigationPage(new HomePage()));
            }

            if(_syncIndicator)
            {
                MessagingCenter.Subscribe<Application, bool>(Application.Current, "SyncCompleted",
                    (sender, success) => Device.BeginInvokeOnMainThread(() => IsBusy = _syncService.SyncInProgress));
                MessagingCenter.Subscribe<ISyncService>(Application.Current, "SyncStarted",
                    (sender) => Device.BeginInvokeOnMainThread(() => IsBusy = _syncService.SyncInProgress));
            }

            if(_syncIndicator)
            {
                IsBusy = _syncService.SyncInProgress;
            }

            _googleAnalyticsService.TrackPage(GetType().Name);
            await _lockService.CheckLockAsync(false, true);
            base.OnAppearing();
        }

        protected override void OnDisappearing()
        {
            if(_syncIndicator)
            {
                MessagingCenter.Unsubscribe<Application, bool>(Application.Current, "SyncCompleted");
                MessagingCenter.Unsubscribe<Application>(Application.Current, "SyncStarted");
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
