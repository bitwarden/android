using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Services;
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
        private CancellationTokenSource _requestTimeCts;
        private Task _requestTimeTask;

        private const int REQUEST_TIME_UPDATE_PERIOD_IN_MINUTES = 5;

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

        public string LogInAttemptByLabel => LoginRequest != null ? string.Format(AppResources.LogInAttemptByXOnY, LoginRequest.Email, LoginRequest.Origin) : string.Empty;

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

        public void StopRequestTimeUpdater()
        {
            try
            {
                _requestTimeCts?.Cancel();
                _requestTimeCts?.Dispose();
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
            }
        }

        public void StartRequestTimeUpdater()
        {
            try
            {
                _requestTimeCts?.Cancel();
                _requestTimeCts = new CancellationTokenSource();
                _requestTimeTask = new TimerTask(_logger, UpdateRequestTime, _requestTimeCts).RunPeriodic(TimeSpan.FromMinutes(REQUEST_TIME_UPDATE_PERIOD_IN_MINUTES));
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
            }
        }

        private async Task UpdateRequestTime()
        {
            TriggerPropertyChanged(nameof(TimeOfRequestText));
            if (LoginRequest?.IsExpired ?? false)
            {
                StopRequestTimeUpdater();
                await _platformUtilsService.ShowDialogAsync(AppResources.LoginRequestHasAlreadyExpired);
                await Page.Navigation.PopModalAsync();
            }
        }

        private async Task PasswordlessLoginAsync(bool approveRequest)
        {
            if (LoginRequest.IsExpired)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.LoginRequestHasAlreadyExpired);
                await Page.Navigation.PopModalAsync();
                return;
            }

            var loginRequestData = await _authService.GetPasswordlessLoginRequestByIdAsync(LoginRequest.Id);
            if (loginRequestData.RequestApproved.HasValue && loginRequestData.ResponseDate.HasValue)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.ThisRequestIsNoLongerValid);
                await Page.Navigation.PopModalAsync();
                return;
            }

            await _deviceActionService.ShowLoadingAsync(AppResources.Loading);
            await _authService.PasswordlessLoginAsync(LoginRequest.Id, LoginRequest.PubKey, approveRequest);
            await _deviceActionService.HideLoadingAsync();
            await Page.Navigation.PopModalAsync();
            _platformUtilsService.ShowToast("info", null, approveRequest ? AppResources.LogInAccepted : AppResources.LogInDenied);

            StopRequestTimeUpdater();
        }

        private string CreateRequestDate(DateTime? requestDate)
        {
            if (!requestDate.HasValue)
            {
                return string.Empty;
            }

            if (DateTime.UtcNow < requestDate.Value.ToUniversalTime().AddMinutes(REQUEST_TIME_UPDATE_PERIOD_IN_MINUTES))
            {
                return AppResources.JustNow;
            }

            return string.Format(AppResources.XMinutesAgo, DateTime.UtcNow.Minute - requestDate.Value.ToUniversalTime().Minute);
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

        public bool IsExpired => RequestDate.ToUniversalTime().AddMinutes(Constants.PasswordlessNotificationTimeoutInMinutes) < DateTime.UtcNow;
    }
}
