using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public class AccessibilityServicePageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;

        private bool _enabled;
        private bool _permitted;

        public AccessibilityServicePageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            PageTitle = AppResources.AutofillAccessibilityService;
        }

        public bool Enabled
        {
            get => _enabled;
            set => SetProperty(ref _enabled, value,
                additionalPropertyNames: new string[]
                {
                    nameof(EnabledWithoutPermission),
                    nameof(EnabledAndPermitted)
                });
        }

        public bool Permitted
        {
            get => _permitted;
            set => SetProperty(ref _permitted, value,
                additionalPropertyNames: new string[]
                {
                    nameof(EnabledWithoutPermission),
                    nameof(EnabledAndPermitted)
                });
        }

        public bool EnabledWithoutPermission => Enabled && !Permitted;

        public bool EnabledAndPermitted => Enabled && Permitted;

        public void OpenSettings()
        {
            _deviceActionService.OpenAccessibilitySettings();
        }

        public void OpenOverlayPermissionSettings()
        {
            _deviceActionService.OpenAccessibilityOverlayPermissionSettings();
        }

        public void UpdateEnabled()
        {
            Enabled = _deviceActionService.AutofillAccessibilityServiceRunning();
        }

        public void UpdatePermitted()
        {
            Permitted = _deviceActionService.AutofillAccessibilityOverlayPermitted();
        }
    }
}
