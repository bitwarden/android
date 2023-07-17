using System;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.Helpers;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class LockPageViewModel : BaseViewModel
    {
        private readonly IApiService _apiService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly ICryptoService _cryptoService;
        private readonly IMessagingService _messagingService;
        private readonly IEnvironmentService _environmentService;
        private readonly IStateService _stateService;
        private readonly IBiometricService _biometricService;
        private readonly IKeyConnectorService _keyConnectorService;
        private readonly ILogger _logger;
        private readonly IWatchDeviceService _watchDeviceService;
        private readonly WeakEventManager<int?> _secretEntryFocusWeakEventManager = new WeakEventManager<int?>();
        private readonly IPolicyService _policyService;
        private readonly IPasswordGenerationService _passwordGenerationService;

        private string _email;
        private string _masterPassword;
        private string _pin;
        private bool _showPassword;
        private bool _pinLock;
        private bool _biometricLock;
        private bool _biometricIntegrityValid = true;
        private bool _biometricButtonVisible;
        private bool _usingKeyConnector;
        private string _biometricButtonText;
        private string _loggedInAsText;
        private string _lockedVerifyText;
        private bool _isPinProtected;
        private bool _isPinProtectedWithKey;

        public LockPageViewModel()
        {
            _apiService = ServiceContainer.Resolve<IApiService>("apiService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _biometricService = ServiceContainer.Resolve<IBiometricService>("biometricService");
            _keyConnectorService = ServiceContainer.Resolve<IKeyConnectorService>("keyConnectorService");
            _logger = ServiceContainer.Resolve<ILogger>("logger");
            _watchDeviceService = ServiceContainer.Resolve<IWatchDeviceService>();
            _policyService = ServiceContainer.Resolve<IPolicyService>();
            _passwordGenerationService = ServiceContainer.Resolve<IPasswordGenerationService>();

            PageTitle = AppResources.VerifyMasterPassword;
            TogglePasswordCommand = new Command(TogglePassword);
            SubmitCommand = new Command(async () => await SubmitAsync());

            AccountSwitchingOverlayViewModel = new AccountSwitchingOverlayViewModel(_stateService, _messagingService, _logger)
            {
                AllowAddAccountRow = true,
                AllowActiveAccountSelection = true
            };
        }

        public string MasterPassword
        {
            get => _masterPassword;
            set => SetProperty(ref _masterPassword, value);
        }

        public string Pin
        {
            get => _pin;
            set => SetProperty(ref _pin, value);
        }

        public bool ShowPassword
        {
            get => _showPassword;
            set => SetProperty(ref _showPassword, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowPasswordIcon),
                    nameof(PasswordVisibilityAccessibilityText),
                });
        }

        public bool PinLock
        {
            get => _pinLock;
            set => SetProperty(ref _pinLock, value);
        }

        public bool UsingKeyConnector
        {
            get => _usingKeyConnector;
        }

        public bool BiometricLock
        {
            get => _biometricLock;
            set => SetProperty(ref _biometricLock, value);
        }

        public bool BiometricIntegrityValid
        {
            get => _biometricIntegrityValid;
            set => SetProperty(ref _biometricIntegrityValid, value);
        }

        public bool BiometricButtonVisible
        {
            get => _biometricButtonVisible;
            set => SetProperty(ref _biometricButtonVisible, value);
        }

        public string BiometricButtonText
        {
            get => _biometricButtonText;
            set => SetProperty(ref _biometricButtonText, value);
        }

        public string LoggedInAsText
        {
            get => _loggedInAsText;
            set => SetProperty(ref _loggedInAsText, value);
        }

        public string LockedVerifyText
        {
            get => _lockedVerifyText;
            set => SetProperty(ref _lockedVerifyText, value);
        }

        public AccountSwitchingOverlayViewModel AccountSwitchingOverlayViewModel { get; }

        public Command SubmitCommand { get; }
        public Command TogglePasswordCommand { get; }
        public string ShowPasswordIcon => ShowPassword ? BitwardenIcons.EyeSlash : BitwardenIcons.Eye;
        public string PasswordVisibilityAccessibilityText => ShowPassword ? AppResources.PasswordIsVisibleTapToHide : AppResources.PasswordIsNotVisibleTapToShow;
        public Action UnlockedAction { get; set; }
        public event Action<int?> FocusSecretEntry
        {
            add => _secretEntryFocusWeakEventManager.AddEventHandler(value);
            remove => _secretEntryFocusWeakEventManager.RemoveEventHandler(value);
        }

        public async Task InitAsync()
        {
            (_isPinProtected, _isPinProtectedWithKey) = await _vaultTimeoutService.IsPinLockSetAsync();
            PinLock = (_isPinProtected && await _stateService.GetPinProtectedKeyAsync() != null) ||
                      _isPinProtectedWithKey;
            BiometricLock = await _vaultTimeoutService.IsBiometricLockSetAsync() && await _cryptoService.HasKeyAsync();

            // Users with key connector and without biometric or pin has no MP to unlock with
            _usingKeyConnector = await _keyConnectorService.GetUsesKeyConnector();
            if (_usingKeyConnector && !(BiometricLock || PinLock))
            {
                await _vaultTimeoutService.LogOutAsync();
                return;
            }
            _email = await _stateService.GetEmailAsync();
            if (string.IsNullOrWhiteSpace(_email))
            {
                await _vaultTimeoutService.LogOutAsync();
                _logger.Exception(new NullReferenceException("Email not found in storage"));
                return;
            }
            var webVault = _environmentService.GetWebVaultUrl(true);
            if (string.IsNullOrWhiteSpace(webVault))
            {
                webVault = "https://bitwarden.com";
            }
            var webVaultHostname = CoreHelpers.GetHostname(webVault);
            LoggedInAsText = string.Format(AppResources.LoggedInAsOn, _email, webVaultHostname);
            if (PinLock)
            {
                PageTitle = AppResources.VerifyPIN;
                LockedVerifyText = AppResources.VaultLockedPIN;
            }
            else
            {
                if (_usingKeyConnector)
                {
                    PageTitle = AppResources.UnlockVault;
                    LockedVerifyText = AppResources.VaultLockedIdentity;
                }
                else
                {
                    PageTitle = AppResources.VerifyMasterPassword;
                    LockedVerifyText = AppResources.VaultLockedMasterPassword;
                }
            }

            if (BiometricLock)
            {
                BiometricIntegrityValid = await _platformUtilsService.IsBiometricIntegrityValidAsync();
                if (!_biometricIntegrityValid)
                {
                    BiometricButtonVisible = false;
                    return;
                }
                BiometricButtonVisible = true;
                BiometricButtonText = AppResources.UseBiometricsToUnlock;
                if (Device.RuntimePlatform == Device.iOS)
                {
                    var supportsFace = await _deviceActionService.SupportsFaceBiometricAsync();
                    BiometricButtonText = supportsFace ? AppResources.UseFaceIDToUnlock :
                        AppResources.UseFingerprintToUnlock;
                }

            }
        }

        public async Task SubmitAsync()
        {
            if (PinLock && string.IsNullOrWhiteSpace(Pin))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.PIN),
                    AppResources.Ok);
                return;
            }
            if (!PinLock && string.IsNullOrWhiteSpace(MasterPassword))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.MasterPassword),
                    AppResources.Ok);
                return;
            }

            ShowPassword = false;
            var kdfConfig = await _stateService.GetActiveUserCustomDataAsync(a => new KdfConfig(a?.Profile));

            if (PinLock)
            {
                var failed = true;
                try
                {
                    if (_isPinProtected)
                    {
                        var key = await _cryptoService.MakeKeyFromPinAsync(Pin, _email,
                            kdfConfig,
                            await _stateService.GetPinProtectedKeyAsync());
                        var encKey = await _cryptoService.GetEncKeyAsync(key);
                        var protectedPin = await _stateService.GetProtectedPinAsync();
                        var decPin = await _cryptoService.DecryptToUtf8Async(new EncString(protectedPin), encKey);
                        failed = decPin != Pin;
                        if (!failed)
                        {
                            Pin = string.Empty;
                            await AppHelpers.ResetInvalidUnlockAttemptsAsync();
                            await SetKeyAndContinueAsync(key);
                        }
                    }
                    else
                    {
                        var key = await _cryptoService.MakeKeyFromPinAsync(Pin, _email, kdfConfig);
                        failed = false;
                        Pin = string.Empty;
                        await AppHelpers.ResetInvalidUnlockAttemptsAsync();
                        await SetKeyAndContinueAsync(key);
                    }
                }
                catch
                {
                    failed = true;
                }
                if (failed)
                {
                    var invalidUnlockAttempts = await AppHelpers.IncrementInvalidUnlockAttemptsAsync();
                    if (invalidUnlockAttempts >= 5)
                    {
                        _messagingService.Send("logout");
                        return;
                    }
                    await _platformUtilsService.ShowDialogAsync(AppResources.InvalidPIN,
                        AppResources.AnErrorHasOccurred);
                }
            }
            else
            {
                var key = await _cryptoService.MakeKeyAsync(MasterPassword, _email, kdfConfig);
                var storedKeyHash = await _cryptoService.GetPasswordHashAsync();
                var passwordValid = false;
                MasterPasswordPolicyOptions enforcedMasterPasswordOptions = null;

                if (storedKeyHash != null)
                {
                    passwordValid = await _cryptoService.CompareAndUpdatePasswordHashAsync(MasterPassword, key);
                }
                else
                {
                    await _deviceActionService.ShowLoadingAsync(AppResources.Loading);
                    var keyHash = await _cryptoService.HashPasswordAsync(MasterPassword, key, HashPurpose.ServerAuthorization);
                    var request = new PasswordVerificationRequest();
                    request.MasterPasswordHash = keyHash;

                    try
                    {
                        var response = await _apiService.PostAccountVerifyPasswordAsync(request);
                        enforcedMasterPasswordOptions = response.MasterPasswordPolicy;
                        passwordValid = true;
                        var localKeyHash = await _cryptoService.HashPasswordAsync(MasterPassword, key, HashPurpose.LocalAuthorization);
                        await _cryptoService.SetPasswordHashAsync(localKeyHash);
                    }
                    catch (Exception e)
                    {
                        System.Diagnostics.Debug.WriteLine(">>> {0}: {1}", e.GetType(), e.StackTrace);
                    }
                    await _deviceActionService.HideLoadingAsync();
                }
                if (passwordValid)
                {
                    if (_isPinProtected)
                    {
                        var protectedPin = await _stateService.GetProtectedPinAsync();
                        var encKey = await _cryptoService.GetEncKeyAsync(key);
                        var decPin = await _cryptoService.DecryptToUtf8Async(new EncString(protectedPin), encKey);
                        var pinKey = await _cryptoService.MakePinKeyAysnc(decPin, _email, kdfConfig);
                        await _stateService.SetPinProtectedKeyAsync(await _cryptoService.EncryptAsync(key.Key, pinKey));
                    }

                    if (await RequirePasswordChangeAsync(enforcedMasterPasswordOptions))
                    {
                        // Save the ForcePasswordResetReason to force a password reset after unlock
                        await _stateService.SetForcePasswordResetReasonAsync(
                            ForcePasswordResetReason.WeakMasterPasswordOnLogin);
                    }

                    MasterPassword = string.Empty;
                    await AppHelpers.ResetInvalidUnlockAttemptsAsync();
                    await SetKeyAndContinueAsync(key);

                    // Re-enable biometrics
                    if (BiometricLock & !BiometricIntegrityValid)
                    {
                        await _biometricService.SetupBiometricAsync();
                    }
                }
                else
                {
                    var invalidUnlockAttempts = await AppHelpers.IncrementInvalidUnlockAttemptsAsync();
                    if (invalidUnlockAttempts >= 5)
                    {
                        _messagingService.Send("logout");
                        return;
                    }
                    await _platformUtilsService.ShowDialogAsync(AppResources.InvalidMasterPassword,
                        AppResources.AnErrorHasOccurred);
                }
            }
        }

        /// <summary>
        /// Checks if the master password requires updating to meet the enforced policy requirements
        /// </summary>
        /// <param name="options"></param>
        private async Task<bool> RequirePasswordChangeAsync(MasterPasswordPolicyOptions options = null)
        {
            // If no policy options are provided, attempt to load them from the policy service
            var enforcedOptions = options ?? await _policyService.GetMasterPasswordPolicyOptions();

            // No policy to enforce on login/unlock
            if (!(enforcedOptions is { EnforceOnLogin: true }))
            {
                return false;
            }

            var strength = _passwordGenerationService.PasswordStrength(
                MasterPassword, _passwordGenerationService.GetPasswordStrengthUserInput(_email))?.Score;

            if (!strength.HasValue)
            {
                _logger.Error("Unable to evaluate master password strength during unlock");
                return false;
            }

            return !await _policyService.EvaluateMasterPassword(
                strength.Value,
                MasterPassword,
                enforcedOptions
            );
        }

        public async Task LogOutAsync()
        {
            var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.LogoutConfirmation,
                AppResources.LogOut, AppResources.Yes, AppResources.Cancel);
            if (confirmed)
            {
                _messagingService.Send("logout");
            }
        }

        public void ResetPinPasswordFields()
        {
            try
            {
                MasterPassword = string.Empty;
                Pin = string.Empty;
                ShowPassword = false;
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        public void TogglePassword()
        {
            ShowPassword = !ShowPassword;
            var secret = PinLock ? Pin : MasterPassword;
            _secretEntryFocusWeakEventManager.RaiseEvent(string.IsNullOrEmpty(secret) ? 0 : secret.Length, nameof(FocusSecretEntry));
        }

        public async Task PromptBiometricAsync()
        {
            BiometricIntegrityValid = await _platformUtilsService.IsBiometricIntegrityValidAsync();
            BiometricButtonVisible = BiometricIntegrityValid;
            if (!BiometricLock || !BiometricIntegrityValid)
            {
                return;
            }
            var success = await _platformUtilsService.AuthenticateBiometricAsync(null,
                PinLock ? AppResources.PIN : AppResources.MasterPassword,
                () => _secretEntryFocusWeakEventManager.RaiseEvent((int?)null, nameof(FocusSecretEntry)));
            await _stateService.SetBiometricLockedAsync(!success);
            if (success)
            {
                await DoContinueAsync();
            }
        }

        private async Task SetKeyAndContinueAsync(SymmetricCryptoKey key)
        {
            var hasKey = await _cryptoService.HasKeyAsync();
            if (!hasKey)
            {
                await _cryptoService.SetKeyAsync(key);
            }
            await DoContinueAsync();
        }

        private async Task DoContinueAsync()
        {
            await _stateService.SetBiometricLockedAsync(false);
            _watchDeviceService.SyncDataToWatchAsync().FireAndForget();
            _messagingService.Send("unlocked");
            UnlockedAction?.Invoke();
        }
    }
}
