using System;
using System.Threading.Tasks;
using Bit.App.Controls;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Plugin.Fingerprint.Abstractions;
using Plugin.Settings.Abstractions;
using Bit.App.Abstractions;
using Bit.App.Utilities;

namespace Bit.App.Pages
{
    public class LockFingerprintPage : BaseLockPage
    {
        private readonly IFingerprint _fingerprint;
        private readonly ISettings _settings;
        private readonly IAppSettingsService _appSettings;
        private readonly IDeviceInfoService _deviceInfoService;
        private readonly bool _checkFingerprintImmediately;
        private DateTime? _lastAction;

        public LockFingerprintPage(bool checkFingerprintImmediately)
        {
            _checkFingerprintImmediately = checkFingerprintImmediately;
            _fingerprint = Resolver.Resolve<IFingerprint>();
            _settings = Resolver.Resolve<ISettings>();
            _appSettings = Resolver.Resolve<IAppSettingsService>();
            _deviceInfoService = Resolver.Resolve<IDeviceInfoService>();

            Init();
        }

        public void Init()
        {
            var biometricIcon = Helpers.OnPlatform(
                        iOS: _deviceInfoService.HasFaceIdSupport ? "smile.png" : "fingerprint.png",
                        Android: "fingerprint.png",
                        Windows: "smile.png");
            var biometricText = Helpers.OnPlatform(
                        iOS: _deviceInfoService.HasFaceIdSupport ?
                            AppResources.UseFaceIDToUnlock : AppResources.UseFingerprintToUnlock,
                        Android: AppResources.UseFingerprintToUnlock,
                        Windows: AppResources.UseWindowsHelloToUnlock);
            var biometricTitle = Helpers.OnPlatform(
                        iOS: _deviceInfoService.HasFaceIdSupport ?
                            AppResources.VerifyFaceID : AppResources.VerifyFingerprint,
                        Android: AppResources.VerifyFingerprint,
                        Windows: AppResources.VerifyWindowsHello);


            var fingerprintIcon = new ExtendedButton
            {
                Image = biometricIcon,
                BackgroundColor = Color.Transparent,
                Command = new Command(async () => await CheckFingerprintAsync()),
                VerticalOptions = LayoutOptions.CenterAndExpand,
                Margin = new Thickness(0, 0, 0, 15)
            };

            var fingerprintButton = new ExtendedButton
            {
                Text = biometricText,
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

            Title = biometricTitle;
            Content = stackLayout;
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();

            if(_checkFingerprintImmediately)
            {
                await Task.Delay(Device.RuntimePlatform == Device.Android ? 500 : 200);
                await CheckFingerprintAsync();
            }
        }

        public async Task CheckFingerprintAsync()
        {
            if(_lastAction.LastActionWasRecent())
            {
                return;
            }
            _lastAction = DateTime.UtcNow;

            var direction = _deviceInfoService.HasFaceIdSupport ?
                AppResources.FaceIDDirection : AppResources.FingerprintDirection;

            var fingerprintRequest = new AuthenticationRequestConfiguration(direction)
            {
                AllowAlternativeAuthentication = true,
                CancelTitle = AppResources.Cancel,
                FallbackTitle = AppResources.LogOut
            };
            var result = await _fingerprint.AuthenticateAsync(fingerprintRequest);
            if(result.Authenticated)
            {
                _appSettings.Locked = false;
                if(Navigation.ModalStack.Count > 0)
                {
                    await Navigation.PopModalAsync();
                }
            }
            else if(result.Status == FingerprintAuthenticationResultStatus.FallbackRequested)
            {
                AuthService.LogOut();
            }
        }
    }
}
