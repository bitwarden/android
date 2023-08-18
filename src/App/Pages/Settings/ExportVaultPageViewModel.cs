using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class ExportVaultPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IFileService _fileService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly II18nService _i18nService;
        private readonly IExportService _exportService;
        private readonly IPolicyService _policyService;
        private readonly IUserVerificationService _userVerificationService;
        private readonly IApiService _apiService;
        private readonly ILogger _logger;

        private int _fileFormatSelectedIndex;
        private string _exportWarningMessage;
        private bool _showPassword;
        private string _secret;
        private byte[] _exportResult;
        private string _defaultFilename;
        private bool _initialized = false;
        private bool _useOTPVerification = false;
        private string _secretName;
        private string _instructionText;

        public ExportVaultPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _fileService = ServiceContainer.Resolve<IFileService>();
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _i18nService = ServiceContainer.Resolve<II18nService>("i18nService");
            _exportService = ServiceContainer.Resolve<IExportService>("exportService");
            _policyService = ServiceContainer.Resolve<IPolicyService>("policyService");
            _userVerificationService = ServiceContainer.Resolve<IUserVerificationService>();
            _apiService = ServiceContainer.Resolve<IApiService>("apiService");
            _logger = ServiceContainer.Resolve<ILogger>("logger");

            PageTitle = AppResources.ExportVault;
            TogglePasswordCommand = new Command(TogglePassword);
            ExportVaultCommand = new Command(async () => await ExportVaultAsync());

            FileFormatOptions = new List<KeyValuePair<string, string>>
            {
                new KeyValuePair<string, string>("json", ".json"),
                new KeyValuePair<string, string>("csv", ".csv"),
                new KeyValuePair<string, string>("encrypted_json", ".json (Encrypted)")
            };
        }

        public async Task InitAsync()
        {
            _initialized = true;
            FileFormatSelectedIndex = FileFormatOptions.FindIndex(k => k.Key == "json");
            DisablePrivateVaultPolicyEnabled = await _policyService.PolicyAppliesToUser(PolicyType.DisablePersonalVaultExport);
            UseOTPVerification = !await _userVerificationService.HasMasterPasswordAsync(true);

            if (UseOTPVerification)
            {
                InstructionText = _i18nService.T("ExportVaultOTPDescription");
                SecretName = _i18nService.T("VerificationCode");
            }
            else
            {
                InstructionText = _i18nService.T("ExportVaultMasterPasswordDescription");
                SecretName = _i18nService.T("MasterPassword");
            }

            UpdateWarning();
        }

        public List<KeyValuePair<string, string>> FileFormatOptions { get; set; }
        private bool _disabledPrivateVaultPolicyEnabled = false;

        public bool DisablePrivateVaultPolicyEnabled
        {
            get => _disabledPrivateVaultPolicyEnabled;
            set
            {
                SetProperty(ref _disabledPrivateVaultPolicyEnabled, value);
            }
        }

        public int FileFormatSelectedIndex
        {
            get => _fileFormatSelectedIndex;
            set { SetProperty(ref _fileFormatSelectedIndex, value); }
        }

        public string ExportWarningMessage
        {
            get => _exportWarningMessage;
            set { SetProperty(ref _exportWarningMessage, value); }
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

        public bool UseOTPVerification
        {
            get => _useOTPVerification;
            set => SetProperty(ref _useOTPVerification, value);
        }

        public string Secret
        {
            get => _secret;
            set => SetProperty(ref _secret, value);
        }

        public string SecretName
        {
            get => _secretName;
            set => SetProperty(ref _secretName, value);
        }

        public string InstructionText
        {
            get => _instructionText;
            set => SetProperty(ref _instructionText, value);
        }

        public Command TogglePasswordCommand { get; }

        public string ShowPasswordIcon => ShowPassword ? BitwardenIcons.EyeSlash : BitwardenIcons.Eye;
        public string PasswordVisibilityAccessibilityText => ShowPassword ? AppResources.PasswordIsVisibleTapToHide : AppResources.PasswordIsNotVisibleTapToShow;

        public void TogglePassword()
        {
            ShowPassword = !ShowPassword;
            (Page as ExportVaultPage).SecretEntry.Focus();
        }

        public Command ExportVaultCommand { get; }

        public async Task ExportVaultAsync()
        {
            bool userConfirmedExport = await _platformUtilsService.ShowDialogAsync(ExportWarningMessage,
                _i18nService.T("ExportVaultConfirmationTitle"), _i18nService.T("ExportVault"), _i18nService.T("Cancel"));

            if (!userConfirmedExport)
            {
                return;
            }

            var verificationType = await _userVerificationService.HasMasterPasswordAsync(true)
                ? VerificationType.MasterPassword
                : VerificationType.OTP;
            if (!await _userVerificationService.VerifyUser(Secret, verificationType))
            {
                return;
            }

            Secret = string.Empty;

            try
            {
                var data = await _exportService.GetExport(FileFormatOptions[FileFormatSelectedIndex].Key);
                var fileFormat = FileFormatOptions[FileFormatSelectedIndex].Key;
                fileFormat = fileFormat == "encrypted_json" ? "json" : fileFormat;

                _defaultFilename = _exportService.GetFileName(null, fileFormat);
                _exportResult = Encoding.UTF8.GetBytes(data);

                if (!_fileService.SaveFile(_exportResult, null, _defaultFilename, null))
                {
                    ClearResult();
                    await _platformUtilsService.ShowDialogAsync(_i18nService.T("ExportVaultFailure"));
                }
            }
            catch (Exception ex)
            {
                ClearResult();
                await _platformUtilsService.ShowDialogAsync(_i18nService.T("ExportVaultFailure"));
                System.Diagnostics.Debug.WriteLine(">>> {0}: {1}", ex.GetType(), ex.StackTrace);
                _logger.Exception(ex);
            }
        }

        public async Task RequestOTP()
        {
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Sending);
                await _apiService.PostAccountRequestOTP();
                await _deviceActionService.HideLoadingAsync();
                _platformUtilsService.ShowToast("success", null, AppResources.CodeSent);
            }
            catch (ApiException e)
            {
                await _deviceActionService.HideLoadingAsync();
                if (e?.Error != null)
                {
                    await _platformUtilsService.ShowDialogAsync(e.Error.GetSingleMessage(),
                        AppResources.AnErrorHasOccurred);
                }
            }

        }

        public async void SaveFileSelected(string contentUri, string filename)
        {
            if (_fileService.SaveFile(_exportResult, null, filename ?? _defaultFilename, contentUri))
            {
                ClearResult();
                _platformUtilsService.ShowToast("success", null, _i18nService.T("ExportVaultSuccess"));
                return;
            }

            ClearResult();
            await _platformUtilsService.ShowDialogAsync(_i18nService.T("ExportVaultFailure"));
        }

        public void UpdateWarning()
        {
            if (!_initialized)
            {
                return;
            }

            switch (FileFormatOptions[FileFormatSelectedIndex].Key)
            {
                case "encrypted_json":
                    ExportWarningMessage = _i18nService.T("EncExportKeyWarning") +
                        "\n\n" +
                        _i18nService.T("EncExportAccountWarning");
                    break;
                default:
                    ExportWarningMessage = _i18nService.T("ExportVaultWarning");
                    break;
            }
        }

        private void ClearResult()
        {
            _defaultFilename = null;
            _exportResult = null;
        }
    }
}
