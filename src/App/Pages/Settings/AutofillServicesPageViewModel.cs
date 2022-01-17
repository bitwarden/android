using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.App.Services;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public class AutofillServicesPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IStateService _stateService;
        private readonly MobileI18nService _i18nService;
        
        private bool _autofillServiceToggled;
        private bool _inlineAutofillToggled;
        private bool _accessibilityToggled;
        private bool _drawOverToggled;
        private bool _inited;

        public AutofillServicesPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _i18nService = ServiceContainer.Resolve<II18nService>("i18nService") as MobileI18nService;
            PageTitle = AppResources.AutofillServices;
        }
        
        #region Autofill Service
        
        public bool AutofillServiceVisible
        {
            get => _deviceActionService.SystemMajorVersion() >= 26;
        }

        public bool AutofillServiceToggled
        {
            get => _autofillServiceToggled;
            set => SetProperty(ref _autofillServiceToggled, value,
                additionalPropertyNames: new string[]
                {
                    nameof(InlineAutofillEnabled)
                });
        }
        
        #endregion
        
        #region Inline Autofill

        public bool InlineAutofillVisible
        {
            get => _deviceActionService.SystemMajorVersion() >= 30;
        }
        
        public bool InlineAutofillEnabled
        {
            get => AutofillServiceToggled;
        }

        public bool InlineAutofillToggled
        {
            get => _inlineAutofillToggled;
            set
            {
                if (SetProperty(ref _inlineAutofillToggled, value))
                {
                    var task = UpdateInlineAutofillToggledAsync();
                }
            }
        }
        
        #endregion
        
        #region Accessibility

        public string AccessibilityDescriptionLabel
        {
            get
            {
                if (_deviceActionService.SystemMajorVersion() <= 22)
                {
                    // Android 5
                    return _i18nService.T("AccessibilityDescription");
                }
                if (_deviceActionService.SystemMajorVersion() == 23)
                {
                    // Android 6
                    return _i18nService.T("AccessibilityDescription2");
                }
                if (_deviceActionService.SystemMajorVersion() == 24 || _deviceActionService.SystemMajorVersion() == 25)
                {
                    // Android 7
                    return _i18nService.T("AccessibilityDescription3");
                }
                // Android 8+
                return _i18nService.T("AccessibilityDescription4");
            }
        }
        
        public bool AccessibilityToggled
        {
            get => _accessibilityToggled;
            set => SetProperty(ref _accessibilityToggled, value,
                additionalPropertyNames: new string[]
                {
                    nameof(DrawOverEnabled)
                });
        }

        #endregion
        
        #region Draw-Over
        
        public bool DrawOverVisible
        {
            get => _deviceActionService.SystemMajorVersion() >= 23;
        }

        public string DrawOverDescriptionLabel
        {
            get
            {
                if (_deviceActionService.SystemMajorVersion() <= 23)
                {
                    // Android 6
                    return _i18nService.T("DrawOverDescription");
                }
                if (_deviceActionService.SystemMajorVersion() == 24 || _deviceActionService.SystemMajorVersion() == 25)
                {
                    // Android 7
                    return _i18nService.T("DrawOverDescription2");
                }
                // Android 8+
                return _i18nService.T("DrawOverDescription3");
            }
        }
        
        public bool DrawOverEnabled
        {
            get => AccessibilityToggled;
        }
        
        public bool DrawOverToggled
        {
            get => _drawOverToggled;
            set => SetProperty(ref _drawOverToggled, value);
        }

        #endregion
        
        public async Task InitAsync()
        {
            InlineAutofillToggled = await _stateService.GetInlineAutofillEnabledAsync() ?? true;
            _inited = true;
        }

        public void ToggleAutofillService()
        {
            if (!AutofillServiceToggled)
            {
                _deviceActionService.OpenAutofillSettings();
            }
            else
            {
                _deviceActionService.DisableAutofillService();
            }
        }

        public void ToggleInlineAutofill()
        {
            if (!InlineAutofillEnabled)
            {
                return;
            }
            InlineAutofillToggled = !InlineAutofillToggled;
        }

        public void ToggleAccessibility()
        {
            _deviceActionService.OpenAccessibilitySettings();
        }

        public void ToggleDrawOver()
        {
            if (!DrawOverEnabled)
            {
                return;
            }
            _deviceActionService.OpenAccessibilityOverlayPermissionSettings();
        }
        
        public void UpdateEnabled()
        {
            AutofillServiceToggled = _deviceActionService.AutofillServiceEnabled();
            AccessibilityToggled = _deviceActionService.AutofillAccessibilityServiceRunning();
            DrawOverToggled = _deviceActionService.AutofillAccessibilityOverlayPermitted();
        }
        
        private async Task UpdateInlineAutofillToggledAsync()
        {
            if (_inited)
            {
                await _stateService.SetInlineAutofillEnabledAsync(InlineAutofillToggled);
            }
        }
    }
}
