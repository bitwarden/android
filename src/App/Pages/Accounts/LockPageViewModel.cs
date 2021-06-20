using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;
using System;
using System.Threading.Tasks;
using Bit.App.Utilities;
using Bit.Core.Models.Request;
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
        private readonly IStorageService _storageService;
        private readonly IUserService _userService;
        private readonly IMessagingService _messagingService;
        private readonly IStorageService _secureStorageService;
        private readonly IEnvironmentService _environmentService;
        private readonly IStateService _stateService;
        private readonly IBiometricService _biometricService;

        private string _email;
        private bool _showPassword;
        private bool _pinLock;
        private bool _biometricLock;
        private bool _biometricIntegrityValid = true;
        private bool _biometricButtonVisible;
        private string _biometricButtonText;
        private string _loggedInAsText;
        private string _lockedVerifyText;
        private Tuple<bool, bool> _pinSet;

        public LockPageViewModel()
        {
            _apiService = ServiceContainer.Resolve<IApiService>("apiService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _userService = ServiceContainer.Resolve<IUserService>("userService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _secureStorageService = ServiceContainer.Resolve<IStorageService>("secureStorageService");
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _biometricService = ServiceContainer.Resolve<IBiometricService>("biometricService");

            PageTitle = AppResources.VerifyMasterPassword;
            TogglePasswordCommand = new Command(TogglePassword);
            SubmitCommand = new Command(async () => await SubmitAsync());
        }

        public bool ShowPassword
        {
            get => _showPassword;
            set => SetProperty(ref _showPassword, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowPasswordIcon)
                });
        }

        public bool PinLock
        {
            get => _pinLock;
            set => SetProperty(ref _pinLock, value);
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

        public Command SubmitCommand { get; }
        public Command TogglePasswordCommand { get; }
        public string ShowPasswordIcon => ShowPassword ? "" : "";
        public string MasterPassword { get; set; }
        public string Pin { get; set; }
        public Action UnlockedAction { get; set; }

        public async Task InitAsync(bool autoPromptBiometric)
        {
            _pinSet = await _vaultTimeoutService.IsPinLockSetAsync();
            PinLock = (_pinSet.Item1 && _vaultTimeoutService.PinProtectedKey != null) || _pinSet.Item2;
            BiometricLock = await _vaultTimeoutService.IsBiometricLockSetAsync() && await _cryptoService.HasKeyAsync();
            _email = await _userService.GetEmailAsync();
            var webVault = _environmentService.GetWebVaultUrl();
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
                PageTitle = AppResources.VerifyMasterPassword;
                LockedVerifyText = AppResources.VaultLockedMasterPassword;
            }

            if (BiometricLock)
            {
                BiometricIntegrityValid = await _biometricService.ValidateIntegrityAsync();
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
                if (autoPromptBiometric)
                {
                    var tasks = Task.Run(async () =>
                    {
                        await Task.Delay(500);
                        Device.BeginInvokeOnMainThread(async () => await PromptBiometricAsync());
                    });
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
            var kdf = await _userService.GetKdfAsync();
            var kdfIterations = await _userService.GetKdfIterationsAsync();

            if (PinLock)
            {
                var failed = true;
                try
                {
                    if (_pinSet.Item1)
                    {
                        var key = await _cryptoService.MakeKeyFromPinAsync(Pin, _email,
                            kdf.GetValueOrDefault(KdfType.PBKDF2_SHA256), kdfIterations.GetValueOrDefault(5000),
                            _vaultTimeoutService.PinProtectedKey);
                        var encKey = await _cryptoService.GetEncKeyAsync(key);
                        var protectedPin = await _storageService.GetAsync<string>(Constants.ProtectedPin);
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
                        var key = await _cryptoService.MakeKeyFromPinAsync(Pin, _email,
                            kdf.GetValueOrDefault(KdfType.PBKDF2_SHA256), kdfIterations.GetValueOrDefault(5000));
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
                var key = await _cryptoService.MakeKeyAsync(MasterPassword, _email, kdf, kdfIterations);
                var storedKeyHash = await _cryptoService.GetKeyHashAsync();
                var passwordValid = false;
                
                if (storedKeyHash != null)
                {
                    passwordValid = await _cryptoService.CompareAndUpdateKeyHashAsync(MasterPassword, key);
                }
                else
                {
                    await _deviceActionService.ShowLoadingAsync(AppResources.Loading);
                    var keyHash = await _cryptoService.HashPasswordAsync(MasterPassword, key, HashPurpose.ServerAuthorization);
                    var request = new PasswordVerificationRequest();
                    request.MasterPasswordHash = keyHash;
                    try
                    {
                        await _apiService.PostAccountVerifyPasswordAsync(request);
                        passwordValid = true;
                        var localKeyHash = await _cryptoService.HashPasswordAsync(MasterPassword, key, HashPurpose.LocalAuthorization);
                        await _cryptoService.SetKeyHashAsync(localKeyHash);
                    }
                    catch (Exception e)
                    {
                        System.Diagnostics.Debug.WriteLine(">>> {0}: {1}", e.GetType(), e.StackTrace);
                    }
                    await _deviceActionService.HideLoadingAsync();
                }
                if (passwordValid)
                {
                    if (_pinSet.Item1)
                    {
                        var protectedPin = await _storageService.GetAsync<string>(Constants.ProtectedPin);
                        var encKey = await _cryptoService.GetEncKeyAsync(key);
                        var decPin = await _cryptoService.DecryptToUtf8Async(new EncString(protectedPin), encKey);
                        var pinKey = await _cryptoService.MakePinKeyAysnc(decPin, _email,
                            kdf.GetValueOrDefault(KdfType.PBKDF2_SHA256), kdfIterations.GetValueOrDefault(5000));
                        _vaultTimeoutService.PinProtectedKey = await _cryptoService.EncryptAsync(key.Key, pinKey);
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

        public async Task LogOutAsync()
        {
            var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.LogoutConfirmation,
                AppResources.LogOut, AppResources.Yes, AppResources.Cancel);
            if (confirmed)
            {
                _messagingService.Send("logout");
            }
        }

        public void TogglePassword()
        {
            ShowPassword = !ShowPassword;
            var page = (Page as LockPage);
            var entry = PinLock ? page.PinEntry : page.MasterPasswordEntry;
            entry.Focus();
        }

        public async Task PromptBiometricAsync()
        {
            BiometricIntegrityValid = await _biometricService.ValidateIntegrityAsync();
            if (!BiometricLock || !BiometricIntegrityValid)
            {
                return;
            }
            var success = await _platformUtilsService.AuthenticateBiometricAsync(null,
            PinLock ? AppResources.PIN : AppResources.MasterPassword, () =>
            {
                var page = Page as LockPage;
                if (PinLock)
                {
                    page.PinEntry.Focus();
                }
                else
                {
                    page.MasterPasswordEntry.Focus();
                }
            });
            _vaultTimeoutService.BiometricLocked = !success;
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
            _vaultTimeoutService.BiometricLocked = false;
            var disableFavicon = await _storageService.GetAsync<bool?>(Constants.DisableFaviconKey);
            await _stateService.SaveAsync(Constants.DisableFaviconKey, disableFavicon.GetValueOrDefault());
            _messagingService.Send("unlocked");
            UnlockedAction?.Invoke();
        }
    }
}
