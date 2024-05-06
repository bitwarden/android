using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Pages.Accounts;
using Bit.Core.Resources.Localization;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;
using CommunityToolkit.Mvvm.Input;

namespace Bit.App.Pages
{
    public class SecuritySettingsPageViewModel : BaseViewModel
    {
        private const int NEVER_SESSION_TIMEOUT_VALUE = -2;
        private const int CUSTOM_VAULT_TIMEOUT_VALUE = -100;

        private readonly IStateService _stateService;
        private readonly IPushNotificationService _pushNotificationService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly IBiometricService _biometricsService;
        private readonly IUserPinService _userPinService;
        private readonly ICryptoService _cryptoService;
        private readonly IUserVerificationService _userVerificationService;
        private readonly IPolicyService _policyService;
        private readonly IMessagingService _messagingService;
        private readonly IEnvironmentService _environmentService;
        private readonly ILogger _logger;

        private bool _inited;
        private bool _useThisDeviceToApproveLoginRequests;
        private bool _supportsBiometric, _canUnlockWithBiometrics;
        private bool _canUnlockWithPin;
        private bool _hasMasterPassword;
        private int? _maximumVaultTimeoutPolicy;
        private string _vaultTimeoutActionPolicy;
        private TimeSpan? _customVaultTimeoutTime;

        public SecuritySettingsPageViewModel()
        {
            _stateService = ServiceContainer.Resolve<IStateService>();
            _pushNotificationService = ServiceContainer.Resolve<IPushNotificationService>();
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>();
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>();
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>();
            _biometricsService = ServiceContainer.Resolve<IBiometricService>();
            _userPinService = ServiceContainer.Resolve<IUserPinService>();
            _cryptoService = ServiceContainer.Resolve<ICryptoService>();
            _userVerificationService = ServiceContainer.Resolve<IUserVerificationService>();
            _policyService = ServiceContainer.Resolve<IPolicyService>();
            _messagingService = ServiceContainer.Resolve<IMessagingService>();
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>();
            _logger = ServiceContainer.Resolve<ILogger>();

            VaultTimeoutPickerViewModel = new PickerViewModel<int>(
                _deviceActionService,
                _logger,
                OnVaultTimeoutChangingAsync,
                AppResources.SessionTimeout,
                () => _inited,
                ex => HandleException(ex));
            VaultTimeoutPickerViewModel.SetAfterSelectionChanged(_ => MainThread.InvokeOnMainThreadAsync(TriggerUpdateCustomVaultTimeoutPicker));

            VaultTimeoutActionPickerViewModel = new PickerViewModel<VaultTimeoutAction>(
                _deviceActionService,
                _logger,
                OnVaultTimeoutActionChangingAsync,
                AppResources.SessionTimeoutAction,
                () => _inited && !HasVaultTimeoutActionPolicy && IsVaultTimeoutActionLockAllowed,
                ex => HandleException(ex));

            ToggleUseThisDeviceToApproveLoginRequestsCommand = CreateDefaultAsyncRelayCommand(ToggleUseThisDeviceToApproveLoginRequestsAsync, () => _inited, allowsMultipleExecutions: false);
            GoToPendingLogInRequestsCommand = CreateDefaultAsyncRelayCommand(() => Page.Navigation.PushModalAsync(new NavigationPage(new LoginPasswordlessRequestsListPage())), allowsMultipleExecutions: false);
            ToggleCanUnlockWithBiometricsCommand = CreateDefaultAsyncRelayCommand(ToggleCanUnlockWithBiometricsAsync, () => _inited, allowsMultipleExecutions: false);
            ToggleCanUnlockWithPinCommand = CreateDefaultAsyncRelayCommand(ToggleCanUnlockWithPinAsync, () => _inited, allowsMultipleExecutions: false);
            ShowAccountFingerprintPhraseCommand = CreateDefaultAsyncRelayCommand(ShowAccountFingerprintPhraseAsync, allowsMultipleExecutions: false);
            GoToTwoStepLoginCommand = CreateDefaultAsyncRelayCommand(() => GoToWebVaultSettingsAsync(AppResources.TwoStepLoginDescriptionLong, AppResources.ContinueToWebApp), allowsMultipleExecutions: false);
            GoToChangeMasterPasswordCommand = CreateDefaultAsyncRelayCommand(() => GoToWebVaultSettingsAsync(AppResources.ChangeMasterPasswordDescriptionLong, AppResources.ContinueToWebApp), allowsMultipleExecutions: false);
            LockCommand = CreateDefaultAsyncRelayCommand(() => _vaultTimeoutService.LockAsync(true, true), allowsMultipleExecutions: false);
            LogOutCommand = CreateDefaultAsyncRelayCommand(LogOutAsync, allowsMultipleExecutions: false);
            DeleteAccountCommand = CreateDefaultAsyncRelayCommand(() => Page.Navigation.PushModalAsync(new NavigationPage(new DeleteAccountPage())), allowsMultipleExecutions: false);
        }

        public bool UseThisDeviceToApproveLoginRequests
        {
            get => _useThisDeviceToApproveLoginRequests;
            set
            {
                if (SetProperty(ref _useThisDeviceToApproveLoginRequests, value))
                {
                    ((ICommand)ToggleUseThisDeviceToApproveLoginRequestsCommand).Execute(null);
                }
            }
        }

        public string UnlockWithBiometricsTitle
        {
            get
            {
                if (!_supportsBiometric)
                {
                    return null;
                }

                var biometricName = AppResources.Biometrics;
                if (DeviceInfo.Platform == DevicePlatform.iOS)
                {
                    biometricName = _deviceActionService.SupportsFaceBiometric()
                        ? AppResources.FaceID
                        : AppResources.TouchID;
                }

                return string.Format(AppResources.UnlockWith, biometricName);
            }
        }

        public bool CanUnlockWithBiometrics
        {
            get => _canUnlockWithBiometrics;
            set
            {
                TriggerVaultTimeoutActionLockAllowedPropertyChanged();
                if (SetProperty(ref _canUnlockWithBiometrics, value))
                {
                    ((ICommand)ToggleCanUnlockWithBiometricsCommand).Execute(null);
                }
            }
        }

        public bool CanUnlockWithPin
        {
            get => _canUnlockWithPin;
            set
            {
                TriggerVaultTimeoutActionLockAllowedPropertyChanged();
                if (SetProperty(ref _canUnlockWithPin, value))
                {
                    ((ICommand)ToggleCanUnlockWithPinCommand).Execute(null);
                }
            }
        }

        public bool IsVaultTimeoutActionLockAllowed => _hasMasterPassword || _canUnlockWithBiometrics || _canUnlockWithPin;

        public string SetUpUnlockMethodLabel => IsVaultTimeoutActionLockAllowed ? null : AppResources.SetUpAnUnlockOptionToChangeYourVaultTimeoutAction;

        public TimeSpan? CustomVaultTimeoutTime
        {
            get => _customVaultTimeoutTime;
            set
            {
                var oldValue = _customVaultTimeoutTime;

                if (SetProperty(ref _customVaultTimeoutTime, value, additionalPropertyNames: new string[] { nameof(CustomVaultTimeoutTimeVerbalized) }) && value.HasValue)
                {
                    UpdateVaultTimeoutAsync((int)value.Value.TotalMinutes)
                        .FireAndForget(ex =>
                        {
                            HandleException(ex);
                            MainThread.BeginInvokeOnMainThread(() => SetProperty(ref _customVaultTimeoutTime, oldValue));
                        });
                }
                TriggerVaultTimeoutActionLockAllowedPropertyChanged();
            }
        }

        public string CustomVaultTimeoutTimeVerbalized => CustomVaultTimeoutTime?.Verbalize(A11yExtensions.TimeSpanVerbalizationMode.HoursAndMinutes);

        public bool ShowCustomVaultTimeoutPicker => VaultTimeoutPickerViewModel.SelectedKey == CUSTOM_VAULT_TIMEOUT_VALUE;

        public bool ShowVaultTimeoutPolicyInfo => _maximumVaultTimeoutPolicy.HasValue || HasVaultTimeoutActionPolicy;

        public string VaultTimeoutPolicyDescription
        {
            get
            {
                if (!ShowVaultTimeoutPolicyInfo)
                {
                    return null;
                }

                static string LocalizeTimeoutAction(string actionPolicy)
                {
                    return actionPolicy == Policy.ACTION_LOCK ? AppResources.Lock : AppResources.LogOut;
                };

                if (!_maximumVaultTimeoutPolicy.HasValue)
                {
                    return string.Format(AppResources.VaultTimeoutActionPolicyInEffect, LocalizeTimeoutAction(_vaultTimeoutActionPolicy));
                }

                var hours = Math.Floor((float)_maximumVaultTimeoutPolicy / 60);
                var minutes = _maximumVaultTimeoutPolicy % 60;

                return string.IsNullOrWhiteSpace(_vaultTimeoutActionPolicy)
                    ? string.Format(AppResources.VaultTimeoutPolicyInEffect, hours, minutes)
                    : string.Format(AppResources.VaultTimeoutPolicyWithActionInEffect, hours, minutes, LocalizeTimeoutAction(_vaultTimeoutActionPolicy));
            }
        }

        public bool ShowChangeMasterPassword { get; private set; }

        private int? CurrentVaultTimeout => GetRawVaultTimeoutFrom(VaultTimeoutPickerViewModel.SelectedKey);

        private bool IncludeLinksWithSubscriptionInfo => DeviceInfo.Platform != DevicePlatform.iOS;

        private bool HasVaultTimeoutActionPolicy => !string.IsNullOrEmpty(_vaultTimeoutActionPolicy);

        public PickerViewModel<int> VaultTimeoutPickerViewModel { get; }
        public PickerViewModel<VaultTimeoutAction> VaultTimeoutActionPickerViewModel { get; }

        public AsyncRelayCommand ToggleUseThisDeviceToApproveLoginRequestsCommand { get; }
        public ICommand GoToPendingLogInRequestsCommand { get; }
        public AsyncRelayCommand ToggleCanUnlockWithBiometricsCommand { get; }
        public AsyncRelayCommand ToggleCanUnlockWithPinCommand { get; }
        public ICommand ShowAccountFingerprintPhraseCommand { get; }
        public ICommand GoToTwoStepLoginCommand { get; }
        public ICommand GoToChangeMasterPasswordCommand { get; }
        public ICommand LockCommand { get; }
        public ICommand LogOutCommand { get; }
        public ICommand DeleteAccountCommand { get; }

        public async Task InitAsync()
        {
            var decryptionOptions = await _stateService.GetAccountDecryptionOptions();
            // set default true for backwards compatibility
            _hasMasterPassword = decryptionOptions?.HasMasterPassword ?? true;
            _useThisDeviceToApproveLoginRequests = await _stateService.GetApprovePasswordlessLoginsAsync();
            _supportsBiometric = await _platformUtilsService.SupportsBiometricAsync();
            _canUnlockWithBiometrics = await _vaultTimeoutService.IsBiometricLockSetAsync();
            _canUnlockWithPin = await _vaultTimeoutService.GetPinLockTypeAsync() != Core.Services.PinLockType.Disabled;

            await LoadPoliciesAsync();
            await InitVaultTimeoutPickerAsync();
            await InitVaultTimeoutActionPickerAsync();

            ShowChangeMasterPassword = IncludeLinksWithSubscriptionInfo && await _userVerificationService.HasMasterPasswordAsync();

            _inited = true;

            MainThread.BeginInvokeOnMainThread(() =>
            {
                TriggerPropertyChanged(nameof(UseThisDeviceToApproveLoginRequests));
                TriggerPropertyChanged(nameof(UnlockWithBiometricsTitle));
                TriggerPropertyChanged(nameof(CanUnlockWithBiometrics));
                TriggerPropertyChanged(nameof(CanUnlockWithPin));
                TriggerPropertyChanged(nameof(ShowVaultTimeoutPolicyInfo));
                TriggerPropertyChanged(nameof(VaultTimeoutPolicyDescription));
                TriggerPropertyChanged(nameof(ShowChangeMasterPassword));
                TriggerUpdateCustomVaultTimeoutPicker();
                TriggerVaultTimeoutActionLockAllowedPropertyChanged();
                ToggleUseThisDeviceToApproveLoginRequestsCommand.NotifyCanExecuteChanged();
                ToggleCanUnlockWithBiometricsCommand.NotifyCanExecuteChanged();
                ToggleCanUnlockWithPinCommand.NotifyCanExecuteChanged();
                VaultTimeoutPickerViewModel.SelectOptionCommand.NotifyCanExecuteChanged();
                VaultTimeoutActionPickerViewModel.SelectOptionCommand.NotifyCanExecuteChanged();
            });
        }

        private async Task LoadPoliciesAsync()
        {
            if (!await _policyService.PolicyAppliesToUser(PolicyType.MaximumVaultTimeout))
            {
                return;
            }

            var maximumVaultTimeoutPolicy = await _policyService.FirstOrDefault(PolicyType.MaximumVaultTimeout);
            _maximumVaultTimeoutPolicy = maximumVaultTimeoutPolicy?.GetInt(Policy.MINUTES_KEY);
            _vaultTimeoutActionPolicy = maximumVaultTimeoutPolicy?.GetString(Policy.ACTION_KEY);

            MainThread.BeginInvokeOnMainThread(VaultTimeoutActionPickerViewModel.SelectOptionCommand.NotifyCanExecuteChanged);
        }

        private async Task InitVaultTimeoutPickerAsync()
        {
            var options = new Dictionary<int, string>
            {
                [0] = AppResources.Immediately,
                [1] = AppResources.OneMinute,
                [5] = AppResources.FiveMinutes,
                [15] = AppResources.FifteenMinutes,
                [30] = AppResources.ThirtyMinutes,
                [60] = AppResources.OneHour,
                [240] = AppResources.FourHours,
                [-1] = AppResources.OnRestart,
                [NEVER_SESSION_TIMEOUT_VALUE] = AppResources.Never
            };

            if (_maximumVaultTimeoutPolicy.HasValue)
            {
                options = options.Where(t => t.Key >= 0 && t.Key <= _maximumVaultTimeoutPolicy.Value)
                                 .ToDictionary(v => v.Key, v => v.Value);
            }

            options.Add(CUSTOM_VAULT_TIMEOUT_VALUE, AppResources.Custom);

            var vaultTimeout = await _vaultTimeoutService.GetVaultTimeout() ?? NEVER_SESSION_TIMEOUT_VALUE;
            VaultTimeoutPickerViewModel.Init(options, vaultTimeout, CUSTOM_VAULT_TIMEOUT_VALUE, false);

            if (VaultTimeoutPickerViewModel.SelectedKey == CUSTOM_VAULT_TIMEOUT_VALUE)
            {
                _customVaultTimeoutTime = TimeSpan.FromMinutes(vaultTimeout);
            }
            TriggerVaultTimeoutActionLockAllowedPropertyChanged();
        }

        private async Task InitVaultTimeoutActionPickerAsync()
        {
            var options = new Dictionary<VaultTimeoutAction, string>();
            if (IsVaultTimeoutActionLockAllowed)
            {
                options.Add(VaultTimeoutAction.Lock, AppResources.Lock);
            }
            options.Add(VaultTimeoutAction.Logout, AppResources.LogOut);

            var timeoutAction = await _vaultTimeoutService.GetVaultTimeoutAction() ?? VaultTimeoutAction.Lock;
            if (!IsVaultTimeoutActionLockAllowed && timeoutAction == VaultTimeoutAction.Lock)
            {
                timeoutAction = VaultTimeoutAction.Logout;
                await _vaultTimeoutService.SetVaultTimeoutOptionsAsync(CurrentVaultTimeout, VaultTimeoutAction.Logout);
            }

            VaultTimeoutActionPickerViewModel.Init(options, timeoutAction, IsVaultTimeoutActionLockAllowed ? VaultTimeoutAction.Lock : VaultTimeoutAction.Logout);
            TriggerVaultTimeoutActionLockAllowedPropertyChanged();
        }

        private async Task ToggleUseThisDeviceToApproveLoginRequestsAsync()
        {
            if (UseThisDeviceToApproveLoginRequests
                &&
                !await Page.DisplayAlert(AppResources.ApproveLoginRequests, AppResources.UseThisDeviceToApproveLoginRequestsMadeFromOtherDevices, AppResources.Yes, AppResources.No))
            {
                _useThisDeviceToApproveLoginRequests = !UseThisDeviceToApproveLoginRequests;
                MainThread.BeginInvokeOnMainThread(() => TriggerPropertyChanged(nameof(UseThisDeviceToApproveLoginRequests)));
                return;
            }

            await _stateService.SetApprovePasswordlessLoginsAsync(UseThisDeviceToApproveLoginRequests);

            if (!UseThisDeviceToApproveLoginRequests || await _pushNotificationService.AreNotificationsSettingsEnabledAsync())
            {
                return;
            }

            var openAppSettingsResult = await _platformUtilsService.ShowDialogAsync(
                AppResources.ReceivePushNotificationsForNewLoginRequests,
                string.Empty,
                AppResources.Settings,
                AppResources.NoThanks
            );
            if (openAppSettingsResult)
            {
                _deviceActionService.OpenAppSettings();
            }
        }

        private async Task ToggleCanUnlockWithBiometricsAsync()
        {
            if (!_canUnlockWithBiometrics)
            {
                MainThread.BeginInvokeOnMainThread(() => TriggerPropertyChanged(nameof(CanUnlockWithBiometrics)));
                await UpdateVaultTimeoutActionIfNeededAsync();
                await _biometricsService.SetCanUnlockWithBiometricsAsync(CanUnlockWithBiometrics);
                return;
            }

            if (!_supportsBiometric
                ||
                await _platformUtilsService.AuthenticateBiometricAsync(null, DeviceInfo.Platform == DevicePlatform.Android ? "." : null) != true)
            {
                _canUnlockWithBiometrics = false;
                MainThread.BeginInvokeOnMainThread(() => TriggerPropertyChanged(nameof(CanUnlockWithBiometrics)));
                return;
            }

            await _biometricsService.SetCanUnlockWithBiometricsAsync(CanUnlockWithBiometrics);
            await InitVaultTimeoutActionPickerAsync();
        }

        public async Task ToggleCanUnlockWithPinAsync()
        {
            if (!_canUnlockWithPin)
            {
                await _vaultTimeoutService.ClearAsync();
                await UpdateVaultTimeoutActionIfNeededAsync();
                return;
            }

            var newPin = await _deviceActionService.DisplayPromptAync(AppResources.EnterPIN,
                AppResources.SetPINDescription, null, AppResources.Submit, AppResources.Cancel, true);
            if (string.IsNullOrWhiteSpace(newPin))
            {
                _canUnlockWithPin = false;
                MainThread.BeginInvokeOnMainThread(() => TriggerPropertyChanged(nameof(CanUnlockWithPin)));
                return;
            }

            var requireMasterPasswordOnRestart = await _userVerificationService.HasMasterPasswordAsync()
                                                 &&
                                                 await _platformUtilsService.ShowDialogAsync(AppResources.PINRequireMasterPasswordRestart,
                                                       AppResources.UnlockWithPIN,
                                                       AppResources.Yes,
                                                       AppResources.No);

            await _userPinService.SetupPinAsync(newPin, requireMasterPasswordOnRestart);
            await InitVaultTimeoutActionPickerAsync();
        }

        private async Task UpdateVaultTimeoutActionIfNeededAsync()
        {
            TriggerVaultTimeoutActionLockAllowedPropertyChanged();
            if (IsVaultTimeoutActionLockAllowed)
            {
                return;
            }

            VaultTimeoutActionPickerViewModel.Select(VaultTimeoutAction.Logout);
            await _vaultTimeoutService.SetVaultTimeoutOptionsAsync(CurrentVaultTimeout, VaultTimeoutAction.Logout);
            _deviceActionService.Toast(AppResources.VaultTimeoutActionChangedToLogOut);
        }

        private async Task<bool> OnVaultTimeoutChangingAsync(int newTimeout)
        {
            if (newTimeout == NEVER_SESSION_TIMEOUT_VALUE
                &&
                !await _platformUtilsService.ShowDialogAsync(AppResources.NeverLockWarning, AppResources.Warning, AppResources.Yes, AppResources.Cancel))

            {
                return false;
            }

            if (newTimeout == CUSTOM_VAULT_TIMEOUT_VALUE)
            {
                _customVaultTimeoutTime = TimeSpan.FromMinutes(0);
            }

            return await UpdateVaultTimeoutAsync(newTimeout);
        }

        private async Task<bool> UpdateVaultTimeoutAsync(int newTimeout)
        {
            var rawTimeout = GetRawVaultTimeoutFrom(newTimeout);

            if (rawTimeout > _maximumVaultTimeoutPolicy)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.VaultTimeoutToLarge, AppResources.Warning);
                VaultTimeoutPickerViewModel.Select(_maximumVaultTimeoutPolicy.Value, false);

                if (VaultTimeoutPickerViewModel.SelectedKey == CUSTOM_VAULT_TIMEOUT_VALUE)
                {
                    _customVaultTimeoutTime = TimeSpan.FromMinutes(_maximumVaultTimeoutPolicy.Value);
                }

                MainThread.BeginInvokeOnMainThread(TriggerUpdateCustomVaultTimeoutPicker);

                return false;
            }

            await _vaultTimeoutService.SetVaultTimeoutOptionsAsync(rawTimeout, VaultTimeoutActionPickerViewModel.SelectedKey);

            await _cryptoService.RefreshKeysAsync();

            return true;
        }

        private void TriggerUpdateCustomVaultTimeoutPicker()
        {
            TriggerPropertyChanged(nameof(ShowCustomVaultTimeoutPicker));
            TriggerPropertyChanged(nameof(CustomVaultTimeoutTime));
        }

        private void TriggerVaultTimeoutActionLockAllowedPropertyChanged()
        {
            MainThread.BeginInvokeOnMainThread(() =>
            {
                TriggerPropertyChanged(nameof(IsVaultTimeoutActionLockAllowed));
                TriggerPropertyChanged(nameof(SetUpUnlockMethodLabel));
                VaultTimeoutActionPickerViewModel.SelectOptionCommand.NotifyCanExecuteChanged();
            });
        }

        private int? GetRawVaultTimeoutFrom(int vaultTimeoutPickerKey)
        {
            if (vaultTimeoutPickerKey == NEVER_SESSION_TIMEOUT_VALUE)
            {
                return null;
            }

            if (vaultTimeoutPickerKey == CUSTOM_VAULT_TIMEOUT_VALUE
                &&
                CustomVaultTimeoutTime.HasValue)
            {
                return (int)CustomVaultTimeoutTime.Value.TotalMinutes;
            }

            return vaultTimeoutPickerKey;
        }

        private async Task<bool> OnVaultTimeoutActionChangingAsync(VaultTimeoutAction timeoutActionKey)
        {
            if (!string.IsNullOrEmpty(_vaultTimeoutActionPolicy))
            {
                // do nothing if we have a policy set
                return false;
            }

            if (timeoutActionKey == VaultTimeoutAction.Logout
                &&
                !await _platformUtilsService.ShowDialogAsync(AppResources.VaultTimeoutLogOutConfirmation, AppResources.Warning, AppResources.Yes, AppResources.Cancel))
            {
                return false;
            }

            await _vaultTimeoutService.SetVaultTimeoutOptionsAsync(CurrentVaultTimeout, timeoutActionKey);
            _messagingService.Send(AppHelpers.VAULT_TIMEOUT_ACTION_CHANGED_MESSAGE_COMMAND);
            TriggerVaultTimeoutActionLockAllowedPropertyChanged();
            return true;
        }

        private async Task ShowAccountFingerprintPhraseAsync()
        {
            List<string> fingerprint;
            try
            {
                fingerprint = await _cryptoService.GetFingerprintAsync(await _stateService.GetActiveUserIdAsync());
            }
            catch (Exception e) when (e.Message == "No public key available.")
            {
                return;
            }

            var phrase = string.Join("-", fingerprint);
            var text = $"{AppResources.YourAccountsFingerprint}:\n\n{phrase}";

            var learnMore = await _platformUtilsService.ShowDialogAsync(text, AppResources.FingerprintPhrase,
                AppResources.LearnMore, AppResources.Close);
            if (learnMore)
            {
                _platformUtilsService.LaunchUri(ExternalLinksConstants.HELP_FINGERPRINT_PHRASE);
            }
        }

        private async Task GoToWebVaultSettingsAsync(string dialogText, string dialogTitle)
        {
            if (await _platformUtilsService.ShowDialogAsync(dialogText, dialogTitle, AppResources.Continue, AppResources.Cancel))
            {
                _platformUtilsService.LaunchUri(string.Format(ExternalLinksConstants.WEB_VAULT_SETTINGS_FORMAT, _environmentService.GetWebVaultUrl()));
            }
        }

        public async Task LogOutAsync()
        {
            if (await _platformUtilsService.ShowDialogAsync(AppResources.LogoutConfirmation, AppResources.LogOut, AppResources.Yes, AppResources.Cancel))
            {
                _messagingService.Send(AccountsManagerMessageCommands.LOGOUT);
            }
        }
    }
}
