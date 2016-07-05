using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Plugin.Settings.Abstractions;
using Bit.App.Controls;

namespace Bit.App.Pages
{
    public class HomePage : ExtendedContentPage
    {
        private readonly IAuthService _authService;
        private readonly IUserDialogs _userDialogs;
        private readonly ISettings _settings;

        public HomePage()
        {
            _authService = Resolver.Resolve<IAuthService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _settings = Resolver.Resolve<ISettings>();

            Init();
        }

        public void Init()
        {
            var logo = new Image
            {
                Source = "logo",
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center
            };

            var message = new Label
            {
                Text = "Log in or create a new account to access your secure vault.",
                VerticalOptions = LayoutOptions.StartAndExpand,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Large, typeof(Label)),
                TextColor = Color.FromHex("333333")
            };

            var createAccountButton = new Button
            {
                Text = "Create Account",
                Command = new Command(async () => await RegisterAsync()),
                VerticalOptions = LayoutOptions.End,
                HorizontalOptions = LayoutOptions.Fill,
                Style = (Style)Application.Current.Resources["btn-primary"]
            };

            var loginButton = new Button
            {
                Text = AppResources.LogIn,
                Command = new Command(async () => await LoginAsync()),
                VerticalOptions = LayoutOptions.End,
                Style = (Style)Application.Current.Resources["btn-primaryAccent"],
                HorizontalOptions = LayoutOptions.Fill
            };

            var buttonStackLayout = new StackLayout
            {
                Padding = new Thickness(30, 40),
                Spacing = 10,
                Children = { logo, message, createAccountButton, loginButton }
            };

            Title = "bitwarden";
            Content = new ScrollView { Content = buttonStackLayout };
        }

        public async Task LoginAsync()
        {
            await Navigation.PushModalAsync(new ExtendedNavigationPage(new LoginPage()));
        }

        public async Task RegisterAsync()
        {
            await Navigation.PushModalAsync(new ExtendedNavigationPage(new RegisterPage()));
        }
    }
}
