using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class SyncPage : ContentPage
    {
        private readonly ISyncService _syncService;
        private readonly IUserDialogs _userDialogs;

        public SyncPage()
        {
            _syncService = Resolver.Resolve<ISyncService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();

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
        }

        public async Task SyncAsync()
        {
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
    }
}
