using System;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class LoginPasswordlessViewModel : BaseViewModel
    {
        private IDeviceActionService _deviceActionService;
        private IAuthService _authService;
        private IPlatformUtilsService _platformUtilsService;
        private ILogger _logger;
        private LoginPasswordlessDetails _resquest;

        public LoginPasswordlessViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _authService = ServiceContainer.Resolve<IAuthService>("authService");
            _logger = ServiceContainer.Resolve<ILogger>("logger");

            PageTitle = AppResources.LogInRequested;

            AcceptRequestCommand = new AsyncCommand(() => PasswordlessLoginAsync(true),
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);
            RejectRequestCommand = new AsyncCommand(() => PasswordlessLoginAsync(false),
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);
        }

        public ICommand AcceptRequestCommand { get; }

        public ICommand RejectRequestCommand { get; }

        public string LogInAttemptByLabel => string.Format(AppResources.LogInAttemptByXOnY, LoginRequest?.Email, LoginRequest?.Origin);

        public string TimeOfRequestText => CreateRequestDate(LoginRequest?.RequestDate);

        public bool ShowIpAddress => !string.IsNullOrEmpty(LoginRequest?.IpAddress);

        public LoginPasswordlessDetails LoginRequest
        {
            get => _resquest;
            set
            {
                SetProperty(ref _resquest, value, additionalPropertyNames: new string[]
                    {
                        nameof(LogInAttemptByLabel),
                        nameof(TimeOfRequestText),
                        nameof(ShowIpAddress),
                    });
            }
        }

        private async Task PasswordlessLoginAsync(bool approveRequest)
        {
            await _deviceActionService.ShowLoadingAsync(AppResources.Loading);
            await _authService.PasswordlessLoginAsync(LoginRequest.Id, LoginRequest.PubKey, approveRequest);
            await _deviceActionService.HideLoadingAsync();
            await Page.Navigation.PopModalAsync();
            _platformUtilsService.ShowToast("info", null, approveRequest ? AppResources.LogInAccepted : AppResources.LogInDenied);
        }

        private string CreateRequestDate(DateTime? requestDate)
        {
            if (!requestDate.HasValue)
            {
                return String.Empty;
            }

            var minutesSinceRequest = requestDate.Value.ToUniversalTime().Minute - DateTime.UtcNow.Minute;
            if (minutesSinceRequest < 5)
            {
                return AppResources.JustNow;
            }
            if (minutesSinceRequest < 59)
            {
                return string.Format(AppResources.XMinutesAgo, minutesSinceRequest);
            }

            return requestDate.Value.ToShortTimeString();
        }

        private void HandleException(Exception ex)
        {
            Xamarin.Essentials.MainThread.InvokeOnMainThreadAsync(async () =>
            {
                await _deviceActionService.HideLoadingAsync();
                await _platformUtilsService.ShowDialogAsync(AppResources.GenericErrorMessage);
            }).FireAndForget();
            _logger.Exception(ex);
        }
    }

    public class LoginPasswordlessDetails
    {
        public string Id { get; set; }

        public string Key { get; set; }

        public string PubKey { get; set; }

        public string Origin { get; set; }

        public string Email { get; set; }

        public string FingerprintPhrase { get; set; }

        public DateTime RequestDate { get; set; }

        public string DeviceType { get; set; }

        public string IpAddress { get; set; }
    }
}
