using System;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Acr.UserDialogs;
using System.Threading.Tasks;
using PushNotification.Plugin.Abstractions;
using Bit.App.Models;

namespace Bit.App.Pages
{
    public class LoginTwoFactorPage : ExtendedContentPage
    {
        private IAuthService _authService;
        private IUserDialogs _userDialogs;
        private ISyncService _syncService;
        private IGoogleAnalyticsService _googleAnalyticsService;
        private IPushNotification _pushNotification;
        private readonly string _email;
        private readonly string _masterPasswordHash;
        private readonly CryptoKey _key;

        public LoginTwoFactorPage(string email, string masterPasswordHash, CryptoKey key)
            : base(updateActivity: false)
        {
            _email = email;
            _masterPasswordHash = masterPasswordHash;
            _key = key;

            _authService = Resolver.Resolve<IAuthService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _syncService = Resolver.Resolve<ISyncService>();
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
                await LogInAsync();
            }, ToolbarItemOrder.Default, 0);

            ToolbarItems.Add(continueToolbarItem);
            Title = AppResources.VerificationCode;
            Content = scrollView;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            CodeCell.InitEvents();
            CodeCell.Entry.FocusWithDelay();
            CodeCell.Entry.Completed += Entry_Completed;
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            CodeCell.Dispose();
            CodeCell.Entry.Completed -= Entry_Completed;
        }

        private void Lost2FAApp()
        {
            Device.OpenUri(new Uri("https://vault.bitwarden.com/#/recover"));
        }

        private async void Entry_Completed(object sender, EventArgs e)
        {
            await LogInAsync();
        }

        private async Task LogInAsync()
        {
            if(string.IsNullOrWhiteSpace(CodeCell.Entry.Text))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired,
                    AppResources.VerificationCode), AppResources.Ok);
                return;
            }

            _userDialogs.ShowLoading(AppResources.ValidatingCode, MaskType.Black);
            var response = await _authService.TokenPostTwoFactorAsync(CodeCell.Entry.Text, _email, _masterPasswordHash, _key);
            _userDialogs.HideLoading();
            if(!response.Success)
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, response.ErrorMessage, AppResources.Ok);
                return;
            }

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
