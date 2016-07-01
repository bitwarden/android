using System;
using System.Linq;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models.Api;
using Bit.App.Resources;
using Plugin.DeviceInfo.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;
using Acr.UserDialogs;
using System.Threading.Tasks;

namespace Bit.App.Pages
{
    public class LoginPage : ExtendedContentPage
    {
        private ICryptoService _cryptoService;
        private IAuthService _authService;
        private IDeviceInfo _deviceInfo;
        private IAppIdService _appIdService;
        private IUserDialogs _userDialogs;
        private ISyncService _syncService;

        public LoginPage()
        {
            _cryptoService = Resolver.Resolve<ICryptoService>();
            _authService = Resolver.Resolve<IAuthService>();
            _deviceInfo = Resolver.Resolve<IDeviceInfo>();
            _appIdService = Resolver.Resolve<IAppIdService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _syncService = Resolver.Resolve<ISyncService>();

            Init();
        }

        public FormEntryCell PasswordCell { get; set; }
        public FormEntryCell EmailCell { get; set; }

        private void Init()
        {
            PasswordCell = new FormEntryCell(AppResources.MasterPassword, IsPassword: true,
                useLabelAsPlaceholder: true, imageSource: "lock");
            EmailCell = new FormEntryCell(AppResources.EmailAddress, nextElement: PasswordCell.Entry,
                entryKeyboard: Keyboard.Email, useLabelAsPlaceholder: true, imageSource: "envelope");

            PasswordCell.Entry.ReturnType = Enums.ReturnType.Go;
            PasswordCell.Entry.Completed += Entry_Completed;

            var table = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                EnableScrolling = true,
                HasUnevenRows = true,
                EnableSelection = false,
                Root = new TableRoot
                {
                    new TableSection()
                    {
                        EmailCell,
                        PasswordCell
                    }
                }
            };

            var loginToolbarItem = new ToolbarItem(AppResources.LogIn, null, async () =>
            {
                await LogIn();
            }, ToolbarItemOrder.Default, 0);

            if(Device.OS == TargetPlatform.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 70;
                ToolbarItems.Add(new DismissModalToolBarItem(this, "Cancel"));
            }

            ToolbarItems.Add(loginToolbarItem);
            Title = AppResources.Bitwarden;
            Content = table;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            EmailCell.Entry.Focus();
        }

        private async void Entry_Completed(object sender, EventArgs e)
        {
            await LogIn();
        }

        private async Task LogIn()
        {
            if(string.IsNullOrWhiteSpace(EmailCell.Entry.Text))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, AppResources.EmailAddress), AppResources.Ok);
                return;
            }

            if(string.IsNullOrWhiteSpace(PasswordCell.Entry.Text))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, AppResources.MasterPassword), AppResources.Ok);
                return;
            }

            var key = _cryptoService.MakeKeyFromPassword(PasswordCell.Entry.Text, EmailCell.Entry.Text);

            var request = new TokenRequest
            {
                Email = EmailCell.Entry.Text,
                MasterPasswordHash = _cryptoService.HashPasswordBase64(key, PasswordCell.Entry.Text),
                Device = new DeviceRequest(_appIdService, _deviceInfo)
            };

            var responseTask = _authService.TokenPostAsync(request);
            _userDialogs.ShowLoading("Logging in...", MaskType.Black);
            var response = await responseTask;
            _userDialogs.HideLoading();
            if(!response.Succeeded)
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, response.Errors.FirstOrDefault()?.Message, AppResources.Ok);
                return;
            }

            _cryptoService.Key = key;
            _authService.Token = response.Result.Token;
            _authService.UserId = response.Result.Profile.Id;

            var syncTask = _syncService.FullSyncAsync();
            Application.Current.MainPage = new MainPage();
        }
    }
}
