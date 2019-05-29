using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System.Threading.Tasks;

namespace Bit.App.Pages
{
    public class OptionsPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IStorageService _storageService;
        private readonly ITotpService _totpService;
        private readonly IStateService _stateService;

        private bool _disableFavicon;
        private bool _disableAutoTotpCopy;

        public OptionsPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _totpService = ServiceContainer.Resolve<ITotpService>("totpService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");

            PageTitle = AppResources.Options;
        }

        public bool DisableFavicon
        {
            get => _disableFavicon;
            set
            {
                if(SetProperty(ref _disableFavicon, value))
                {
                    var task = UpdateDisableFaviconAsync();
                }
            }
        }

        public bool DisableAutoTotpCopy
        {
            get => _disableAutoTotpCopy;
            set
            {
                if(SetProperty(ref _disableAutoTotpCopy, value))
                {
                    var task = UpdateAutoTotpCopyAsync();
                }
            }
        }

        public async Task InitAsync()
        {
            DisableAutoTotpCopy = !(await _totpService.IsAutoCopyEnabledAsync());
            DisableFavicon = await _storageService.GetAsync<bool>(Constants.DisableFaviconKey);
        }

        private async Task UpdateAutoTotpCopyAsync()
        {
            await _storageService.SaveAsync(Constants.DisableAutoTotpCopyKey, DisableAutoTotpCopy);
        }

        private async Task UpdateDisableFaviconAsync()
        {
            await _storageService.SaveAsync(Constants.DisableFaviconKey, DisableFavicon);
            await _stateService.SaveAsync(Constants.DisableFaviconKey, DisableFavicon);
        }
    }
}
