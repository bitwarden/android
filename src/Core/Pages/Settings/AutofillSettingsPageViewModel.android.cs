using System.Windows.Input;
using Bit.Core.Resources.Localization;
using CommunityToolkit.Mvvm.Input;

namespace Bit.App.Pages
{
    public partial class AutofillSettingsPageViewModel
    {
        private bool _useAutofillServices;
        private bool _useInlineAutofill;
        private bool _useAccessibility;
        private bool _useDrawOver;
        private bool _askToAddLogin;

        public bool SupportsCredentialProviderService => DeviceInfo.Platform == DevicePlatform.Android && _deviceActionService.SupportsCredentialProviderService();

        public bool SupportsAndroidAutofillServices => DeviceInfo.Platform == DevicePlatform.Android && _deviceActionService.SupportsAutofillServices();

        public bool UseAutofillServices
        {
            get => _useAutofillServices;
            set
            {
                if (SetProperty(ref _useAutofillServices, value))
                {
                    ((ICommand)ToggleUseAutofillServicesCommand).Execute(null);
                }
            }
        }

        public bool ShowUseInlineAutofillToggle => _deviceActionService.SupportsInlineAutofill();

        public bool UseInlineAutofill
        {
            get => _useInlineAutofill;
            set
            {
                if (SetProperty(ref _useInlineAutofill, value))
                {
                    ((ICommand)ToggleUseInlineAutofillCommand).Execute(null);
                }
            }
        }

        public bool ShowUseAccessibilityToggle => DeviceInfo.Platform == DevicePlatform.Android;

        public string UseAccessibilityDescription => _deviceActionService.GetAutofillAccessibilityDescription();

        public bool UseAccessibility
        {
            get => _useAccessibility;
            set
            {
                if (SetProperty(ref _useAccessibility, value))
                {
                    ((ICommand)ToggleUseAccessibilityCommand).Execute(null);
                }
            }
        }

        public bool ShowUseDrawOverToggle => _deviceActionService.SupportsDrawOver();

        public bool UseDrawOver
        {
            get => _useDrawOver;
            set
            {
                if (SetProperty(ref _useDrawOver, value))
                {
                    ((ICommand)ToggleUseDrawOverCommand).Execute(null);
                }
            }
        }

        public string UseDrawOverDescription => _deviceActionService.GetAutofillDrawOverDescription();

        public bool AskToAddLogin
        {
            get => _askToAddLogin;
            set
            {
                if (SetProperty(ref _askToAddLogin, value))
                {
                    ((ICommand)ToggleAskToAddLoginCommand).Execute(null);
                }
            }
        }

        public AsyncRelayCommand ToggleUseAutofillServicesCommand { get; private set; }
        public AsyncRelayCommand ToggleUseInlineAutofillCommand { get; private set; }
        public AsyncRelayCommand ToggleUseAccessibilityCommand { get; private set; }
        public AsyncRelayCommand ToggleUseDrawOverCommand { get; private set; }
        public AsyncRelayCommand ToggleAskToAddLoginCommand { get; private set; }
        public ICommand GoToBlockAutofillUrisCommand { get; private set; }
        public ICommand GoToCredentialProviderSettingsCommand { get; private set; }

        private void InitAndroidCommands()
        {
            ToggleUseAutofillServicesCommand = CreateDefaultAsyncRelayCommand(() => MainThread.InvokeOnMainThreadAsync(() => ToggleUseAutofillServices()), () => _inited, allowsMultipleExecutions: false);
            ToggleUseInlineAutofillCommand = CreateDefaultAsyncRelayCommand(() => MainThread.InvokeOnMainThreadAsync(() => ToggleUseInlineAutofillEnabledAsync()), () => _inited, allowsMultipleExecutions: false);
            ToggleUseAccessibilityCommand = CreateDefaultAsyncRelayCommand(ToggleUseAccessibilityAsync, () => _inited, allowsMultipleExecutions: false);
            ToggleUseDrawOverCommand = CreateDefaultAsyncRelayCommand(() => MainThread.InvokeOnMainThreadAsync(() => ToggleDrawOver()), () => _inited, allowsMultipleExecutions: false);
            ToggleAskToAddLoginCommand = CreateDefaultAsyncRelayCommand(ToggleAskToAddLoginAsync, () => _inited, allowsMultipleExecutions: false);
            GoToBlockAutofillUrisCommand = CreateDefaultAsyncRelayCommand(() => Page.Navigation.PushAsync(new BlockAutofillUrisPage()), allowsMultipleExecutions: false);
            GoToCredentialProviderSettingsCommand = CreateDefaultAsyncRelayCommand(() => MainThread.InvokeOnMainThreadAsync(() => GoToCredentialProviderSettings()), () => _inited, allowsMultipleExecutions: false);
        }

        private async Task InitAndroidAutofillSettingsAsync()
        {
            _useInlineAutofill = await _stateService.GetInlineAutofillEnabledAsync() ?? true;

            await UpdateAndroidAutofillSettingsAsync();

            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                TriggerPropertyChanged(nameof(UseInlineAutofill));
            });
        }

        private async Task UpdateAndroidAutofillSettingsAsync()
        {
            _useAutofillServices =
                _autofillHandler.SupportsAutofillService() && _autofillHandler.AutofillServiceEnabled();
            _useAccessibility = _autofillHandler.AutofillAccessibilityServiceRunning();
            _useDrawOver = _autofillHandler.AutofillAccessibilityOverlayPermitted();
            _askToAddLogin = await _stateService.GetAutofillDisableSavePromptAsync() != true;

            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                TriggerPropertyChanged(nameof(UseAutofillServices));
                TriggerPropertyChanged(nameof(UseAccessibility));
                TriggerPropertyChanged(nameof(UseDrawOver));
                TriggerPropertyChanged(nameof(AskToAddLogin));
            });
        }

        private async Task GoToCredentialProviderSettings()
        {
            var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.SetBitwardenAsPasskeyManagerDescription, AppResources.ContinueToDeviceSettings,
                AppResources.Continue,
                AppResources.Cancel);
            if (confirmed)
            {
                _deviceActionService.OpenCredentialProviderSettings();
            }
        }

        private void ToggleUseAutofillServices()
        {
            if (UseAutofillServices)
            {
                _deviceActionService.OpenAutofillSettings();
            }
            else
            {
                _autofillHandler.DisableAutofillService();
            }
        }

        private async Task ToggleUseInlineAutofillEnabledAsync()
        {
            await _stateService.SetInlineAutofillEnabledAsync(UseInlineAutofill);
        }

        private async Task ToggleUseAccessibilityAsync()
        {
            if (!_autofillHandler.AutofillAccessibilityServiceRunning()
                &&
                !await _platformUtilsService.ShowDialogAsync(AppResources.AccessibilityDisclosureText, AppResources.AccessibilityServiceDisclosure,
                    AppResources.Accept, AppResources.Decline))
            {
                _useAccessibility = false;
                await MainThread.InvokeOnMainThreadAsync(() => TriggerPropertyChanged(nameof(UseAccessibility)));
                return;
            }
            _deviceActionService.OpenAccessibilitySettings();
        }

        private void ToggleDrawOver()
        {
            if (!UseAccessibility)
            {
                return;
            }
            _deviceActionService.OpenAccessibilityOverlayPermissionSettings();
        }

        private async Task ToggleAskToAddLoginAsync()
        {
            await _stateService.SetAutofillDisableSavePromptAsync(!AskToAddLogin);
        }
    }
}
