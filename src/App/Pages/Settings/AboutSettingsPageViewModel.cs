using System;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Essentials;

namespace Bit.App.Pages
{
    public class AboutSettingsPageViewModel : BaseViewModel
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IDeviceActionService _deviceActionService;

        public AboutSettingsPageViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>();
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>();
            var environmentService = ServiceContainer.Resolve<IEnvironmentService>();

            GoToHelpCenterCommand = CreateDefaultAsyncCommnad(
                () => MainThread.InvokeOnMainThreadAsync(() => _platformUtilsService.LaunchUri(ExternalLinksConstants.HELP_CENTER)));

            ContactBitwardenSupportCommand = CreateDefaultAsyncCommnad(
                () => MainThread.InvokeOnMainThreadAsync(() => _platformUtilsService.LaunchUri(ExternalLinksConstants.CONTACT_SUPPORT)));

            GoToWebVaultCommand = CreateDefaultAsyncCommnad(
                () => MainThread.InvokeOnMainThreadAsync(
                    () => _platformUtilsService.LaunchUri(environmentService.GetWebVaultUrl())));

            GoToLearnAboutOrgsCommand = CreateDefaultAsyncCommnad(
                () => MainThread.InvokeOnMainThreadAsync(() => _platformUtilsService.LaunchUri(ExternalLinksConstants.HELP_ABOUT_ORGANIZATIONS)));

            RateTheAppCommand = CreateDefaultAsyncCommnad(
                () => MainThread.InvokeOnMainThreadAsync(() => _deviceActionService.RateApp()));
        }

        public string AppInfo
        {
            get
            {
                var appInfo = string.Format("{0}: {1} ({2})",
                    AppResources.Version,
                    _platformUtilsService.GetApplicationVersion(),
                    _deviceActionService.GetBuildNumber());

                return $"© Bitwarden Inc. 2015-{DateTime.Now.Year}\n\n{appInfo}";
            }
        }

        public ICommand GoToHelpCenterCommand { get; }
        public ICommand ContactBitwardenSupportCommand { get; }
        public ICommand GoToWebVaultCommand { get; }
        public ICommand GoToLearnAboutOrgsCommand { get; }
        public ICommand RateTheAppCommand { get; }

        /// INFO: Left here in case we need to debug push notifications
        /// <summary>
        /// Sets up app info plus debugging information for push notifications.
        /// Useful when trying to solve problems regarding push notifications.
        /// </summary>
        /// <example>
        /// Add an IniAsync() method to be called on view appearing, change the AppInfo to be a normal property with setter
        /// and set the result of this method in the main thread to that property to show that in the UI.
        /// </example>
//        public async Task<string> GetAppInfoForPushNotificationsDebugAsync()
//        {
//            var stateService = ServiceContainer.Resolve<IStateService>();

//            var appInfo = string.Format("{0}: {1} ({2})", AppResources.Version,
//                _platformUtilsService.GetApplicationVersion(), _deviceActionService.GetBuildNumber());

//#if DEBUG
//            var pushNotificationsRegistered = ServiceContainer.Resolve<IPushNotificationService>("pushNotificationService").IsRegisteredForPush;
//            var pnServerRegDate = await stateService.GetPushLastRegistrationDateAsync();
//            var pnServerError = await stateService.GetPushInstallationRegistrationErrorAsync();

//            var pnServerRegDateMessage = default(DateTime) == pnServerRegDate ? "-" : $"{pnServerRegDate.GetValueOrDefault().ToShortDateString()}-{pnServerRegDate.GetValueOrDefault().ToShortTimeString()} UTC";
//            var errorMessage = string.IsNullOrEmpty(pnServerError) ? string.Empty : $"Push Notifications Server Registration error: {pnServerError}";

//            var text = string.Format("© Bitwarden Inc. 2015-{0}\n\n{1}\nPush Notifications registered:{2}\nPush Notifications Server Last Date :{3}\n{4}", DateTime.Now.Year, appInfo, pushNotificationsRegistered, pnServerRegDateMessage, errorMessage);
//#else
//            var text = string.Format("© Bitwarden Inc. 2015-{0}\n\n{1}", DateTime.Now.Year, appInfo);
//#endif
//            return text;
//        }
    }
}
