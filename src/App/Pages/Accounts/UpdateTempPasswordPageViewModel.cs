using System;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class UpdateTempPasswordPageViewModel : BaseChangePasswordViewModel
    {
        private readonly IUserVerificationService _userVerificationService;

        private ForcePasswordResetReason _reason = ForcePasswordResetReason.AdminForcePasswordReset;

        public UpdateTempPasswordPageViewModel()
        {
            PageTitle = AppResources.UpdateMasterPassword;
            TogglePasswordCommand = new Command(TogglePassword);
            ToggleConfirmPasswordCommand = new Command(ToggleConfirmPassword);
            SubmitCommand = new AsyncCommand(SubmitAsync,
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);

            _userVerificationService = ServiceContainer.Resolve<IUserVerificationService>();
        }

        public AsyncCommand SubmitCommand { get; }
        public Command TogglePasswordCommand { get; }
        public Command ToggleConfirmPasswordCommand { get; }
        public Action UpdateTempPasswordSuccessAction { get; set; }
        public Action LogOutAction { get; set; }
        public string CurrentMasterPassword { get; set; }

        public override async Task InitAsync(bool forceSync = false)
        {
            await base.InitAsync(forceSync);

            var forcePasswordResetReason = await _stateService.GetForcePasswordResetReasonAsync();

            if (forcePasswordResetReason.HasValue)
            {
                _reason = forcePasswordResetReason.Value;
            }
        }

        public bool RequireCurrentPassword
        {
            get => _reason == ForcePasswordResetReason.WeakMasterPasswordOnLogin;
        }

        public string UpdateMasterPasswordWarningText
        {
            get
            {
                return _reason == ForcePasswordResetReason.WeakMasterPasswordOnLogin
                    ? AppResources.UpdateWeakMasterPasswordWarning
                    : AppResources.UpdateMasterPasswordWarning;
            }
        }

        public void TogglePassword()
        {
            ShowPassword = !ShowPassword;
            (Page as UpdateTempPasswordPage).MasterPasswordEntry.Focus();
        }

        public void ToggleConfirmPassword()
        {
            ShowPassword = !ShowPassword;
            (Page as UpdateTempPasswordPage).ConfirmMasterPasswordEntry.Focus();
        }

        public async Task SubmitAsync()
        {
            if (!await ValidateMasterPasswordAsync())
            {
                return;
            }

            if (RequireCurrentPassword &&
                !await _userVerificationService.VerifyUser(CurrentMasterPassword, VerificationType.MasterPassword))
            {
                return;
            }

            // Retrieve details for key generation
            var kdfConfig = await _stateService.GetActiveUserCustomDataAsync(a => new KdfConfig(a?.Profile));
            var email = await _stateService.GetEmailAsync();

            // Create new master key and hash new password
            var masterKey = await _cryptoService.MakeMasterKeyAsync(MasterPassword, email, kdfConfig);
            var masterPasswordHash = await _cryptoService.HashMasterKeyAsync(MasterPassword, masterKey);

            // Encrypt user key with new master key
            var (userKey, newProtectedUserKey) = await _cryptoService.EncryptUserKeyWithMasterKeyAsync(masterKey);

            // Initiate API action
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.UpdatingPassword);

                switch (_reason)
                {
                    case ForcePasswordResetReason.AdminForcePasswordReset:
                        await UpdateTempPasswordAsync(masterPasswordHash, newProtectedUserKey.EncryptedString);
                        break;
                    case ForcePasswordResetReason.WeakMasterPasswordOnLogin:
                        await UpdatePasswordAsync(masterPasswordHash, newProtectedUserKey.EncryptedString);
                        break;
                    default:
                        throw new ArgumentOutOfRangeException();
                }
                await _deviceActionService.HideLoadingAsync();

                // Clear the force reset password reason
                await _stateService.SetForcePasswordResetReasonAsync(null);

                _platformUtilsService.ShowToast(null, null, AppResources.UpdatedMasterPassword);

                UpdateTempPasswordSuccessAction?.Invoke();
            }
            catch (ApiException e)
            {
                await _deviceActionService.HideLoadingAsync();
                if (e?.Error != null)
                {
                    await _platformUtilsService.ShowDialogAsync(e.Error.GetSingleMessage(),
                        AppResources.AnErrorHasOccurred, AppResources.Ok);
                }
                else
                {
                    await _platformUtilsService.ShowDialogAsync(AppResources.UpdatePasswordError,
                        AppResources.AnErrorHasOccurred, AppResources.Ok);
                }
            }
        }

        private async Task UpdateTempPasswordAsync(string newMasterPasswordHash, string newEncKey)
        {
            var request = new UpdateTempPasswordRequest
            {
                Key = newEncKey,
                NewMasterPasswordHash = newMasterPasswordHash,
                MasterPasswordHint = Hint
            };

            await _apiService.PutUpdateTempPasswordAsync(request);
        }

        private async Task UpdatePasswordAsync(string newMasterPasswordHash, string newEncKey)
        {
            var currentPasswordHash = await _cryptoService.HashMasterKeyAsync(CurrentMasterPassword, null);

            var request = new PasswordRequest
            {
                MasterPasswordHash = currentPasswordHash,
                Key = newEncKey,
                NewMasterPasswordHash = newMasterPasswordHash,
                MasterPasswordHint = Hint
            };

            await _apiService.PostPasswordAsync(request);
        }
    }
}
