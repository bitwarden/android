using System;
using System.Linq;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models.Api;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Acr.UserDialogs;
using System.Threading.Tasks;
using Plugin.Settings.Abstractions;
using PushNotification.Plugin.Abstractions;

namespace Bit.App.Pages
{
    public class LoginTwoFactorPage : ExtendedContentPage
    {
        private ICryptoService _cryptoService;
        private IAuthService _authService;
        private ITokenService _tokenService;
        private IDeviceInfoService _deviceInfoService;
        private IAppIdService _appIdService;
        private IUserDialogs _userDialogs;
        private ISyncService _syncService;
        private ISettings _settings;
        private IGoogleAnalyticsService _googleAnalyticsService;
        private IPushNotification _pushNotification;
        private readonly string _email;
        private readonly string _masterPasswordHash;
        private readonly byte[] _key;

        public LoginTwoFactorPage(string email, string masterPasswordHash, byte[] key)
            : base(updateActivity: false)
        {
            _email = email;
            _masterPasswordHash = masterPasswordHash;
            _key = key;

            _cryptoService = Resolver.Resolve<ICryptoService>();
            _authService = Resolver.Resolve<IAuthService>();
            _tokenService = Resolver.Resolve<ITokenService>();
            _deviceInfoService = Resolver.Resolve<IDeviceInfoService>();
            _appIdService = Resolver.Resolve<IAppIdService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _syncService = Resolver.Resolve<ISyncService>();
            _settings = Resolver.Resolve<ISettings>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            _pushNotification = Resolver.Resolve<IPushNotification>();

            Init();
        }

        public FormEntryCell CodeCell { get; set; }

        private void Init()
        {
            var padding = Device.OnPlatform(
                iOS: new Thickness(15, 20),
                Android: new Thickness(15, 8),
                WinPhone: new Thickness(15, 20));

            CodeCell = new FormEntryCell(AppResources.VerificationCode, useLabelAsPlaceholder: true,
                imageSource: "lock", containerPadding: padding);

            CodeCell.Entry.Keyboard = Keyboard.Numeric;
            CodeCell.Entry.ReturnType = Enums.ReturnType.Go;
            CodeCell.Entry.Completed += Entry_Completed;

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
                        CodeCell
                    }
                }
            };

            var codeLabel = new Label
            {
                Text = AppResources.EnterVerificationCode,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                Style = (Style)Application.Current.Resources["text-muted"],
                Margin = new Thickness(15, (this.IsLandscape() ? 5 : 0), 15, 25)
            };

            var lostAppButton = new ExtendedButton
            {
                Text = AppResources.Lost2FAApp,
                Style = (Style)Application.Current.Resources["btn-primaryAccent"],
                Margin = new Thickness(15, 0, 15, 25),
                Command = new Command(() => Lost2FAApp()),
                Uppercase = false,
                BackgroundColor = Color.Transparent
            };

            var layout = new StackLayout
            {
                Children = { table, codeLabel, lostAppButton },
                Spacing = 0
            };

            var scrollView = new ScrollView { Content = layout };

            if(Device.OS == TargetPlatform.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 70;
            }

            var continueToolbarItem = new ToolbarItem(AppResources.Continue, null, async () =>
            {
                await LogIn();
            }, ToolbarItemOrder.Default, 0);

            ToolbarItems.Add(continueToolbarItem);
            Title = AppResources.VerificationCode;
            Content = scrollView;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            CodeCell.Entry.FocusWithDelay();
        }

        private void Lost2FAApp()
        {
            Device.OpenUri(new Uri("https://vault.bitwarden.com/#/recover"));
        }

        private async void Entry_Completed(object sender, EventArgs e)
        {
            await LogIn();
        }

        private async Task LogIn()
        {
            if(string.IsNullOrWhiteSpace(CodeCell.Entry.Text))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired,
                    AppResources.VerificationCode), AppResources.Ok);
                return;
            }

            var request = new TokenRequest
            {
                Email = _email,
                MasterPasswordHash = _masterPasswordHash,
                Token = CodeCell.Entry.Text.Replace(" ", ""),
                Provider = 0, // Authenticator app (only 1 provider for now, so hard coded)
                Device = new DeviceRequest(_appIdService, _deviceInfoService)
            };

            _userDialogs.ShowLoading(AppResources.ValidatingCode, MaskType.Black);
            var response = await _authService.TokenPostAsync(request);
            _userDialogs.HideLoading();
            if(!response.Succeeded)
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, response.Errors.FirstOrDefault()?.Message, AppResources.Ok);
                return;
            }

            _cryptoService.Key = _key;
            _tokenService.Token = response.Result.AccessToken;
            _tokenService.RefreshToken = response.Result.RefreshToken;
            _authService.UserId = _tokenService.TokenUserId;
            _authService.Email = _tokenService.TokenEmail;
            _settings.AddOrUpdateValue(Constants.LastLoginEmail, _authService.Email);
            _googleAnalyticsService.RefreshUserId();
            _googleAnalyticsService.TrackAppEvent("LoggedIn From Two-step");

            if(Device.OS == TargetPlatform.Android)
            {
                _pushNotification.Register();
            }

            var task = Task.Run(async () => await _syncService.FullSyncAsync(true));
            Application.Current.MainPage = new MainPage();
        }
    }
}
