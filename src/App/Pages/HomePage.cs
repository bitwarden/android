using System;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Plugin.Settings.Abstractions;
using Bit.App.Controls;
using FFImageLoading.Forms;

namespace Bit.App.Pages
{
    public class HomePage : ExtendedContentPage
    {
        private readonly IAuthService _authService;
        private readonly ISettings _settings;
        private readonly IDeviceActionService _deviceActionService;
        private DateTime? _lastAction;

        public HomePage()
            : base(updateActivity: false)
        {
            _authService = Resolver.Resolve<IAuthService>();
            _deviceActionService = Resolver.Resolve<IDeviceActionService>();
            _settings = Resolver.Resolve<ISettings>();

            Init();
        }

        public void Init()
        {
            MessagingCenter.Send(Application.Current, "ShowStatusBar", false);

            var settingsButton = new ExtendedButton
            {
                Image = "cog.png",
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Start,
                WidthRequest = 25,
                HeightRequest = 25,
                BackgroundColor = Color.Transparent,
                Margin = new Thickness(-20, -30, 0, 0),
                Command = new Command(async () => await SettingsAsync())
            };

            var logo = new CachedImage
            {
                Source = "logo.png",
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center,
                WidthRequest = 282,
                Margin = new Thickness(0, 30, 0, 0),
                HeightRequest = 44
            };

            var message = new Label
            {
                Text = AppResources.LoginOrCreateNewAccount,
                VerticalOptions = LayoutOptions.StartAndExpand,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Large, typeof(Label)),
                TextColor = Color.FromHex("333333")
            };

            var createAccountButton = new ExtendedButton
            {
                Text = AppResources.CreateAccount,
                Command = new Command(async () => await RegisterAsync()),
                VerticalOptions = LayoutOptions.End,
                HorizontalOptions = LayoutOptions.Fill,
                Style = (Style)Application.Current.Resources["btn-primary"],
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Button))
            };

            var loginButton = new ExtendedButton
            {
                Text = AppResources.LogIn,
                Command = new Command(async () => await LoginAsync()),
                VerticalOptions = LayoutOptions.End,
                Style = (Style)Application.Current.Resources["btn-primaryAccent"],
                HorizontalOptions = LayoutOptions.Fill,
                BackgroundColor = Color.Transparent,
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Button))
            };

            var buttonStackLayout = new StackLayout
            {
                Padding = new Thickness(30, 40),
                Spacing = 10,
                Children = { settingsButton, logo, message, createAccountButton, loginButton }
            };

            Title = AppResources.Bitwarden;
            NavigationPage.SetHasNavigationBar(this, false);
            Content = new ScrollView { Content = buttonStackLayout };
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            MessagingCenter.Send(Application.Current, "ShowStatusBar", false);
        }

        public async Task LoginAsync()
        {
            if(_lastAction.LastActionWasRecent())
            {
                return;
            }
            _lastAction = DateTime.UtcNow;

            await Navigation.PushForDeviceAsync(new LoginPage());
        }

        public async Task RegisterAsync()
        {
            if(_lastAction.LastActionWasRecent())
            {
                return;
            }
            _lastAction = DateTime.UtcNow;

            await Navigation.PushForDeviceAsync(new RegisterPage(this));
        }

        public async Task DismissRegisterAndLoginAsync(string email)
        {
            await Navigation.PopForDeviceAsync();
            await Navigation.PushForDeviceAsync(new LoginPage(email));
            _deviceActionService.Toast(AppResources.AccountCreated);
        }

        public async Task SettingsAsync()
        {
            if(_lastAction.LastActionWasRecent())
            {
                return;
            }
            _lastAction = DateTime.UtcNow;

            await Navigation.PushForDeviceAsync(new EnvironmentPage());
        }
    }
}
