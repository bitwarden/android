using System;
using System.Threading.Tasks;
using Bit.App.Controls;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Plugin.Fingerprint.Abstractions;
using Plugin.Settings.Abstractions;
using Bit.App.Abstractions;

namespace Bit.App.Pages
{
    public class LockFingerprintPage : BaseLockPage
    {
        private readonly IFingerprint _fingerprint;
        private readonly ISettings _settings;
        private readonly IAppSettingsService _appSettings;
        private readonly bool _checkFingerprintImmediately;
        private DateTime? _lastAction;

        public LockFingerprintPage(bool checkFingerprintImmediately)
        {
            _checkFingerprintImmediately = checkFingerprintImmediately;
            _fingerprint = Resolver.Resolve<IFingerprint>();
            _settings = Resolver.Resolve<ISettings>();
            _appSettings = Resolver.Resolve<IAppSettingsService>();

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

        protected override void OnAppearing()
        {
            base.OnAppearing();

            if(_checkFingerprintImmediately)
            {
                var task = CheckFingerprintAsync();
            }
        }

        public async Task CheckFingerprintAsync()
        {
            if(_lastAction.LastActionWasRecent())
            {
                return;
            }
            _lastAction = DateTime.UtcNow;

            var fingerprintRequest = new AuthenticationRequestConfiguration(AppResources.FingerprintDirection)
            {
                AllowAlternativeAuthentication = true,
                CancelTitle = AppResources.Cancel,
                FallbackTitle = AppResources.LogOut
            };
            var result = await _fingerprint.AuthenticateAsync(fingerprintRequest);
            if(result.Authenticated)
            {
                _appSettings.Locked = false;
                await Navigation.PopModalAsync();
            }
            else if(result.Status == FingerprintAuthenticationResultStatus.FallbackRequested)
            {
                MessagingCenter.Send(Application.Current, "Logout", (string)null);
            }
        }
    }
}
