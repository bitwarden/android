using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
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
            : base(false, false)
        {
            _checkFingerprintImmediately = checkFingerprintImmediately;
            _fingerprint = Resolver.Resolve<IFingerprint>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _settings = Resolver.Resolve<ISettings>();

            Init();
        }

        public void Init()
        {
            var fingerprintIcon = new ExtendedButton
            {
                Image = "fingerprint",
                BackgroundColor = Color.Transparent,
                Command = new Command(async () => await CheckFingerprintAsync()),
                VerticalOptions = LayoutOptions.CenterAndExpand,
                Margin = new Thickness(0, 0, 0, 15)
            };

            var fingerprintButton = new ExtendedButton
            {
                Text = AppResources.UseFingerprintToUnlock,
                Command = new Command(async () => await CheckFingerprintAsync()),
                VerticalOptions = LayoutOptions.EndAndExpand,
                Style = (Style)Application.Current.Resources["btn-primary"]
            };

            var logoutButton = new ExtendedButton
            {
                Text = AppResources.LogOut,
                Command = new Command(async () => await LogoutAsync()),
                VerticalOptions = LayoutOptions.End,
                Style = (Style)Application.Current.Resources["btn-primaryAccent"],
                BackgroundColor = Color.Transparent,
                Uppercase = false
            };

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(30, 40),
                Spacing = 10,
                Children = { fingerprintIcon, fingerprintButton, logoutButton }
            };

            Title = AppResources.VerifyFingerprint;
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
            if(!await _userDialogs.ConfirmAsync(AppResources.LogoutConfirmation, null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            MessagingCenter.Send(Application.Current, "Logout", (string)null);
        }

        public async Task CheckFingerprintAsync()
        {
            var result = await _fingerprint.AuthenticateAsync(AppResources.FingerprintDirection);
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
