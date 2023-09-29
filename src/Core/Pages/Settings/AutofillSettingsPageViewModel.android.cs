using System.Threading.Tasks;
using System.Windows.Input;
using Bit.Core.Resources.Localization;
using Microsoft.Maui.ApplicationModel;
using Microsoft.Maui.Controls;
using Microsoft.Maui;
using Bit.App.Utilities;

namespace Bit.App.Pages
{
    public partial class AutofillSettingsPageViewModel
    {
        private bool _useAutofillServices;
        private bool _useInlineAutofill;
        private bool _useAccessibility;
        private bool _useDrawOver;
        private bool _askToAddLogin;

        public bool SupportsAndroidAutofillServices => // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
Device.RuntimePlatform == Device.Android && _deviceActionService.SupportsAutofillServices();

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

        public bool ShowUseAccessibilityToggle => // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
Device.RuntimePlatform == Device.Android;

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

        public AsyncCommand ToggleUseAutofillServicesCommand { get; private set; }
        public AsyncCommand ToggleUseInlineAutofillCommand { get; private set; }
        public AsyncCommand ToggleUseAccessibilityCommand { get; private set; }
        public AsyncCommand ToggleUseDrawOverCommand { get; private set; }
        public AsyncCommand ToggleAskToAddLoginCommand { get; private set; }
        public ICommand GoToBlockAutofillUrisCommand { get; private set; }

        private void InitAndroidCommands()
        {
            ToggleUseAutofillServicesCommand = CreateDefaultAsyncCommnad(() => MainThread.InvokeOnMainThreadAsync(() => ToggleUseAutofillServices()), () => _inited);
            ToggleUseInlineAutofillCommand = CreateDefaultAsyncCommnad(() => MainThread.InvokeOnMainThreadAsync(() => ToggleUseInlineAutofillEnabledAsync()), () => _inited);
            ToggleUseAccessibilityCommand = CreateDefaultAsyncCommnad(ToggleUseAccessibilityAsync, () => _inited);
            ToggleUseDrawOverCommand = CreateDefaultAsyncCommnad(() => MainThread.InvokeOnMainThreadAsync(() => ToggleDrawOver()), () => _inited);
            ToggleAskToAddLoginCommand = CreateDefaultAsyncCommnad(ToggleAskToAddLoginAsync, () => _inited);
            GoToBlockAutofillUrisCommand = CreateDefaultAsyncCommnad(() => Page.Navigation.PushAsync(new BlockAutofillUrisPage()));
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
