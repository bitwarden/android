using System;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Plugin.Connectivity.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;
using Plugin.Settings.Abstractions;

namespace Bit.App.Pages
{
    public class SettingsSyncPage : ExtendedContentPage
    {
        private readonly ISyncService _syncService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IConnectivity _connectivity;
        private readonly ISettings _settings;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;

        public SettingsSyncPage()
        {
            _syncService = Resolver.Resolve<ISyncService>();
            _deviceActionService = Resolver.Resolve<IDeviceActionService>();
            _connectivity = Resolver.Resolve<IConnectivity>();
            _settings = Resolver.Resolve<ISettings>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            Init();
        }

        public Label LastSyncLabel { get; set; }

        public void Init()
        {
            var syncButton = new ExtendedButton
            {
                Text = AppResources.SyncVaultNow,
                Command = new Command(async () => await SyncAsync()),
                Style = (Style)Application.Current.Resources["btn-primaryAccent"]
            };

            LastSyncLabel = new Label
            {
                Style = (Style)Application.Current.Resources["text-muted"],
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                HorizontalTextAlignment = TextAlignment.Center
            };

            SetLastSync();

            var stackLayout = new StackLayout
            {
                VerticalOptions = LayoutOptions.CenterAndExpand,
                Children = { syncButton, LastSyncLabel },
                Padding = new Thickness(15, 0)
            };

            if(Device.RuntimePlatform == Device.iOS || Device.RuntimePlatform == Device.UWP)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Close));
            }

            Title = AppResources.Sync;
            Content = stackLayout;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            if(!_connectivity.IsConnected)
            {
                AlertNoConnection();
            }
        }

        private void SetLastSync()
        {
            DateTime? lastSyncDate = null;
            if(_settings.Contains(Constants.LastSync))
            {
                lastSyncDate = _settings.GetValueOrDefault(Constants.LastSync, DateTime.UtcNow);
            }
            try
            {
                LastSyncLabel.Text = AppResources.LastSync + " " + lastSyncDate?.ToLocalTime().ToString() ?? AppResources.Never;
            }
            catch
            {
                // some users with different calendars have issues with ToString()ing a date
                // it seems the linker is at fault. just catch for now since this isn't that important.
                // ref http://bit.ly/2c2JU7b
            }
        }

        public async Task SyncAsync()
        {
            if(!_connectivity.IsConnected)
            {
                AlertNoConnection();
                return;
            }

            await _deviceActionService.ShowLoadingAsync(AppResources.Syncing);
            var succeeded = await _syncService.FullSyncAsync(true);
            await _deviceActionService.HideLoadingAsync();
            if(succeeded)
            {
                _deviceActionService.Toast(AppResources.SyncingComplete);
                _googleAnalyticsService.TrackAppEvent("Synced");
            }
            else
            {
                _deviceActionService.Toast(AppResources.SyncingFailed);
            }

            SetLastSync();
        }

        public void AlertNoConnection()
        {
            DisplayAlert(AppResources.InternetConnectionRequiredTitle, AppResources.InternetConnectionRequiredMessage,
                AppResources.Ok);
        }
    }
}
