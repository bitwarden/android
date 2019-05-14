using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Utilities;
using System.Threading.Tasks;

namespace Bit.App.Pages
{
    public class SyncPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly ISyncService _syncService;

        public SyncPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");

            PageTitle = AppResources.Sync;
        }

        public async Task SyncAsync()
        {
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Syncing);
                await _syncService.FullSyncAsync(true);
                await _deviceActionService.HideLoadingAsync();
                _platformUtilsService.ShowToast("success", null, AppResources.SyncingComplete);
            }
            catch(ApiException e)
            {
                await _deviceActionService.HideLoadingAsync();
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred, e.Error.GetSingleMessage(), AppResources.Ok);
            }
        }
    }
}
