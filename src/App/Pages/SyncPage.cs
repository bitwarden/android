using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Plugin.Connectivity.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class SyncPage : ContentPage
    {
        private readonly ISyncService _syncService;
        private readonly IUserDialogs _userDialogs;
        private readonly IConnectivity _connectivity;

        public SyncPage()
        {
            _syncService = Resolver.Resolve<ISyncService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _connectivity = Resolver.Resolve<IConnectivity>();

            Init();
        }

        public void Init()
        {
            var syncButton = new Button
            {
                Text = "Sync Vault",
                Command = new Command(async () => await SyncAsync())
            };

            var stackLayout = new StackLayout { };
            stackLayout.Children.Add(syncButton);

            Title = "Sync";
            Content = stackLayout;
            Icon = "fa-refresh";

            if(!_connectivity.IsConnected)
            {
                AlertNoConnection();
            }
        }

        public async Task SyncAsync()
        {
            if(!_connectivity.IsConnected)
            {
                AlertNoConnection();
                return;
            }

            _userDialogs.ShowLoading("Syncing...", MaskType.Black);
            var succeeded = await _syncService.SyncAsync();
            _userDialogs.HideLoading();
            if(succeeded)
            {
                _userDialogs.SuccessToast("Syncing complete.");
            }
            else
            {
                _userDialogs.ErrorToast("Syncing failed.");
            }
        }

        public void AlertNoConnection()
        {
            DisplayAlert("No internet connection", "Adding a new folder required an internet connection. Please connect to the internet before continuing.", "Ok");
        }
    }
}
