using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public class SyncPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IStateService _stateService;
        private readonly ISyncService _syncService;
        private readonly ILocalizeService _localizeService;

        private string _lastSync = "--";
        private bool _inited;
        private bool _syncOnRefresh;

        public SyncPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _localizeService = ServiceContainer.Resolve<ILocalizeService>("localizeService");

            PageTitle = AppResources.Sync;
        }

        public bool EnableSyncOnRefresh
        {
            get => _syncOnRefresh;
            set
            {
                if (SetProperty(ref _syncOnRefresh, value))
                {
                    var task = UpdateSyncOnRefreshAsync();
                }
            }
        }

        public string LastSync
        {
            get => _lastSync;
            set => SetProperty(ref _lastSync, value);
        }

        public async Task InitAsync()
        {
            await SetLastSyncAsync();
            EnableSyncOnRefresh = await _stateService.GetSyncOnRefreshAsync();
            _inited = true;
        }

        public async Task UpdateSyncOnRefreshAsync()
        {
            if (_inited)
            {
                await _stateService.SetSyncOnRefreshAsync(_syncOnRefresh);
            }
        }

        public async Task SetLastSyncAsync()
        {
            var last = await _syncService.GetLastSyncAsync();
            if (last != null)
            {
                var localDate = last.Value.ToLocalTime();
                LastSync = string.Format("{0} {1}",
                    _localizeService.GetLocaleShortDate(localDate),
                    _localizeService.GetLocaleShortTime(localDate));
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
                await _syncService.SyncPasswordlessLoginRequestsAsync();
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
