using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class OtherSettingsPageViewModel : BaseViewModel
    {
        private const int CLEAR_CLIPBOARD_NEVER_OPTION = -1;

        private readonly IDeviceActionService _deviceActionService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IStateService _stateService;
        private readonly ISyncService _syncService;
        private readonly ILocalizeService _localizeService;
        private readonly IWatchDeviceService _watchDeviceService;
        private readonly ILogger _logger;

        private string _lastSyncDisplay = "--";
        private bool _inited;
        private bool _syncOnRefresh;
        private bool _isScreenCaptureAllowed;
        private bool _shouldConnectToWatch;

        public OtherSettingsPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>();
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>();
            _stateService = ServiceContainer.Resolve<IStateService>();
            _syncService = ServiceContainer.Resolve<ISyncService>();
            _localizeService = ServiceContainer.Resolve<ILocalizeService>();
            _watchDeviceService = ServiceContainer.Resolve<IWatchDeviceService>();
            _logger = ServiceContainer.Resolve<ILogger>();

            SyncCommand = CreateDefaultAsyncCommnad(SyncAsync, _ => _inited);
            ToggleIsScreenCaptureAllowedCommand = CreateDefaultAsyncCommnad(ToggleIsScreenCaptureAllowedAsync, _ => _inited);
            ToggleShouldConnectToWatchCommand = CreateDefaultAsyncCommnad(ToggleShouldConnectToWatchAsync, _ => _inited);

            ClearClipboardPickerViewModel = new PickerViewModel<int>(
                _deviceActionService,
                _logger,
                OnClearClipboardChangingAsync,
                AppResources.ClearClipboard,
                _ => _inited,
                ex => HandleException(ex));
        }

        public bool EnableSyncOnRefresh
        {
            get => _syncOnRefresh;
            set
            {
                if (SetProperty(ref _syncOnRefresh, value))
                {
                    UpdateSyncOnRefreshAsync().FireAndForget();
                }
            }
        }

        public string LastSyncDisplay
        {
            get => $"{AppResources.LastSync} {_lastSyncDisplay}";
            set => SetProperty(ref _lastSyncDisplay, value);
        }

        public PickerViewModel<int> ClearClipboardPickerViewModel { get; }

        public bool IsScreenCaptureAllowed
        {
            get => _isScreenCaptureAllowed;
            set
            {
                if (SetProperty(ref _isScreenCaptureAllowed, value))
                {
                    ((ICommand)ToggleIsScreenCaptureAllowedCommand).Execute(null);
                }
            }
        }

        public bool CanToggleeScreenCaptureAllowed => ToggleIsScreenCaptureAllowedCommand.CanExecute(null);

        public bool ShouldConnectToWatch
        {
            get => _shouldConnectToWatch;
            set
            {
                if (SetProperty(ref _shouldConnectToWatch, value))
                {
                    ((ICommand)ToggleShouldConnectToWatchCommand).Execute(null);
                }
            }
        }

        public bool CanToggleShouldConnectToWatch => ToggleShouldConnectToWatchCommand.CanExecute(null);

        public AsyncCommand SyncCommand { get; }
        public AsyncCommand ToggleIsScreenCaptureAllowedCommand { get; }
        public AsyncCommand ToggleShouldConnectToWatchCommand { get; }

        public async Task InitAsync()
        {
            await SetLastSyncAsync();

            EnableSyncOnRefresh = await _stateService.GetSyncOnRefreshAsync();

            await InitClearClipboardAsync();

            _isScreenCaptureAllowed = await _stateService.GetScreenCaptureAllowedAsync();
            _shouldConnectToWatch = await _stateService.GetShouldConnectToWatchAsync();

            _inited = true;

            MainThread.BeginInvokeOnMainThread(() =>
            {
                TriggerPropertyChanged(nameof(IsScreenCaptureAllowed));
                TriggerPropertyChanged(nameof(ShouldConnectToWatch));
                SyncCommand.RaiseCanExecuteChanged();
                ClearClipboardPickerViewModel.SelectOptionCommand.RaiseCanExecuteChanged();
                ToggleIsScreenCaptureAllowedCommand.RaiseCanExecuteChanged();
                ToggleShouldConnectToWatchCommand.RaiseCanExecuteChanged();
            });
        }

        private async Task InitClearClipboardAsync()
        {
            var clearClipboardOptions = new Dictionary<int, string>
            {
                [CLEAR_CLIPBOARD_NEVER_OPTION] = AppResources.Never,
                [10] = AppResources.TenSeconds,
                [20] = AppResources.TwentySeconds,
                [30] = AppResources.ThirtySeconds,
                [60] = AppResources.OneMinute
            };
            if (Device.RuntimePlatform != Device.iOS)
            {
                clearClipboardOptions.Add(120, AppResources.TwoMinutes);
                clearClipboardOptions.Add(300, AppResources.FiveMinutes);
            }

            var clearClipboard = await _stateService.GetClearClipboardAsync() ?? CLEAR_CLIPBOARD_NEVER_OPTION;

            ClearClipboardPickerViewModel.Init(clearClipboardOptions, clearClipboard, CLEAR_CLIPBOARD_NEVER_OPTION);
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
            if (last is null)
            {
                LastSyncDisplay = AppResources.Never;
                return;
            }

            var localDate = last.Value.ToLocalTime();
            LastSyncDisplay = string.Format("{0} {1}",
                _localizeService.GetLocaleShortDate(localDate),
                _localizeService.GetLocaleShortTime(localDate));
        }

        public async Task SyncAsync()
        {
            if (!await HasConnectivityAsync())
            {
                return;
            }

            await _deviceActionService.ShowLoadingAsync(AppResources.Syncing);
            await _syncService.SyncPasswordlessLoginRequestsAsync();
            var success = await _syncService.FullSyncAsync(true);
            await _deviceActionService.HideLoadingAsync();
            if (!success)
            {
                await Page.DisplayAlert(null, AppResources.SyncingFailed, AppResources.Ok);
                return;
            }

            await SetLastSyncAsync();
            _platformUtilsService.ShowToast("success", null, AppResources.SyncingComplete);
        }

        private async Task<bool> OnClearClipboardChangingAsync(int optionKey)
        {
            await _stateService.SetClearClipboardAsync(optionKey == CLEAR_CLIPBOARD_NEVER_OPTION ? (int?)null : optionKey);
            return true;
        }

        private async Task ToggleIsScreenCaptureAllowedAsync()
        {
            if (IsScreenCaptureAllowed
                &&
                !await Page.DisplayAlert(AppResources.AllowScreenCapture, AppResources.AreYouSureYouWantToEnableScreenCapture, AppResources.Yes, AppResources.No))
            {
                _isScreenCaptureAllowed = !IsScreenCaptureAllowed;
                TriggerPropertyChanged(nameof(IsScreenCaptureAllowed));
                return;
            }

            await _stateService.SetScreenCaptureAllowedAsync(IsScreenCaptureAllowed);
            await _deviceActionService.SetScreenCaptureAllowedAsync();
        }

        private async Task ToggleShouldConnectToWatchAsync()
        {
            await _watchDeviceService.SetShouldConnectToWatchAsync(ShouldConnectToWatch);
        }

        private void ToggleIsScreenCaptureAllowedCommand_CanExecuteChanged(object sender, EventArgs e)
        {
            TriggerPropertyChanged(nameof(CanToggleeScreenCaptureAllowed));
        }

        private void ToggleShouldConnectToWatchCommand_CanExecuteChanged(object sender, EventArgs e)
        {
            TriggerPropertyChanged(nameof(CanToggleShouldConnectToWatch));
        }

        internal void SubscribeEvents()
        {
            ToggleIsScreenCaptureAllowedCommand.CanExecuteChanged += ToggleIsScreenCaptureAllowedCommand_CanExecuteChanged;
            ToggleShouldConnectToWatchCommand.CanExecuteChanged += ToggleShouldConnectToWatchCommand_CanExecuteChanged;
        }

        internal void UnsubscribeEvents()
        {
            ToggleIsScreenCaptureAllowedCommand.CanExecuteChanged -= ToggleIsScreenCaptureAllowedCommand_CanExecuteChanged;
            ToggleShouldConnectToWatchCommand.CanExecuteChanged -= ToggleShouldConnectToWatchCommand_CanExecuteChanged;
        }
    }
}
