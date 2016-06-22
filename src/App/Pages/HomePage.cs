using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Plugin.Settings.Abstractions;

namespace Bit.App.Pages
{
    public class HomePage : ContentPage
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
                Text = "Welcome!",
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center
            };

            var createAccountButton = new Button
            {
                Text = "Create Account",
                //Command = new Command(async () => await LogoutAsync()),
                VerticalOptions = LayoutOptions.End,
                HorizontalOptions = LayoutOptions.CenterAndExpand,
                Style = (Style)Application.Current.Resources["btn-default"]
            };

            var loginButton = new Button
            {
                Text = AppResources.LogIn,
                Command = new Command(async () => await LoginAsync()),
                VerticalOptions = LayoutOptions.End,
                HorizontalOptions = LayoutOptions.CenterAndExpand,
                Style = (Style)Application.Current.Resources["btn-default"]
            };

            var buttonStackLayout = new StackLayout
            {
                Padding = new Thickness(30, 40),
                Spacing = 10,
                Children = { logo, message, createAccountButton, loginButton }
            };

            Title = "bitwarden";
            Content = buttonStackLayout;
            BackgroundImage = "bg.png";
        }

        public async Task LoginAsync()
        {
            await Navigation.PushModalAsync(new LoginNavigationPage());
        }
    }
}
