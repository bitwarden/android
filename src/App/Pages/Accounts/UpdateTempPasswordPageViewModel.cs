using System;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class UpdateTempPasswordPageViewModel : BaseChangePasswordViewModel
    {
        public UpdateTempPasswordPageViewModel()
        {
            PageTitle = AppResources.UpdateMasterPassword;
            TogglePasswordCommand = new Command(TogglePassword);
            ToggleConfirmPasswordCommand = new Command(ToggleConfirmPassword);
            SubmitCommand = new Command(async () => await SubmitAsync());
        }

        public Command SubmitCommand { get; }
        public Command TogglePasswordCommand { get; }
        public Command ToggleConfirmPasswordCommand { get; }
        public Action UpdateTempPasswordSuccessAction { get; set; }
        public Action LogOutAction { get; set; }
        public string CurrentMasterPassword { get; set; }
        public ForcePasswordResetReason Reason { get; set; } = ForcePasswordResetReason.AdminForcePasswordReset;

        public override async Task InitAsync(bool forceSync = false)
        {
            await base.InitAsync(forceSync);
            
            var forcePasswordResetReason = await _stateService.GetForcePasswordResetReasonAsync();

            if (forcePasswordResetReason.HasValue)
            {
                Reason = forcePasswordResetReason.Value;
            }
        }

        public bool RequireCurrentPassword
        {
            get => Reason == ForcePasswordResetReason.WeakMasterPasswordOnLogin;
        }

        public string UpdateMasterPasswordWarningText
        {
            get
            {
                return Reason == ForcePasswordResetReason.WeakMasterPasswordOnLogin
                    ? _i18nService.T("UpdateWeakMasterPasswordWarning")
                    : _i18nService.T("UpdateMasterPasswordWarning");
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

            // Retrieve details for key generation
            var kdfConfig = await _stateService.GetActiveUserCustomDataAsync(a => new KdfConfig(a?.Profile));
            var email = await _stateService.GetEmailAsync();

            // Create new key and hash new password
            var key = await _cryptoService.MakeKeyAsync(MasterPassword, email, kdfConfig);
            var masterPasswordHash = await _cryptoService.HashPasswordAsync(MasterPassword, key);

            // Create new encKey for the User
            var newEncKey = await _cryptoService.RemakeEncKeyAsync(key);

            // Create request
            var request = new UpdateTempPasswordRequest
            {
                Key = newEncKey.Item2.EncryptedString,
                NewMasterPasswordHash = masterPasswordHash,
                MasterPasswordHint = Hint
            };

            // Initiate API action
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.UpdatingPassword);
                await _apiService.PutUpdateTempPasswordAsync(request);
                await _deviceActionService.HideLoadingAsync();

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
    }
}
