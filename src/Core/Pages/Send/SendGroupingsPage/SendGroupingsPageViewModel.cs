using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

using DeviceType = Bit.Core.Enums.DeviceType;
using Microsoft.Maui.Networking;
using Microsoft.Maui.Devices;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public class SendGroupingsPageViewModel : BaseViewModel
    {
        private bool _sendEnabled;
        private bool _refreshing;
        private bool _doingLoad;
        private bool _loading;
        private bool _loaded;
        private bool _showNoData;
        private bool _showList;
        private bool _syncRefreshing;
        private string _noDataText;
        private List<SendView> _allSends;
        private Dictionary<SendType, int> _typeCounts = new Dictionary<SendType, int>();

        private readonly ISendService _sendService;
        private readonly ISyncService _syncService;
        private readonly IStateService _stateService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IPlatformUtilsService _platformUtilsService;

        public SendGroupingsPageViewModel()
        {
            _sendService = ServiceContainer.Resolve<ISendService>("sendService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");

            Loading = true;
            PageTitle = AppResources.Send;
            GroupedSends = new ObservableRangeCollection<ISendGroupingsPageListItem>();
            RefreshCommand = new Command(async () =>
            {
                Refreshing = true;
                await LoadAsync();
            });
            SendOptionsCommand = new Command<SendView>(SendOptionsAsync);
        }

        public bool MainPage { get; set; }
        public SendType? Type { get; set; }
        public Func<SendView, bool> Filter { get; set; }
        public bool HasSends { get; set; }
        public List<SendView> Sends { get; set; }

        public bool SendEnabled
        {
            get => _sendEnabled;
            set => SetProperty(ref _sendEnabled, value);
        }
        public bool Refreshing
        {
            get => _refreshing;
            set => SetProperty(ref _refreshing, value);
        }
        public bool SyncRefreshing
        {
            get => _syncRefreshing;
            set => SetProperty(ref _syncRefreshing, value);
        }
        public bool Loading
        {
            get => _loading;
            set => SetProperty(ref _loading, value);
        }
        public bool Loaded
        {
            get => _loaded;
            set => SetProperty(ref _loaded, value);
        }
        public bool ShowNoData
        {
            get => _showNoData;
            set => SetProperty(ref _showNoData, value);
        }
        public string NoDataText
        {
            get => _noDataText;
            set => SetProperty(ref _noDataText, value);
        }
        public bool ShowList
        {
            get => _showList;
            set => SetProperty(ref _showList, value);
        }
        public ObservableRangeCollection<ISendGroupingsPageListItem> GroupedSends { get; set; }
        public Command RefreshCommand { get; set; }
        public Command<SendView> SendOptionsCommand { get; set; }
        public bool LoadedOnce { get; set; }

        public async Task LoadAsync()
        {
            if (_doingLoad)
            {
                return;
            }
            var authed = await _stateService.IsAuthenticatedAsync();
            if (!authed)
            {
                return;
            }
            if (await _vaultTimeoutService.IsLockedAsync())
            {
                return;
            }
            if (await _stateService.GetSyncOnRefreshAsync() && Refreshing && !SyncRefreshing)
            {
                SyncRefreshing = true;
                await _syncService.FullSyncAsync(false);
                return;
            }

            _doingLoad = true;
            LoadedOnce = true;
            ShowNoData = false;
            Loading = true;
            ShowList = false;
            SendEnabled = !await AppHelpers.IsSendDisabledByPolicyAsync();
            var groupedSends = new List<SendGroupingsPageListGroup>();
            var page = Page as SendGroupingsPage;

            try
            {
                await LoadDataAsync();

                // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
                var uppercaseGroupNames = Device.RuntimePlatform == Device.iOS;
                if (MainPage)
                {
                    groupedSends.Add(new SendGroupingsPageListGroup(
                        AppResources.Types, 0, uppercaseGroupNames, true)
                    {
                        new SendGroupingsPageListItem(SendType.Text, _typeCounts.TryGetValue(SendType.Text, out var textCountValue) ? textCountValue : 0),
                        new SendGroupingsPageListItem(SendType.File, _typeCounts.TryGetValue(SendType.File, out var fileCountValue) ? fileCountValue : 0)
                    });
                }

                if (Sends?.Any() == true)
                {
                    var sendsListItems = Sends.Select(s => new SendGroupingsPageListItem(s, SendEnabled)).ToList();
                    groupedSends.Add(new SendGroupingsPageListGroup(sendsListItems,
                        MainPage ? AppResources.AllSends : AppResources.Sends, sendsListItems.Count,
                        uppercaseGroupNames, !MainPage));
                }

                if (DeviceInfo.Platform == DevicePlatform.Android
                    ||
                    GroupedSends.Any())
                {
                    var items = new List<ISendGroupingsPageListItem>();
                    foreach (var itemGroup in groupedSends)
                    {
                        items.Add(new SendGroupingsPageHeaderListItem(itemGroup.Name, itemGroup.ItemCount));
                        items.AddRange(itemGroup);
                    }

                    GroupedSends.ReplaceRange(items);
                }
                else
                {
                    // HACK: we need this on iOS, so that it doesn't crash when adding coming from an empty list
                    var first = true;
                    var items = new List<ISendGroupingsPageListItem>();
                    foreach (var itemGroup in groupedSends)
                    {
                        if (!first)
                        {
                            items.Add(new SendGroupingsPageHeaderListItem(itemGroup.Name, itemGroup.ItemCount));
                        }
                        else
                        {
                            first = false;
                        }
                        items.AddRange(itemGroup);
                    }

                    if (groupedSends.Any())
                    {
                        GroupedSends.ReplaceRange(new List<ISendGroupingsPageListItem> { new SendGroupingsPageHeaderListItem(groupedSends[0].Name, groupedSends[0].ItemCount) });
                        GroupedSends.AddRange(items);
                    }
                    else
                    {
                        GroupedSends.Clear();
                    }
                }
            }
            finally
            {
                _doingLoad = false;
                Loaded = true;
                Loading = false;
                ShowNoData = (MainPage && !HasSends) || !groupedSends.Any();
                ShowList = !ShowNoData;
                DisableRefreshing();
            }
        }

        public void DisableRefreshing()
        {
            Refreshing = false;
            SyncRefreshing = false;
        }

        public async Task SelectSendAsync(SendView send)
        {
            var page = new SendAddEditPage(null, send.Id);
            await Page.Navigation.PushModalAsync(new NavigationPage(page));
        }

        public async Task SelectTypeAsync(SendType type)
        {
            string title = null;
            switch (type)
            {
                case SendType.Text:
                    title = AppResources.TypeText;
                    break;
                case SendType.File:
                    title = AppResources.TypeFile;
                    break;
                default:
                    break;
            }
            var page = new SendGroupingsPage(false, type, title);
            await Page.Navigation.PushAsync(page);
        }

        public async Task SyncAsync()
        {
            if (Connectivity.NetworkAccess == NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return;
            }
            await _deviceActionService.ShowLoadingAsync(AppResources.Syncing);
            try
            {
                await _syncService.FullSyncAsync(false, true);
                await _deviceActionService.HideLoadingAsync();
                _platformUtilsService.ShowToast("success", null, AppResources.SyncingComplete);
            }
            catch
            {
                await _deviceActionService.HideLoadingAsync();
                _platformUtilsService.ShowToast("error", null, AppResources.SyncingFailed);
            }
        }

        public void ShowAbout()
        {
            _platformUtilsService.LaunchUri("https://bitwarden.com/products/send/");
        }

        private async Task LoadDataAsync()
        {
            NoDataText = AppResources.NoSends;
            _allSends = await _sendService.GetAllDecryptedAsync();
            HasSends = _allSends.Any();
            _typeCounts.Clear();
            Filter = null;

            if (MainPage)
            {
                Sends = _allSends;
                foreach (var c in _allSends)
                {
                    if (_typeCounts.ContainsKey(c.Type))
                    {
                        _typeCounts[c.Type] = _typeCounts[c.Type] + 1;
                    }
                    else
                    {
                        _typeCounts.Add(c.Type, 1);
                    }
                }
            }
            else
            {
                if (Type != null)
                {
                    Filter = c => c.Type == Type.Value;
                }
                else
                {
                    PageTitle = AppResources.AllSends;
                }
                Sends = Filter != null ? _allSends.Where(Filter).ToList() : _allSends;
            }
        }

        private async void SendOptionsAsync(SendView send)
        {
            if ((Page as BaseContentPage).DoOnce())
            {
                var selection = await AppHelpers.SendListOptions(Page, send);
                if (selection == AppResources.RemovePassword || selection == AppResources.Delete)
                {
                    await LoadAsync();
                }
            }
        }
    }
}
