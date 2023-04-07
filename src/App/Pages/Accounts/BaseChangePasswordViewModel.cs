using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;
using Xamarin.Essentials;

namespace Bit.App.Pages
{
    public class BaseChangePasswordViewModel : BaseViewModel
    {
        protected readonly IPlatformUtilsService _platformUtilsService;
        protected readonly IStateService _stateService;
        protected readonly IPolicyService _policyService;
        protected readonly IPasswordGenerationService _passwordGenerationService;
        protected readonly II18nService _i18nService;
        protected readonly ICryptoService _cryptoService;
        protected readonly IDeviceActionService _deviceActionService;
        protected readonly IApiService _apiService;
        protected readonly ISyncService _syncService;

        private bool _showPassword;
        private bool _isPolicyInEffect;
        private string _policySummary;
        private MasterPasswordPolicyOptions _policy;

        protected BaseChangePasswordViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _policyService = ServiceContainer.Resolve<IPolicyService>("policyService");
            _passwordGenerationService =
                ServiceContainer.Resolve<IPasswordGenerationService>("passwordGenerationService");
            _i18nService = ServiceContainer.Resolve<II18nService>("i18nService");
            _cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _apiService = ServiceContainer.Resolve<IApiService>("apiService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
        }

        public bool ShowPassword
        {
            get => _showPassword;
            set => SetProperty(ref _showPassword, value,
                additionalPropertyNames: new[]
                {
                    nameof(ShowPasswordIcon),
                    nameof(PasswordVisibilityAccessibilityText)
                });
        }

        public bool IsPolicyInEffect
        {
            get => _isPolicyInEffect;
            set => SetProperty(ref _isPolicyInEffect, value);
        }

        public string PolicySummary
        {
            get => _policySummary;
            set => SetProperty(ref _policySummary, value);
        }

        public MasterPasswordPolicyOptions Policy
        {
            get => _policy;
            set => SetProperty(ref _policy, value);
        }

        public string ShowPasswordIcon => ShowPassword ? "" : "";
        public string PasswordVisibilityAccessibilityText => ShowPassword ? AppResources.PasswordIsVisibleTapToHide : AppResources.PasswordIsNotVisibleTapToShow;
        public string MasterPassword { get; set; }
        public string ConfirmMasterPassword { get; set; }
        public string Hint { get; set; }

        public async Task InitAsync(bool forceSync = false)
        {
            if (forceSync)
            {
                var task = Task.Run(async () => await _syncService.FullSyncAsync(true));
                await task.ContinueWith(async (t) => await CheckPasswordPolicy());
            }
            else
            {
                await CheckPasswordPolicy();
            }
        }

        private async Task CheckPasswordPolicy()
        {
            Policy = await _policyService.GetMasterPasswordPolicyOptions();
            IsPolicyInEffect = Policy?.InEffect() ?? false;
            if (!IsPolicyInEffect)
            {
                return;
            }

            var bullet = "\n" + "".PadLeft(4) + "\u2022 ";
            var sb = new StringBuilder();
            sb.Append(_i18nService.T("MasterPasswordPolicyInEffect"));
            if (Policy.MinComplexity > 0)
            {
                sb.Append(bullet)
                    .Append(string.Format(_i18nService.T("PolicyInEffectMinComplexity"), Policy.MinComplexity));
            }
            if (Policy.MinLength > 0)
            {
                sb.Append(bullet).Append(string.Format(_i18nService.T("PolicyInEffectMinLength"), Policy.MinLength));
            }
            if (Policy.RequireUpper)
            {
                sb.Append(bullet).Append(_i18nService.T("PolicyInEffectUppercase"));
            }
            if (Policy.RequireLower)
            {
                sb.Append(bullet).Append(_i18nService.T("PolicyInEffectLowercase"));
            }
            if (Policy.RequireNumbers)
            {
                sb.Append(bullet).Append(_i18nService.T("PolicyInEffectNumbers"));
            }
            if (Policy.RequireSpecial)
            {
                sb.Append(bullet).Append(string.Format(_i18nService.T("PolicyInEffectSpecial"), "!@#$%^&*"));
            }
            PolicySummary = sb.ToString();
        }

        protected async Task<bool> ValidateMasterPasswordAsync()
        {
            if (Connectivity.NetworkAccess == NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle, AppResources.Ok);
                return false;
            }
            if (string.IsNullOrWhiteSpace(MasterPassword))
            {
                await _platformUtilsService.ShowDialogAsync(
                    string.Format(AppResources.ValidationFieldRequired, AppResources.MasterPassword),
                    AppResources.AnErrorHasOccurred, AppResources.Ok);
                return false;
            }
            if (IsPolicyInEffect)
            {
                var userInputs = _passwordGenerationService.GetPasswordStrengthUserInput(await _stateService.GetEmailAsync());
                var passwordStrength = _passwordGenerationService.PasswordStrength(MasterPassword, userInputs);
                if (!await _policyService.EvaluateMasterPassword(passwordStrength.Score, MasterPassword, Policy))
                {
                    await _platformUtilsService.ShowDialogAsync(AppResources.MasterPasswordPolicyValidationMessage,
                        AppResources.MasterPasswordPolicyValidationTitle, AppResources.Ok);
                    return false;
                }
            }
            else
            {
                if (MasterPassword.Length < Constants.MasterPasswordMinimumChars)
                {
                    await _platformUtilsService.ShowDialogAsync(string.Format(AppResources.MasterPasswordLengthValMessageX, Constants.MasterPasswordMinimumChars),
                        AppResources.MasterPasswordPolicyValidationTitle, AppResources.Ok);
                    return false;
                }
            }
            if (MasterPassword != ConfirmMasterPassword)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.MasterPasswordConfirmationValMessage,
                    AppResources.AnErrorHasOccurred, AppResources.Ok);
                return false;
            }

            return true;
        }
    }
}
