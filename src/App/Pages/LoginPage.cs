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
using Plugin.Settings.Abstractions;

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
        private ISettings _settings;
        private IGoogleAnalyticsService _googleAnalyticsService;
        private readonly string _email;

        public LoginPage(string email = null)
            : base(updateActivity: false)
        {
            _email = email;
            _cryptoService = Resolver.Resolve<ICryptoService>();
            _authService = Resolver.Resolve<IAuthService>();
            _deviceInfo = Resolver.Resolve<IDeviceInfo>();
            _appIdService = Resolver.Resolve<IAppIdService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _syncService = Resolver.Resolve<ISyncService>();
            _settings = Resolver.Resolve<ISettings>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            Init();
        }

        public FormEntryCell PasswordCell { get; set; }
        public FormEntryCell EmailCell { get; set; }

        private void Init()
        {
            MessagingCenter.Send(Application.Current, "ShowStatusBar", true);

            var padding = Device.OnPlatform(
                iOS: new Thickness(15, 20),
                Android: new Thickness(15, 8),
                WinPhone: new Thickness(15, 20));

            PasswordCell = new FormEntryCell(AppResources.MasterPassword, IsPassword: true,
                useLabelAsPlaceholder: true, imageSource: "lock", containerPadding: padding);
            EmailCell = new FormEntryCell(AppResources.EmailAddress, nextElement: PasswordCell.Entry,
                entryKeyboard: Keyboard.Email, useLabelAsPlaceholder: true, imageSource: "envelope",
                containerPadding: padding);

            var lastLoginEmail = _settings.GetValueOrDefault<string>(Constants.LastLoginEmail);
            if(!string.IsNullOrWhiteSpace(_email))
            {
                EmailCell.Entry.Text = _email;
            }
            else if(!string.IsNullOrWhiteSpace(lastLoginEmail))
            {
                EmailCell.Entry.Text = lastLoginEmail;
            }

            PasswordCell.Entry.ReturnType = Enums.ReturnType.Go;
            PasswordCell.Entry.Completed += Entry_Completed;

            var table = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                EnableScrolling = false,
                HasUnevenRows = true,
                EnableSelection = true,
                NoFooter = true,
                VerticalOptions = LayoutOptions.Start,
                Root = new TableRoot
                {
                    new TableSection()
                    {
                        EmailCell,
                        PasswordCell
                    }
                }
            };

            var forgotPasswordButton = new ExtendedButton
            {
                Text = "Get your master password hint",
                Style = (Style)Application.Current.Resources["btn-primaryAccent"],
                Margin = new Thickness(15, 0, 15, 25),
                Command = new Command(async () => await ForgotPasswordAsync()),
                Uppercase = false,
                BackgroundColor = Color.Transparent
            };

            var layout = new StackLayout
            {
                Children = { table, forgotPasswordButton },
                Spacing = Device.OnPlatform(iOS: 0, Android: 10, WinPhone: 0)
            };

            var scrollView = new ScrollView { Content = layout };

            if(Device.OS == TargetPlatform.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 70;
                ToolbarItems.Add(new DismissModalToolBarItem(this, "Cancel", () =>
                {
                    MessagingCenter.Send(Application.Current, "ShowStatusBar", false);
                }));
            }

            var loginToolbarItem = new ToolbarItem(AppResources.LogIn, null, async () =>
            {
                await LogIn();
            }, ToolbarItemOrder.Default, 0);

            ToolbarItems.Add(loginToolbarItem);
            Title = AppResources.Bitwarden;
            Content = scrollView;
            NavigationPage.SetBackButtonTitle(this, "Log In");
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            MessagingCenter.Send(Application.Current, "ShowStatusBar", true);

            if(string.IsNullOrWhiteSpace(_email))
            {
                if(!string.IsNullOrWhiteSpace(EmailCell.Entry.Text))
                {
                    PasswordCell.Entry.Focus();
                }
                else
                {
                    EmailCell.Entry.Focus();
                }
            }
        }

        private async void Entry_Completed(object sender, EventArgs e)
        {
            await LogIn();
        }

        private async Task ForgotPasswordAsync()
        {
            await Navigation.PushAsync(new PasswordHintPage());
        }

        private async Task LogIn()
        {
            if(string.IsNullOrWhiteSpace(EmailCell.Entry.Text))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired,
                    AppResources.EmailAddress), AppResources.Ok);
                return;
            }

            if(string.IsNullOrWhiteSpace(PasswordCell.Entry.Text))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired,
                    AppResources.MasterPassword), AppResources.Ok);
                return;
            }

            var normalizedEmail = EmailCell.Entry.Text.ToLower();

            var key = _cryptoService.MakeKeyFromPassword(PasswordCell.Entry.Text, normalizedEmail);

            var request = new TokenRequest
            {
                Email = normalizedEmail,
                MasterPasswordHash = _cryptoService.HashPasswordBase64(key, PasswordCell.Entry.Text),
                Device = new DeviceRequest(_appIdService, _deviceInfo)
            };

            _userDialogs.ShowLoading("Logging in...", MaskType.Black);
            var response = await _authService.TokenPostAsync(request);
            _userDialogs.HideLoading();
            if(!response.Succeeded)
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, response.Errors.FirstOrDefault()?.Message, AppResources.Ok);
                return;
            }

            _cryptoService.Key = key;
            _authService.Token = response.Result.Token;
            _authService.UserId = response.Result?.Profile?.Id;
            _authService.Email = response.Result?.Profile?.Email;
            _settings.AddOrUpdateValue(Constants.LastLoginEmail, _authService.Email);
            _googleAnalyticsService.RefreshUserId();
            _googleAnalyticsService.TrackAppEvent("LoggedIn");

            if(_authService.IsAuthenticatedTwoFactor)
            {
                await Navigation.PushAsync(new LoginTwoFactorPage());
            }
            else
            {
                var task = Task.Run(async () => await _syncService.FullSyncAsync());
                Application.Current.MainPage = new MainPage();
            }
        }
    }
}
