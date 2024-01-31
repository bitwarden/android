using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Request;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class RegisterPageViewModel : CaptchaProtectedViewModel, IPasswordStrengthable
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly II18nService _i18nService;
        private readonly IEnvironmentService _environmentService;
        private readonly IAuditService _auditService;
        private readonly IApiService _apiService;
        private readonly ICryptoService _cryptoService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private string _email;
        private string _masterPassword;
        private bool _showPassword;
        private bool _acceptPolicies;
        private bool _checkExposedMasterPassword;

        public RegisterPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _apiService = ServiceContainer.Resolve<IApiService>("apiService");
            _cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _i18nService = ServiceContainer.Resolve<II18nService>("i18nService");
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");
            _auditService = ServiceContainer.Resolve<IAuditService>();

            PageTitle = AppResources.CreateAccount;
            TogglePasswordCommand = new Command(TogglePassword);
            ToggleConfirmPasswordCommand = new Command(ToggleConfirmPassword);
            SubmitCommand = new Command(async () => await SubmitAsync());
            ShowTerms = !_platformUtilsService.IsSelfHost();
            PasswordStrengthViewModel = new PasswordStrengthViewModel(this);
            CheckExposedMasterPassword = true;
        }

        public ICommand PoliciesClickCommand => new Command<string>((url) =>
        {
            _platformUtilsService.LaunchUri(url);
        });

        public bool ShowPassword
        {
            get => _showPassword;
            set => SetProperty(ref _showPassword, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowPasswordIcon),
                    nameof(PasswordVisibilityAccessibilityText)
                });
        }

        public bool AcceptPolicies
        {
            get => _acceptPolicies;
            set => SetProperty(ref _acceptPolicies, value);
        }

        public bool CheckExposedMasterPassword
        {
            get => _checkExposedMasterPassword;
            set => SetProperty(ref _checkExposedMasterPassword, value);
        }

        public string MasterPassword
        {
            get => _masterPassword;
            set
            {
                SetProperty(ref _masterPassword, value);
                PasswordStrengthViewModel.CalculatePasswordStrength();
            }
        }

        public string Email
        {
            get => _email;
            set => SetProperty(ref _email, value);
        }

        public string Password => MasterPassword;
        public List<string> UserInputs => PasswordStrengthViewModel.GetPasswordStrengthUserInput(Email);
        public string MasterPasswordMininumCharactersDescription => string.Format(AppResources.YourMasterPasswordCannotBeRecoveredIfYouForgetItXCharactersMinimum,
                                                                            Constants.MasterPasswordMinimumChars);
        public PasswordStrengthViewModel PasswordStrengthViewModel { get; }
        public bool ShowTerms { get; set; }
        public Command SubmitCommand { get; }
        public Command TogglePasswordCommand { get; }
        public Command ToggleConfirmPasswordCommand { get; }
        public string ShowPasswordIcon => ShowPassword ? BitwardenIcons.EyeSlash : BitwardenIcons.Eye;
        public string PasswordVisibilityAccessibilityText => ShowPassword ? AppResources.PasswordIsVisibleTapToHide : AppResources.PasswordIsNotVisibleTapToShow;
        public string Name { get; set; }
        public string ConfirmMasterPassword { get; set; }
        public string Hint { get; set; }
        public Action RegistrationSuccess { get; set; }
        public Action CloseAction { get; set; }
        protected override II18nService i18nService => _i18nService;
        protected override IEnvironmentService environmentService => _environmentService;
        protected override IDeviceActionService deviceActionService => _deviceActionService;
        protected override IPlatformUtilsService platformUtilsService => _platformUtilsService;

        public async Task SubmitAsync(bool showLoading = true)
        {
            if (Xamarin.Essentials.Connectivity.NetworkAccess == Xamarin.Essentials.NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle, AppResources.Ok);
                return;
            }
            if (string.IsNullOrWhiteSpace(Email))
            {
                await _platformUtilsService.ShowDialogAsync(
                    string.Format(AppResources.ValidationFieldRequired, AppResources.EmailAddress),
                    AppResources.AnErrorHasOccurred,
                    AppResources.Ok);
                return;
            }
            if (!Email.Contains("@"))
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InvalidEmail, AppResources.AnErrorHasOccurred,
                    AppResources.Ok);
                return;
            }
            if (string.IsNullOrWhiteSpace(MasterPassword))
            {
                await _platformUtilsService.ShowDialogAsync(
                    string.Format(AppResources.ValidationFieldRequired, AppResources.MasterPassword),
                    AppResources.AnErrorHasOccurred,
                    AppResources.Ok);
                return;
            }
            if (MasterPassword.Length < Constants.MasterPasswordMinimumChars)
            {
                await _platformUtilsService.ShowDialogAsync(string.Format(AppResources.MasterPasswordLengthValMessageX, Constants.MasterPasswordMinimumChars),
                    AppResources.AnErrorHasOccurred, AppResources.Ok);
                return;
            }
            if (MasterPassword != ConfirmMasterPassword)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.MasterPasswordConfirmationValMessage,
                    AppResources.AnErrorHasOccurred, AppResources.Ok);
                return;
            }
            if (ShowTerms && !AcceptPolicies)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.AcceptPoliciesError,
                    AppResources.AnErrorHasOccurred, AppResources.Ok);
                return;
            }
            if (await IsPasswordWeakOrExposed())
            {
                return;
            }

            if (showLoading)
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.CreatingAccount);
            }

            Name = string.IsNullOrWhiteSpace(Name) ? null : Name;
            Email = Email.Trim().ToLower();
            var kdfConfig = new KdfConfig(KdfType.PBKDF2_SHA256, Constants.Pbkdf2Iterations, null, null);
            var newMasterKey = await _cryptoService.MakeMasterKeyAsync(MasterPassword, Email, kdfConfig);
            var (newUserKey, newProtectedUserKey) = await _cryptoService.EncryptUserKeyWithMasterKeyAsync(
                newMasterKey,
                await _cryptoService.MakeUserKeyAsync()
            );
            var hashedPassword = await _cryptoService.HashMasterKeyAsync(MasterPassword, newMasterKey);
            var (newPublicKey, newProtectedPrivateKey) = await _cryptoService.MakeKeyPairAsync(newUserKey);
            var request = new RegisterRequest
            {
                Email = Email,
                Name = Name,
                MasterPasswordHash = hashedPassword,
                MasterPasswordHint = Hint,
                Key = newProtectedUserKey.EncryptedString,
                Kdf = kdfConfig.Type,
                KdfIterations = kdfConfig.Iterations,
                KdfMemory = kdfConfig.Memory,
                KdfParallelism = kdfConfig.Parallelism,
                Keys = new KeysRequest
                {
                    PublicKey = newPublicKey,
                    EncryptedPrivateKey = newProtectedPrivateKey.EncryptedString
                },
                CaptchaResponse = _captchaToken,
            };

            // TODO: org invite?

            try
            {
                await _apiService.PostRegisterAsync(request);
                await _deviceActionService.HideLoadingAsync();
                _platformUtilsService.ShowToast("success", null, AppResources.AccountCreated,
                    new System.Collections.Generic.Dictionary<string, object>
                    {
                        ["longDuration"] = true
                    });
                RegistrationSuccess?.Invoke();
            }
            catch (ApiException e)
            {
                if (e?.Error != null && e.Error.CaptchaRequired)
                {
                    if (await HandleCaptchaAsync(e.Error.CaptchaSiteKey))
                    {
                        await SubmitAsync(false);
                        _captchaToken = null;
                    }
                    return;
                }
                await _deviceActionService.HideLoadingAsync();
                if (e?.Error != null)
                {
                    await _platformUtilsService.ShowDialogAsync(e.Error.GetSingleMessage(),
                        AppResources.AnErrorHasOccurred, AppResources.Ok);
                }
            }
        }

        public void TogglePassword()
        {
            ShowPassword = !ShowPassword;
            var entry = (Page as RegisterPage).MasterPasswordEntry;
            entry.Focus();
            entry.CursorPosition = String.IsNullOrEmpty(MasterPassword) ? 0 : MasterPassword.Length;
        }

        public void ToggleConfirmPassword()
        {
            ShowPassword = !ShowPassword;
            var entry = (Page as RegisterPage).ConfirmMasterPasswordEntry;
            entry.Focus();
            entry.CursorPosition = String.IsNullOrEmpty(ConfirmMasterPassword) ? 0 : ConfirmMasterPassword.Length;
        }

        private async Task<bool> IsPasswordWeakOrExposed()
        {
            try
            {
                var title = string.Empty;
                var message = string.Empty;
                var exposedPassword = CheckExposedMasterPassword ? await _auditService.PasswordLeakedAsync(MasterPassword) > 0 : false;
                var weakPassword = PasswordStrengthViewModel.PasswordStrengthLevel <= PasswordStrengthLevel.Weak;

                if (exposedPassword && weakPassword)
                {
                    title = AppResources.WeakAndExposedMasterPassword;
                    message = AppResources.WeakPasswordIdentifiedAndFoundInADataBreachAlertDescription;
                }
                else if (exposedPassword)
                {
                    title = AppResources.ExposedMasterPassword;
                    message = AppResources.PasswordFoundInADataBreachAlertDescription;
                }
                else if (weakPassword)
                {
                    title = AppResources.WeakMasterPassword;
                    message = AppResources.WeakPasswordIdentifiedUseAStrongPasswordToProtectYourAccount;
                }

                if (exposedPassword || weakPassword)
                {
                    return !await _platformUtilsService.ShowDialogAsync(message, title, AppResources.Yes, AppResources.No);
                }
            }
            catch (Exception ex)
            {
                HandleException(ex);
            }

            return false;
        }
    }
}
