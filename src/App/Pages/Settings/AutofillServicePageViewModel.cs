using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public class AutofillServicePageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;

        private bool _enabled;

        public AutofillServicePageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            PageTitle = AppResources.AutofillService;
        }

        public bool Enabled
        {
            get => _enabled;
            set => SetProperty(ref _enabled, value);
        }

        public void OpenSettings()
        {
            _deviceActionService.OpenAutofillSettings();
        }

        public void UpdateEnabled()
        {
            Enabled = _deviceActionService.AutofillServiceEnabled();
        }
    }
}
