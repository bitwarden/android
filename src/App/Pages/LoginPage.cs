using System;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Acr.UserDialogs;
using System.Threading.Tasks;
using Plugin.Settings.Abstractions;
using PushNotification.Plugin.Abstractions;
using Bit.App.Utilities;

namespace Bit.App.Pages
{
    public class LoginPage : ExtendedContentPage
    {
        private IAuthService _authService;
        private IUserDialogs _userDialogs;
        private ISyncService _syncService;
        private ISettings _settings;
        private IGoogleAnalyticsService _googleAnalyticsService;
        private IPushNotification _pushNotification;
        private readonly string _email;

        public LoginPage(string email = null)
            : base(updateActivity: false)
        {
            _email = email;
            _authService = Resolver.Resolve<IAuthService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _syncService = Resolver.Resolve<ISyncService>();
            _settings = Resolver.Resolve<ISettings>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            _pushNotification = Resolver.Resolve<IPushNotification>();

            Init();
        }

        public FormEntryCell PasswordCell { get; set; }
        public FormEntryCell EmailCell { get; set; }

        private void Init()
        {
            MessagingCenter.Send(Application.Current, "ShowStatusBar", true);

            var padding = Helpers.OnPlatform(
                iOS: new Thickness(15, 20),
                Android: new Thickness(15, 8),
                WinPhone: new Thickness(15, 20));

            PasswordCell = new FormEntryCell(AppResources.MasterPassword, isPassword: true,
                useLabelAsPlaceholder: true, imageSource: "lock", containerPadding: padding);
            EmailCell = new FormEntryCell(AppResources.EmailAddress, nextElement: PasswordCell.Entry,
                entryKeyboard: Keyboard.Email, useLabelAsPlaceholder: true, imageSource: "envelope",
                containerPadding: padding);

            var lastLoginEmail = _settings.GetValueOrDefault(Constants.LastLoginEmail, string.Empty);
            if(!string.IsNullOrWhiteSpace(_email))
            {
                EmailCell.Entry.Text = _email;
            }
            else if(!string.IsNullOrWhiteSpace(lastLoginEmail))
            {
                EmailCell.Entry.Text = lastLoginEmail;
            }

            PasswordCell.Entry.ReturnType = Enums.ReturnType.Go;

            var table = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                EnableScrolling = false,
                HasUnevenRows = true,
                EnableSelection = true,
                NoFooter = true,
                //NoHeader = true,
                VerticalOptions = LayoutOptions.Start,
                Root = new TableRoot
                {
                    new TableSection(" ")
                    {
                        EmailCell,
                        PasswordCell
                    }
                }
            };

            var forgotPasswordButton = new ExtendedButton
            {
                Text = AppResources.GetPasswordHint,
                Style = (Style)Application.Current.Resources["btn-primaryAccent"],
                Margin = new Thickness(15, 0, 15, 25),
                Command = new Command(async () => await ForgotPasswordAsync()),
                Uppercase = false,
                BackgroundColor = Color.Transparent
            };

            var layout = new StackLayout
            {
                Children = { table, forgotPasswordButton },
                Spacing = Helpers.OnPlatform(iOS: 0, Android: 10, WinPhone: 0)
            };

            var scrollView = new ScrollView { Content = layout };

            if(Device.RuntimePlatform == Device.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 70;
                ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Cancel, () =>
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
            NavigationPage.SetBackButtonTitle(this, AppResources.LogIn);
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            PasswordCell.InitEvents();
            EmailCell.InitEvents();

            PasswordCell.Entry.Completed += Entry_Completed;
            MessagingCenter.Send(Application.Current, "ShowStatusBar", true);

            if(string.IsNullOrWhiteSpace(_email))
            {
                if(!string.IsNullOrWhiteSpace(EmailCell.Entry.Text))
                {
                    PasswordCell.Entry.FocusWithDelay();
                }
                else
                {
                    EmailCell.Entry.FocusWithDelay();
                }
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            PasswordCell.Dispose();
            EmailCell.Dispose();
            PasswordCell.Entry.Completed -= Entry_Completed;
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

            _userDialogs.ShowLoading(AppResources.LoggingIn, MaskType.Black);
            var result = await _authService.TokenPostAsync(EmailCell.Entry.Text, PasswordCell.Entry.Text);
            _userDialogs.HideLoading();
            if(!result.Success)
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, result.ErrorMessage, AppResources.Ok);
                return;
            }

            PasswordCell.Entry.Text = string.Empty;

            if(result.TwoFactorRequired)
            {
                _googleAnalyticsService.TrackAppEvent("LoggedIn To Two-step");
                await Navigation.PushAsync(new LoginTwoFactorPage(EmailCell.Entry.Text, result));
                return;
            }

            _googleAnalyticsService.TrackAppEvent("LoggedIn");

            if(Device.RuntimePlatform == Device.Android)
            {
                _pushNotification.Register();
            }

            var task = Task.Run(async () => await _syncService.FullSyncAsync(true));
            Application.Current.MainPage = new MainPage();
        }
    }
}
