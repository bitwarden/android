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
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class LockPageViewModel : BaseViewModel
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly ILockService _lockService;
        private readonly ICryptoService _cryptoService;
        private readonly IStorageService _storageService;
        private readonly IUserService _userService;
        private readonly IMessagingService _messagingService;
        private readonly IStorageService _secureStorageService;
        private readonly IEnvironmentService _environmentService;
        private readonly IStateService _stateService;

        private string _email;
        private bool _showPassword;
        private bool _pinLock;
        private bool _fingerprintLock;
        private string _fingerprintButtonText;
        private string _loggedInAsText;
        private string _lockedVerifyText;
        private int _invalidPinAttempts = 0;
        private Tuple<bool, bool> _pinSet;

        public LockPageViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _lockService = ServiceContainer.Resolve<ILockService>("lockService");
            _cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _userService = ServiceContainer.Resolve<IUserService>("userService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _secureStorageService = ServiceContainer.Resolve<IStorageService>("secureStorageService");
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");

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

        public bool FingerprintLock
        {
            get => _fingerprintLock;
            set => SetProperty(ref _fingerprintLock, value);
        }

        public string FingerprintButtonText
        {
            get => _fingerprintButtonText;
            set => SetProperty(ref _fingerprintButtonText, value);
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

        public async Task InitAsync(bool autoPromptFingerprint)
        {
            _pinSet = await _lockService.IsPinLockSetAsync();
            PinLock = (_pinSet.Item1 && _lockService.PinProtectedKey != null) || _pinSet.Item2;
            FingerprintLock = await _lockService.IsFingerprintLockSetAsync();
            _email = await _userService.GetEmailAsync();
            var webVault = _environmentService.GetWebVaultUrl();
            if(string.IsNullOrWhiteSpace(webVault))
            {
                webVault = "https://bitwarden.com";
            }
            var webVaultHostname = CoreHelpers.GetHostname(webVault);
            LoggedInAsText = string.Format(AppResources.LoggedInAsOn, _email, webVaultHostname);
            if(PinLock)
            {
                PageTitle = AppResources.VerifyPIN;
                LockedVerifyText = AppResources.VaultLockedPIN;
            }
            else
            {
                PageTitle = AppResources.VerifyMasterPassword;
                LockedVerifyText = AppResources.VaultLockedMasterPassword;
            }

            if(FingerprintLock)
            {
                FingerprintButtonText = _deviceActionService.SupportsFaceId() ? AppResources.UseFaceIDToUnlock :
                    AppResources.UseFingerprintToUnlock;
                if(autoPromptFingerprint)
                {
                    var tasks = Task.Run(async () =>
                    {
                        await Task.Delay(500);
                        Device.BeginInvokeOnMainThread(async () => await PromptFingerprintAsync());
                    });
                }
            }
        }

        public async Task SubmitAsync()
        {
            if(PinLock && string.IsNullOrWhiteSpace(Pin))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.PIN),
                    AppResources.Ok);
                return;
            }
            if(!PinLock && string.IsNullOrWhiteSpace(MasterPassword))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.MasterPassword),
                    AppResources.Ok);
                return;
            }

            ShowPassword = false;
            var kdf = await _userService.GetKdfAsync();
            var kdfIterations = await _userService.GetKdfIterationsAsync();

            if(PinLock)
            {
                var failed = true;
                try
                {
                    if(_pinSet.Item1)
                    {
                        var key = await _cryptoService.MakeKeyFromPinAsync(Pin, _email,
                            kdf.GetValueOrDefault(KdfType.PBKDF2_SHA256), kdfIterations.GetValueOrDefault(5000),
                            _lockService.PinProtectedKey);
                        var encKey = await _cryptoService.GetEncKeyAsync(key);
                        var protectedPin = await _storageService.GetAsync<string>(Constants.ProtectedPin);
                        var decPin = await _cryptoService.DecryptToUtf8Async(new CipherString(protectedPin), encKey);
                        failed = decPin != Pin;
                        if(!failed)
                        {
                            Pin = string.Empty;
                            await SetKeyAndContinueAsync(key);
                        }
                    }
                    else
                    {
                        var key = await _cryptoService.MakeKeyFromPinAsync(Pin, _email,
                            kdf.GetValueOrDefault(KdfType.PBKDF2_SHA256), kdfIterations.GetValueOrDefault(5000));
                        failed = false;
                        Pin = string.Empty;
                        await SetKeyAndContinueAsync(key);
                    }
                }
                catch
                {
                    failed = true;
                }
                if(failed)
                {
                    _invalidPinAttempts++;
                    if(_invalidPinAttempts >= 5)
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
                var keyHash = await _cryptoService.HashPasswordAsync(MasterPassword, key);
                var storedKeyHash = await _cryptoService.GetKeyHashAsync();
                if(storedKeyHash == null)
                {
                    var oldKey = await _secureStorageService.GetAsync<string>("oldKey");
                    if(key.KeyB64 == oldKey)
                    {
                        await _secureStorageService.RemoveAsync("oldKey");
                        await _cryptoService.SetKeyHashAsync(keyHash);
                        storedKeyHash = keyHash;
                    }
                }
                if(storedKeyHash != null && keyHash != null && storedKeyHash == keyHash)
                {
                    if(_pinSet.Item1)
                    {
                        var protectedPin = await _storageService.GetAsync<string>(Constants.ProtectedPin);
                        var encKey = await _cryptoService.GetEncKeyAsync(key);
                        var decPin = await _cryptoService.DecryptToUtf8Async(new CipherString(protectedPin), encKey);
                        var pinKey = await _cryptoService.MakePinKeyAysnc(decPin, _email,
                            kdf.GetValueOrDefault(KdfType.PBKDF2_SHA256), kdfIterations.GetValueOrDefault(5000));
                        _lockService.PinProtectedKey = await _cryptoService.EncryptAsync(key.Key, pinKey);
                    }
                    MasterPassword = string.Empty;
                    await SetKeyAndContinueAsync(key);
                }
                else
                {
                    await _platformUtilsService.ShowDialogAsync(AppResources.InvalidMasterPassword,
                        AppResources.AnErrorHasOccurred);
                }
            }
        }

        public async Task LogOutAsync()
        {
            var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.LogoutConfirmation,
                AppResources.LogOut, AppResources.Yes, AppResources.Cancel);
            if(confirmed)
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

        public async Task PromptFingerprintAsync()
        {
            if(!FingerprintLock)
            {
                return;
            }
            var success = await _platformUtilsService.AuthenticateFingerprintAsync(null,
            PinLock ? AppResources.PIN : AppResources.MasterPassword, () =>
            {
                var page = Page as LockPage;
                if(PinLock)
                {
                    page.PinEntry.Focus();
                }
                else
                {
                    page.MasterPasswordEntry.Focus();
                }
            });
            _lockService.FingerprintLocked = !success;
            if(success)
            {
                await DoContinueAsync();
            }
        }

        private async Task SetKeyAndContinueAsync(SymmetricCryptoKey key)
        {
            var hasKey = await _cryptoService.HasKeyAsync();
            if(!hasKey)
            {
                await _cryptoService.SetKeyAsync(key);
            }
            await DoContinueAsync();
        }

        private async Task DoContinueAsync()
        {
            _lockService.FingerprintLocked = false;
            var disableFavicon = await _storageService.GetAsync<bool?>(Constants.DisableFaviconKey);
            await _stateService.SaveAsync(Constants.DisableFaviconKey, disableFavicon.GetValueOrDefault());
            _messagingService.Send("unlocked");
            UnlockedAction?.Invoke();
        }
    }
}
