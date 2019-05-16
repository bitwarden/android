using Bit.App.Abstractions;
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

        private string _email;
        private bool _showPassword;
        private bool _pinLock;
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

            PageTitle = AppResources.VerifyMasterPassword;
            TogglePasswordCommand = new Command(TogglePassword);
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

        public Command TogglePasswordCommand { get; }
        public string ShowPasswordIcon => ShowPassword ? "" : "";
        public string MasterPassword { get; set; }
        public string Pin { get; set; }

        public async Task InitAsync()
        {
            _pinSet = await _lockService.IsPinLockSetAsync();
            var hasKey = await _cryptoService.HasKeyAsync();
            PinLock = (_pinSet.Item1 && hasKey) || _pinSet.Item2;
            _email = await _userService.GetEmailAsync();
            PageTitle = PinLock ? AppResources.VerifyPIN : AppResources.VerifyMasterPassword;
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

            var kdf = await _userService.GetKdfAsync();
            var kdfIterations = await _userService.GetKdfIterationsAsync();

            if(PinLock)
            {
                var failed = true;
                try
                {
                    if(_pinSet.Item1)
                    {
                        var protectedPin = await _storageService.GetAsync<string>(Constants.ProtectedPin);
                        var decPin = await _cryptoService.DecryptToUtf8Async(new CipherString(protectedPin));
                        failed = decPin != Pin;
                        _lockService.PinLocked = failed;
                        if(!failed)
                        {
                            DoContinue();
                        }
                    }
                    else
                    {
                        var key = await _cryptoService.MakeKeyFromPinAsync(Pin, _email,
                            kdf.GetValueOrDefault(KdfType.PBKDF2_SHA256), kdfIterations.GetValueOrDefault(5000));
                        failed = false;
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
                if(storedKeyHash != null && keyHash != null && storedKeyHash == keyHash)
                {
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

        private async Task SetKeyAndContinueAsync(SymmetricCryptoKey key)
        {
            await _cryptoService.SetKeyAsync(key);
            DoContinue();
        }

        private void DoContinue()
        {
            _messagingService.Send("unlocked");
            Application.Current.MainPage = new TabsPage();
        }
    }
}
