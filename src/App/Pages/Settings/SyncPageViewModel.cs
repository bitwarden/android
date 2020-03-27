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

        private string _lastSync = "--";

        public SyncPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");

            PageTitle = AppResources.Sync;
        }

        public string LastSync
        {
            get => _lastSync;
            set => SetProperty(ref _lastSync, value);
        }

        public async Task SetLastSyncAsync()
        {
            var last = await _syncService.GetLastSyncAsync();
            if (last != null)
            {
                var localDate = last.Value.ToLocalTime();
                LastSync = string.Format("{0} {1}", localDate.ToShortDateString(), localDate.ToShortTimeString());
            }
            else
            {
                LastSync = AppResources.Never;
            }
        }

        public async Task SyncAsync()
        {
            if (Xamarin.Essentials.Connectivity.NetworkAccess == Xamarin.Essentials.NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return;
            }
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Syncing);
                var success = await _syncService.FullSyncAsync(true);
                await _deviceActionService.HideLoadingAsync();
                if (success)
                {
                    await SetLastSyncAsync();
                    _platformUtilsService.ShowToast("success", null, AppResources.SyncingComplete);
                }
                else
                {
                    await Page.DisplayAlert(null, AppResources.SyncingFailed, AppResources.Ok);
                }
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
    }
}
