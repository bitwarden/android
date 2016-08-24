using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Plugin.Fingerprint.Abstractions;
using Plugin.Settings.Abstractions;

namespace Bit.App.Pages
{
    public class LockFingerprintPage : ExtendedContentPage
    {
        private readonly IFingerprint _fingerprint;
        private readonly IUserDialogs _userDialogs;
        private readonly ISettings _settings;
        private readonly bool _checkFingerprintImmediately;

        public LockFingerprintPage(bool checkFingerprintImmediately)
            : base(false)
        {
            _checkFingerprintImmediately = checkFingerprintImmediately;
            _fingerprint = Resolver.Resolve<IFingerprint>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _settings = Resolver.Resolve<ISettings>();

            Init();
        }

        public void Init()
        {
            var fingerprintIcon = new Button
            {
                Image = "fingerprint",
                BackgroundColor = Color.Transparent,
                Command = new Command(async () => await CheckFingerprintAsync()),
                VerticalOptions = LayoutOptions.CenterAndExpand,
                Margin = new Thickness(0, 0, 0, 15)
            };

            var fingerprintButton = new Button
            {
                Text = "Use Fingerprint to Unlock",
                Command = new Command(async () => await CheckFingerprintAsync()),
                VerticalOptions = LayoutOptions.EndAndExpand,
                Style = (Style)Application.Current.Resources["btn-primary"]
            };

            var logoutButton = new Button
            {
                Text = AppResources.LogOut,
                Command = new Command(async () => await LogoutAsync()),
                VerticalOptions = LayoutOptions.End,
                Style = (Style)Application.Current.Resources["btn-primaryAccent"],
                BackgroundColor = Color.Transparent
            };

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(30, 40),
                Spacing = 10,
                Children = { fingerprintIcon, fingerprintButton, logoutButton }
            };

            Title = "Verify Fingerprint";
            Content = stackLayout;
        }

        protected override bool OnBackButtonPressed()
        {
            return true;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();

            if(_checkFingerprintImmediately)
            {
                var task = CheckFingerprintAsync();
            }
        }

        public async Task LogoutAsync()
        {
            if(!await _userDialogs.ConfirmAsync("Are you sure you want to log out?", null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            MessagingCenter.Send(Application.Current, "Logout", (string)null);
        }

        public async Task CheckFingerprintAsync()
        {
            var result = await _fingerprint.AuthenticateAsync("Use your fingerprint to verify.");
            if(result.Authenticated)
            {
                _settings.AddOrUpdateValue(Constants.Locked, false);
                await Navigation.PopModalAsync();
            }
            else if(result.Status == FingerprintAuthenticationResultStatus.FallbackRequested)
            {
                MessagingCenter.Send(Application.Current, "Logout", (string)null);
            }
        }
    }
}
