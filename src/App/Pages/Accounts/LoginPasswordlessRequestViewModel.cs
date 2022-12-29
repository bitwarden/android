using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class LoginPasswordlessRequestViewModel : CaptchaProtectedViewModel
    {
        private const int REQUEST_TIME_UPDATE_PERIOD_IN_SECONDS = 4;

        private IDeviceActionService _deviceActionService;
        private IAuthService _authService;
        private ISyncService _syncService;
        private II18nService _i18nService;
        private IStateService _stateService;
        private IPlatformUtilsService _platformUtilsService;
        private IEnvironmentService _environmentService;
        private ILogger _logger;

        protected override II18nService i18nService => _i18nService;
        protected override IEnvironmentService environmentService => _environmentService;
        protected override IDeviceActionService deviceActionService => _deviceActionService;
        protected override IPlatformUtilsService platformUtilsService => _platformUtilsService;

        private CancellationTokenSource _checkLoginRequestStatusCts;
        private Task _checkLoginRequestStatusTask;
        private string _fingerprintPhrase;
        private string _email;
        private string _requestId;
        private string _requestAccessCode;
        // Item1 publicKey, Item2 privateKey
        private Tuple<byte[], byte[]> _requestKeyPair;

        public LoginPasswordlessRequestViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>();
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>();
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>();
            _authService = ServiceContainer.Resolve<IAuthService>();
            _syncService = ServiceContainer.Resolve<ISyncService>();
            _i18nService = ServiceContainer.Resolve<II18nService>();
            _stateService = ServiceContainer.Resolve<IStateService>();
            _logger = ServiceContainer.Resolve<ILogger>();

            PageTitle = AppResources.LogInWithAnotherDevice;

            CreatePasswordlessLoginCommand = new AsyncCommand(CreatePasswordlessLoginAsync,
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);

            CloseCommand = new AsyncCommand(() => Device.InvokeOnMainThreadAsync(CloseAction),
                onException: _logger.Exception,
                allowsMultipleExecutions: false);
        }

        public Action StartTwoFactorAction { get; set; }
        public Action LogInSuccessAction { get; set; }
        public Action UpdateTempPasswordAction { get; set; }
        public Action CloseAction { get; set; }

        public ICommand CreatePasswordlessLoginCommand { get; }
        public ICommand CloseCommand { get; }

        public string FingerprintPhrase
        {
            get => _fingerprintPhrase;
            set => SetProperty(ref _fingerprintPhrase, value);
        }

        public string Email
        {
            get => _email;
            set => SetProperty(ref _email, value);
        }

        public void StartCheckLoginRequestStatus()
        {
            try
            {
                _checkLoginRequestStatusCts?.Cancel();
                _checkLoginRequestStatusCts = new CancellationTokenSource();
                _checkLoginRequestStatusTask = new TimerTask(_logger, CheckLoginRequestStatus, _checkLoginRequestStatusCts).RunPeriodic(TimeSpan.FromSeconds(REQUEST_TIME_UPDATE_PERIOD_IN_SECONDS));
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
            }
        }

        public void StopCheckLoginRequestStatus()
        {
            try
            {
                _checkLoginRequestStatusCts?.Cancel();
                _checkLoginRequestStatusCts?.Dispose();
                _checkLoginRequestStatusCts = null;
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
            }
        }

        private async Task CheckLoginRequestStatus()
        {
            if (string.IsNullOrEmpty(_requestId) || string.IsNullOrEmpty(_requestAccessCode))
            {
                return;
            }

            try
            {
                var response = await _authService.GetPasswordlessLoginResponseAsync(_requestId, _requestAccessCode);

                if (response.RequestApproved == null || !response.RequestApproved.Value)
                {
                    return;
                }

                StopCheckLoginRequestStatus();

                var authResult = await _authService.LogInPasswordlessAsync(Email, _requestAccessCode, _requestId, _requestKeyPair.Item2, response.Key, response.MasterPasswordHash);
                await AppHelpers.ResetInvalidUnlockAttemptsAsync();

                if (await HandleCaptchaAsync(authResult.CaptchaSiteKey, authResult.CaptchaNeeded, CheckLoginRequestStatus))
                {
                    return;
                }

                if (authResult.TwoFactor)
                {
                    StartTwoFactorAction?.Invoke();
                }
                else if (authResult.ForcePasswordReset)
                {
                    UpdateTempPasswordAction?.Invoke();
                }
                else
                {
                    _syncService.FullSyncAsync(true).FireAndForget();
                    LogInSuccessAction?.Invoke();
                }
            }
            catch (Exception ex)
            {
                StartCheckLoginRequestStatus();
                HandleException(ex);
            }
        }

        private async Task CreatePasswordlessLoginAsync()
        {
            await Device.InvokeOnMainThreadAsync(() => _deviceActionService.ShowLoadingAsync(AppResources.Loading));

            var response = await _authService.PasswordlessCreateLoginRequestAsync(_email);
            if (response != null)
            {
                FingerprintPhrase = response.RequestFingerprint;
                _requestId = response.Id;
                _requestAccessCode = response.RequestAccessCode;
                _requestKeyPair = response.RequestKeyPair;
            }

            await _deviceActionService.HideLoadingAsync();
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
}

