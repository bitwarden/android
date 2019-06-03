using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public class AccessibilityServicePageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IPlatformUtilsService _platformUtilsService;

        private bool _enabled;

        public AccessibilityServicePageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            PageTitle = AppResources.AutofillAccessibilityService;
        }

        public bool Enabled
        {
            get => _enabled;
            set => SetProperty(ref _enabled, value);
        }

        public void OpenSettings()
        {
            _deviceActionService.OpenAccessibilitySettings();
        }

        public void UpdateEnabled()
        {
            Enabled = _deviceActionService.AutofillAccessibilityServiceRunning();
        }
    }
}
