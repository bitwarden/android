using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Request;
using Bit.Core.Utilities;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class RegisterPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IApiService _apiService;
        private readonly ICryptoService _cryptoService;

        private bool _showPassword;

        public RegisterPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _apiService = ServiceContainer.Resolve<IApiService>("apiService");
            _cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");

            PageTitle = AppResources.Bitwarden;
            TogglePasswordCommand = new Command(TogglePassword);
            ToggleConfirmPasswordCommand = new Command(ToggleConfirmPassword);
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

        public Command TogglePasswordCommand { get; }
        public Command ToggleConfirmPasswordCommand { get; }
        public string ShowPasswordIcon => ShowPassword ? "" : "";
        public string Name { get; set; }
        public string Email { get; set; }
        public string MasterPassword { get; set; }
        public string ConfirmMasterPassword { get; set; }
        public string Hint { get; set; }

        public async Task SubmitAsync()
        {
            if(string.IsNullOrWhiteSpace(Email))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.EmailAddress),
                    AppResources.Ok);
                return;
            }
            if(!Email.Contains("@"))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred, AppResources.InvalidEmail, AppResources.Ok);
                return;
            }
            if(string.IsNullOrWhiteSpace(MasterPassword))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.MasterPassword),
                    AppResources.Ok);
                return;
            }
            if(MasterPassword.Length < 8)
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred,
                    AppResources.MasterPasswordLengthValMessage, AppResources.Ok);
                return;
            }
            if(MasterPassword != ConfirmMasterPassword)
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred,
                    AppResources.MasterPasswordConfirmationValMessage, AppResources.Ok);
                return;
            }

            // TODO: Password strength check?

            Name = string.IsNullOrWhiteSpace(Name) ? null : Name;
            Email = Email.Trim().ToLower();
            var kdf = KdfType.PBKDF2_SHA256;
            var kdfIterations = 100_000;
            var key = await _cryptoService.MakeKeyAsync(MasterPassword, Email, kdf, kdfIterations);
            var encKey = await _cryptoService.MakeEncKeyAsync(key);
            var hashedPassword = await _cryptoService.HashPasswordAsync(MasterPassword, key);
            var keys = await _cryptoService.MakeKeyPairAsync(encKey.Item1);
            var request = new RegisterRequest
            {
                Email = Email,
                Name = Name,
                MasterPasswordHash = hashedPassword,
                MasterPasswordHint = Hint,
                Key = encKey.Item2.EncryptedString,
                Kdf = kdf,
                KdfIterations = kdfIterations,
                Keys = new KeysRequest
                {
                    PublicKey = keys.Item1,
                    EncryptedPrivateKey = keys.Item2.EncryptedString
                }
            };
            // TODO: org invite?

            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.LoggingIn);
                await _apiService.PostRegisterAsync(request);
                await _deviceActionService.HideLoadingAsync();
                // TODO: dismiss this page from home page and pass email for login page
            }
            catch(ApiException e)
            {
                await _deviceActionService.HideLoadingAsync();
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred, e.Error.GetSingleMessage(), AppResources.Ok);
            }
        }

        public void TogglePassword()
        {
            ShowPassword = !ShowPassword;
            (Page as RegisterPage).MasterPasswordEntry.Focus();
        }

        public void ToggleConfirmPassword()
        {
            ShowPassword = !ShowPassword;
            (Page as RegisterPage).ConfirmMasterPasswordEntry.Focus();
        }
    }
}
