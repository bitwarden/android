using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Plugin.Connectivity.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;
using Plugin.Fingerprint.Abstractions;
using Plugin.Settings.Abstractions;

namespace Bit.App.Pages
{
    public class LockFingerprintPage : ContentPage
    {
        private readonly IFingerprint _fingerprint;
        private readonly IAuthService _authService;
        private readonly IUserDialogs _userDialogs;
        private readonly ISettings _settings;
        private readonly bool _checkFingerprintImmediately;

        public LockFingerprintPage(bool checkFingerprintImmediately)
        {
            _checkFingerprintImmediately = checkFingerprintImmediately;
            _fingerprint = Resolver.Resolve<IFingerprint>();
            _authService = Resolver.Resolve<IAuthService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _settings = Resolver.Resolve<ISettings>();

            Init();
        }

        public void Init()
        {
            var fingerprintButton = new Button
            {
                Text = "Use Fingerprint to Unlock",
                Command = new Command(async () => await CheckFingerprintAsync()),
                VerticalOptions = LayoutOptions.EndAndExpand,
                Style = (Style)Application.Current.Resources["btn-default"]
            };

            var logoutButton = new Button
            {
                Text = AppResources.LogOut,
                Command = new Command(async () => await LogoutAsync()),
                VerticalOptions = LayoutOptions.End,
                Style = (Style)Application.Current.Resources["btn-default"]
            };

            var stackLayout = new StackLayout { Padding = new Thickness(30, 40), Spacing = 10 };
            stackLayout.Children.Add(fingerprintButton);
            stackLayout.Children.Add(logoutButton);

            Title = "Verify Fingerprint";
            Content = stackLayout;
            BackgroundImage = "bg.png";
        }

        protected override bool OnBackButtonPressed()
        {
            return false;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();

            if(_checkFingerprintImmediately)
            {
                CheckFingerprintAsync();
            }
        }

        public async Task LogoutAsync()
        {
            if(!await _userDialogs.ConfirmAsync("Are you sure you want to log out?", null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            _authService.LogOut();
            await Navigation.PopModalAsync();
            Application.Current.MainPage = new LoginNavigationPage();
        }

        public async Task CheckFingerprintAsync()
        {
            var result = await _fingerprint.AuthenticateAsync("Use your fingerprint to verify.");
            if(result.Authenticated)
            {
                await Navigation.PopModalAsync();
            }
        }
    }
}
