using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
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
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;
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
        private IDeviceTrustCryptoService _deviceTrustCryptoService;
        private readonly ICryptoFunctionService _cryptoFunctionService;
        private readonly ICryptoService _cryptoService;

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
        private AuthRequestType _authRequestType;
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
            _deviceTrustCryptoService = ServiceContainer.Resolve<IDeviceTrustCryptoService>();
            _cryptoFunctionService = ServiceContainer.Resolve<ICryptoFunctionService>();
            _cryptoService = ServiceContainer.Resolve<ICryptoService>();

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
        public bool AuthingWithSso { get; set; }

        public ICommand CreatePasswordlessLoginCommand { get; }
        public ICommand CloseCommand { get; }

        public string HeaderTitle
        {
            get
            {
                switch (_authRequestType)
                {
                    case AuthRequestType.AuthenticateAndUnlock:
                        return AppResources.LogInWithDevice;
                    case AuthRequestType.AdminApproval:
                        return AppResources.LogInInitiated;
                    default:
                        return string.Empty;
                };
            }
        }

        public string Title
        {
            get
            {
                switch (_authRequestType)
                {
                    case AuthRequestType.AuthenticateAndUnlock:
                        return AppResources.LogInInitiated;
                    case AuthRequestType.AdminApproval:
                        return AppResources.AdminApprovalRequested;
                    default:
                        return string.Empty;
                };
            }
        }

        public string SubTitle
        {
            get
            {
                switch (_authRequestType)
                {
                    case AuthRequestType.AuthenticateAndUnlock:
                        return AppResources.ANotificationHasBeenSentToYourDevice;
                    case AuthRequestType.AdminApproval:
                        return AppResources.YourRequestHasBeenSentToYourAdmin;
                    default:
                        return string.Empty;
                };
            }
        }

        public string Description
        {
            get
            {
                switch (_authRequestType)
                {
                    case AuthRequestType.AuthenticateAndUnlock:
                        return AppResources.PleaseMakeSureYourVaultIsUnlockedAndTheFingerprintPhraseMatchesOnTheOtherDevice;
                    case AuthRequestType.AdminApproval:
                        return AppResources.YouWillBeNotifiedOnceApproved;
                    default:
                        return string.Empty;
                };
            }
        }

        public string OtherOptions
        {
            get
            {
                switch (_authRequestType)
                {
                    case AuthRequestType.AuthenticateAndUnlock:
                        return AppResources.LogInWithDeviceMustBeSetUpInTheSettingsOfTheBitwardenAppNeedAnotherOption;
                    case AuthRequestType.AdminApproval:
                        return AppResources.TroubleLoggingIn;
                    default:
                        return string.Empty;
                };
            }
        }

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

        public AuthRequestType AuthRequestType
        {
            get => _authRequestType;
            set
            {
                SetProperty(ref _authRequestType, value, additionalPropertyNames: new string[]
                {
                    nameof(Title),
                    nameof(SubTitle),
                    nameof(Description),
                    nameof(OtherOptions),
                    nameof(ResendNotificationVisible)
                });
                PageTitle = HeaderTitle;
            }
        }

        public bool ResendNotificationVisible => AuthRequestType == AuthRequestType.AuthenticateAndUnlock;

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
            if (string.IsNullOrEmpty(_requestId))
            {
                return;
            }

            try
            {
                PasswordlessLoginResponse response = null;
                if (AuthingWithSso)
                {
                    response = await _authService.GetPasswordlessLoginRequestByIdAsync(_requestId);
                }
                else
                {
                    response = await _authService.GetPasswordlessLoginResquestAsync(_requestId, _requestAccessCode);
                }

                if (response?.RequestApproved != true)
                {
                    return;
                }

                StopCheckLoginRequestStatus();

                var authResult = await _authService.LogInPasswordlessAsync(AuthingWithSso, Email, _requestAccessCode, _requestId, _requestKeyPair.Item2, response.Key, response.MasterPasswordHash);
                await AppHelpers.ResetInvalidUnlockAttemptsAsync();

                if (authResult == null && await _stateService.IsAuthenticatedAsync())
                {
                    await HandleLoginCompleteAsync();
                    return;
                }

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
                    await HandleLoginCompleteAsync();
                }
            }
            catch (ApiException ex) when (ex.Error?.StatusCode == System.Net.HttpStatusCode.BadRequest)
            {
                HandleException(ex);
            }
            catch (Exception ex)
            {
                StartCheckLoginRequestStatus();
                HandleException(ex);
            }
        }

        private async Task HandleLoginCompleteAsync()
        {
            await _stateService.SetPendingAdminAuthRequestAsync(null);
            _syncService.FullSyncAsync(true).FireAndForget();
            LogInSuccessAction?.Invoke();
        }

        private async Task CreatePasswordlessLoginAsync()
        {
            await Device.InvokeOnMainThreadAsync(() => _deviceActionService.ShowLoadingAsync(AppResources.Loading));

            PasswordlessLoginResponse response = null;
            var pendingRequest = await _stateService.GetPendingAdminAuthRequestAsync();
            if (pendingRequest != null && _authRequestType == AuthRequestType.AdminApproval)
            {
                response = await _authService.GetPasswordlessLoginRequestByIdAsync(pendingRequest.Id);
                if (response == null || (response.IsAnswered && !response.RequestApproved.Value))
                {
                    // handle pending auth request not valid remove it from state
                    await _stateService.SetPendingAdminAuthRequestAsync(null);
                    pendingRequest = null;
                    response = null;
                }
                else
                {
                    // Derive pubKey from privKey in state to avoid MITM attacks
                    // Also generate FingerprintPhrase locally for the same reason
                    var derivedPublicKey = await _cryptoFunctionService.RsaExtractPublicKeyAsync(pendingRequest.PrivateKey);
                    response.FingerprintPhrase = string.Join("-", await _cryptoService.GetFingerprintAsync(Email, derivedPublicKey));
                    response.RequestKeyPair = new Tuple<byte[], byte[]>(derivedPublicKey, pendingRequest.PrivateKey);
                }
            }

            if (response == null)
            {
                response = await _authService.PasswordlessCreateLoginRequestAsync(_email, AuthRequestType);
            }

            await HandlePasswordlessLoginAsync(response, pendingRequest == null && _authRequestType == AuthRequestType.AdminApproval);
            await _deviceActionService.HideLoadingAsync();
        }

        private async Task HandlePasswordlessLoginAsync(PasswordlessLoginResponse response, bool createPendingAdminRequest)
        {
            if (response == null)
            {
                throw new ArgumentNullException(nameof(response));
            }

            if (createPendingAdminRequest)
            {
                var pendingAuthRequest = new PendingAdminAuthRequest { Id = response.Id, PrivateKey = response.RequestKeyPair.Item2 };
                await _stateService.SetPendingAdminAuthRequestAsync(pendingAuthRequest);
            }

            FingerprintPhrase = response.FingerprintPhrase;
            _requestId = response.Id;
            _requestAccessCode = response.RequestAccessCode;
            _requestKeyPair = response.RequestKeyPair;
        }
    }
}

