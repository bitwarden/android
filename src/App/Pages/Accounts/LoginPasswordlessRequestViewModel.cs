﻿using System;
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
        private IDeviceTrustCryptoService _deviceTrustCryptoService;

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

        public string Tittle
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

        public string SubTittle
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
                        return AppResources.NeedAnotherOption;
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
            set => SetProperty(ref _authRequestType, value, additionalPropertyNames: new string[]
            {
                nameof(Tittle),
                nameof(SubTittle),
                nameof(Description),
                nameof(OtherOptions),
                nameof(ResendNotificationVisible)
            });
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

                if(authResult == null && await _stateService.IsAuthenticatedAsync())
                {
                    _syncService.FullSyncAsync(true).FireAndForget();
                    LogInSuccessAction?.Invoke();
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

            var response = await _authService.PasswordlessCreateLoginRequestAsync(_email, AuthRequestType);
            if (response != null)
            {
                //TODO TDE if is admin type save to memory to later see if it was approved
                /*
                  const adminAuthReqStorable = new AdminAuthRequestStorable({
                      id: reqResponse.id,
                      privateKey: this.authRequestKeyPair.privateKey,
                    });

                    await this.stateService.setAdminAuthRequest(adminAuthReqStorable);
                */
                FingerprintPhrase = response.FingerprintPhrase;
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

