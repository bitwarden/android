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
        private IDeviceActionService _deviceActionService;
        private IAuthService _authService;
        private ISyncService _syncService;
        private II18nService _i18nService;
        private IStateService _stateService;
        private ICryptoFunctionService _cryptoFunctionService;
        private ICryptoService _cryptoService;
        private IPasswordGenerationService _passwordGenerationService;
        private IPlatformUtilsService _platformUtilsService;
        private IEnvironmentService _environmentService;
        private ILogger _logger;
        private CancellationTokenSource _checkLoginRequestStatusCts;
        private Task _checkLoginRequestStatusTask;
        private const int REQUEST_TIME_UPDATE_PERIOD_IN_SECONDS = 4;
        private string _fingerprintPhrase;
        private string _email;
        private string _requestId;
        private string _requestAccessCode;
        private Tuple<byte[], byte[]> _requestKeyPair;
        public Action StartTwoFactorAction { get; set; }
        public Action LogInSuccessAction { get; set; }
        public Action UpdateTempPasswordAction { get; set; }
        public Action CloseAction { get; set; }

        public LoginPasswordlessRequestViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>();
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>();
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>();
            _authService = ServiceContainer.Resolve<IAuthService>();
            _syncService = ServiceContainer.Resolve<ISyncService>();
            _i18nService = ServiceContainer.Resolve<II18nService>();
            _stateService = ServiceContainer.Resolve<IStateService>();
            _cryptoFunctionService = ServiceContainer.Resolve<ICryptoFunctionService>();
            _cryptoService = ServiceContainer.Resolve<ICryptoService>();
            _passwordGenerationService = ServiceContainer.Resolve<IPasswordGenerationService>();
            _logger = ServiceContainer.Resolve<ILogger>("logger");

            PageTitle = AppResources.LogInWithAnotherDevice;

            CreatePasswordlessLoginCommand = new AsyncCommand(async () => await Device.InvokeOnMainThreadAsync(CreatePasswordlessLoginAsync),
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);
        }

        public ICommand CreatePasswordlessLoginCommand { get; }

        public string FingerprintPhrase
        {
            get => _fingerprintPhrase;
            set
            {
                SetProperty(ref _fingerprintPhrase, value);
            }
        }

        public string Email
        {
            get => _email;
            set
            {
                SetProperty(ref _email, value);
            }
        }

        protected override II18nService i18nService => _i18nService;
        protected override IEnvironmentService environmentService => _environmentService;
        protected override IDeviceActionService deviceActionService => _deviceActionService;
        protected override IPlatformUtilsService platformUtilsService => _platformUtilsService;

        public void StopCheckLoginRequestStatus()
        {
            try
            {
                _checkLoginRequestStatusCts?.Cancel();
                _checkLoginRequestStatusCts?.Dispose();
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
            }
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

        private async Task CheckLoginRequestStatus()
        {
            if (string.IsNullOrEmpty(_requestId) || string.IsNullOrEmpty(_requestAccessCode))
            {
                return;
            }

            try
            {
                var response = await _authService.GetPasswordlessLoginResponse(_requestId, _requestAccessCode);

                if (!response.RequestApproved)
                {
                    return;
                }

                StopCheckLoginRequestStatus();

                var decKey = await _cryptoFunctionService.RsaDecryptAsync(response.Key, _requestKeyPair.Item2);
                var decPasswordHash = await _cryptoFunctionService.RsaDecryptAsync(response.MasterPasswordHash, _requestKeyPair.Item2);

                var authResult = await _authService.LogInPasswordlessAsync(Email, _requestAccessCode, _requestId, decKey, decPasswordHash);
                await AppHelpers.ResetInvalidUnlockAttemptsAsync();

                if (authResult.CaptchaNeeded)
                {
                    if (await HandleCaptchaAsync(authResult.CaptchaSiteKey))
                    {
                        await CheckLoginRequestStatus();
                        _captchaToken = null;
                    }
                    return;
                }
                _captchaToken = null;

                await _deviceActionService.HideLoadingAsync();

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
                    var task = Task.Run(async () => await _syncService.FullSyncAsync(true));
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
            await _deviceActionService.ShowLoadingAsync(AppResources.Loading);

            var keyPair = await _cryptoFunctionService.RsaGenerateKeyPairAsync(2048);
            var generatedFingerprintPhrase = await _cryptoService.GetFingerprintAsync(Email, keyPair.Item1);
            var fingerprintPhrase = string.Join("-", generatedFingerprintPhrase);
            var publicB64 = Convert.ToBase64String(keyPair.Item1);
            var accessCode = await _passwordGenerationService.GeneratePasswordAsync(new PasswordGenerationOptions(true) { Length = 25 });
            var response = await _authService.PasswordlessCreateLoginRequestAsync(_email, publicB64, accessCode, fingerprintPhrase);

            if (response != null)
            {
                FingerprintPhrase = fingerprintPhrase;
                _requestAccessCode = accessCode;
                _requestId = response.Id;
                _requestKeyPair = keyPair;
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

