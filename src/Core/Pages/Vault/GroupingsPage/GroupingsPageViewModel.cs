using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using Bit.Core.Resources.Localization;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public class GroupingsPageViewModel : VaultFilterViewModel
    {
        private const int NoFolderListSize = 100;

        private bool _refreshing;
        private bool _doingLoad;
        private bool _loading;
        private bool _loaded;
        private bool _showAddCipherButton;
        private bool _showNoData;
        private bool _showList;
        private bool _websiteIconsEnabled;
        private bool _syncRefreshing;
        private bool _showTotpFilter;
        private string _noDataText;
        private List<CipherView> _allCiphers;
        private Dictionary<string, int> _folderCounts = new Dictionary<string, int>();
        private Dictionary<string, int> _collectionCounts = new Dictionary<string, int>();
        private Dictionary<CipherType, int> _typeCounts = new Dictionary<CipherType, int>();
        private int _deletedCount = 0;
        private CancellationTokenSource _totpTickCts;
        private Task _totpTickTask;
        private readonly ICipherService _cipherService;
        private readonly IFolderService _folderService;
        private readonly ICollectionService _collectionService;
        private readonly ISyncService _syncService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IMessagingService _messagingService;
        private readonly IStateService _stateService;
        private readonly IPasswordRepromptService _passwordRepromptService;
        private readonly IOrganizationService _organizationService;
        private readonly IPolicyService _policyService;
        private readonly IConfigService _configService;
        private readonly IEnvironmentService _environmentService;
        private readonly ILogger _logger;

        public GroupingsPageViewModel()
        {
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _folderService = ServiceContainer.Resolve<IFolderService>("folderService");
            _collectionService = ServiceContainer.Resolve<ICollectionService>("collectionService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _passwordRepromptService = ServiceContainer.Resolve<IPasswordRepromptService>("passwordRepromptService");
            _organizationService = ServiceContainer.Resolve<IOrganizationService>("organizationService");
            _policyService = ServiceContainer.Resolve<IPolicyService>("policyService");
            _configService = ServiceContainer.Resolve<IConfigService>();
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>();
            _logger = ServiceContainer.Resolve<ILogger>("logger");

            Loading = true;
            GroupedItems = new ObservableRangeCollection<IGroupingsPageListItem>();
            RefreshCommand = new Command(async () =>
            {
                Refreshing = true;
                await LoadAsync();
            });
            CipherOptionsCommand = CreateDefaultAsyncRelayCommand<CipherView>(cipher => AppHelpers.CipherListOptions(Page, cipher, _passwordRepromptService),
                onException: ex => _logger.Exception(ex),
                allowsMultipleExecutions: false);
            VaultFilterCommand = CreateDefaultAsyncRelayCommand(VaultFilterOptionsAsync,
                onException: ex => _logger.Exception(ex),
                allowsMultipleExecutions: false);

            AccountSwitchingOverlayViewModel = new AccountSwitchingOverlayViewModel(_stateService, _messagingService, _logger)
            {
                AllowAddAccountRow = true
            };
        }

        public bool MainPage { get; set; }
        public CipherType? Type { get; set; }
        public string FolderId { get; set; }
        public string CollectionId { get; set; }
        public Func<CipherView, bool> Filter { get; set; }
        public bool Deleted { get; set; }
        public bool HasCiphers { get; set; }
        public bool HasFolders { get; set; }
        public bool HasCollections { get; set; }
        public bool ShowNoFolderCipherGroup => NoFolderCiphers != null
                                               && NoFolderCiphers.Count < NoFolderListSize
                                               && (Collections is null || !Collections.Any());
        public List<CipherView> Ciphers { get; set; }
        public List<CipherView> TOTPCiphers { get; set; }
        public List<CipherView> FavoriteCiphers { get; set; }
        public List<CipherView> NoFolderCiphers { get; set; }
        public List<FolderView> Folders { get; set; }
        public List<TreeNode<FolderView>> NestedFolders { get; set; }
        public List<Core.Models.View.CollectionView> Collections { get; set; }
        public List<TreeNode<Core.Models.View.CollectionView>> NestedCollections { get; set; }

        public AppOptions AppOptions { get; internal set; }
        protected override ICipherService cipherService => _cipherService;
        protected override IPolicyService policyService => _policyService;
        protected override IOrganizationService organizationService => _organizationService;
        protected override ILogger logger => _logger;

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
        public bool ShowAddCipherButton
        {
            get => _showAddCipherButton;
            set => SetProperty(ref _showAddCipherButton, value);
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
        public bool ShowTotp
        {
            get => _showTotpFilter;
            set => SetProperty(ref _showTotpFilter, value);
        }
        public AccountSwitchingOverlayViewModel AccountSwitchingOverlayViewModel { get; }
        public ObservableRangeCollection<IGroupingsPageListItem> GroupedItems { get; set; }
        public Command RefreshCommand { get; set; }
        public ICommand CipherOptionsCommand { get; }
        public bool LoadedOnce { get; set; }

        public async Task LoadAsync()
        {
            if (_doingLoad)
            {
                return;
            }

            try
            {
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
                    await _syncService.SyncPasswordlessLoginRequestsAsync();
                    await _syncService.FullSyncAsync(false);
                    return;
                }

                _deviceActionService.SetScreenCaptureAllowedAsync().FireAndForget();

                await InitVaultFilterAsync(MainPage);
                if (MainPage)
                {
                    PageTitle = ShowVaultFilter ? AppResources.Vaults : AppResources.MyVault;
                }

                _doingLoad = true;
                LoadedOnce = true;
                ShowNoData = false;
                Loading = true;
                ShowList = false;
                ShowAddCipherButton = !Deleted;

                _websiteIconsEnabled = await _stateService.GetDisableFaviconAsync() != true;
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
                throw;
            }

            var groupedItems = new List<GroupingsPageListGroup>();
            var page = Page as GroupingsPage;

            try
            {
                await LoadDataAsync();
                if (ShowNoFolderCipherGroup && (NestedFolders?.Any() ?? false))
                {
                    // Remove "No Folder" folder from folders group
                    NestedFolders = NestedFolders.GetRange(0, NestedFolders.Count - 1);
                }

#if IOS
                var uppercaseGroupNames = true;
#else
                var uppercaseGroupNames = false;
#endif

                var hasFavorites = FavoriteCiphers?.Any() ?? false;
                if (hasFavorites)
                {
                    var favListItems = FavoriteCiphers.Select(c => new CipherItemViewModel(c, _websiteIconsEnabled)).ToList();
                    groupedItems.Add(new GroupingsPageListGroup(favListItems, AppResources.Favorites,
                        favListItems.Count, uppercaseGroupNames, true));
                }
                if (MainPage)
                {
                    AddTotpGroupItem(groupedItems, uppercaseGroupNames);

                    var types = new CipherType[] { CipherType.Login, CipherType.Card, CipherType.Identity, CipherType.SecureNote };
                    var typesGroup = new GroupingsPageListGroup(AppResources.Types, types.Length, uppercaseGroupNames, !hasFavorites);
                    foreach (CipherType t in types)
                    {
                        typesGroup.Add(new GroupingsPageListItem
                        {
                            Type = t,
                            ItemCount = _typeCounts.GetValueOrDefault(t).ToString("N0")
                        });
                    }
                    groupedItems.Add(typesGroup);
                }
                if (NestedFolders?.Any() ?? false)
                {
                    var folderListItems = NestedFolders.Select(f =>
                    {
                        var fId = f.Node.Id ?? "none";
                        return new GroupingsPageListItem
                        {
                            Folder = f.Node,
                            ItemCount = (_folderCounts.ContainsKey(fId) ? _folderCounts[fId] : 0).ToString("N0")
                        };
                    }).ToList();
                    groupedItems.Add(new GroupingsPageListGroup(folderListItems, AppResources.Folders,
                        folderListItems.Count, uppercaseGroupNames, !MainPage));
                }
                if (NestedCollections?.Any() ?? false)
                {
                    var collectionListItems = NestedCollections.Select(c => new GroupingsPageListItem
                    {
                        Collection = c.Node,
                        ItemCount = (_collectionCounts.ContainsKey(c.Node.Id) ?
                            _collectionCounts[c.Node.Id] : 0).ToString("N0")
                    }).ToList();
                    groupedItems.Add(new GroupingsPageListGroup(collectionListItems, AppResources.Collections,
                        collectionListItems.Count, uppercaseGroupNames, !MainPage));
                }
                if (Ciphers?.Any() ?? false)
                {
                    CreateCipherGroupedItems(groupedItems);
                }
                if (ShowTotp && (!TOTPCiphers?.Any() ?? false))
                {
                    Page.Navigation.PopAsync();
                    return;
                }
                if (ShowNoFolderCipherGroup)
                {
                    var noFolderCiphersListItems = NoFolderCiphers.Select(
                        c => new CipherItemViewModel(c, _websiteIconsEnabled)).ToList();
                    groupedItems.Add(new GroupingsPageListGroup(noFolderCiphersListItems, AppResources.FolderNone,
                        noFolderCiphersListItems.Count, uppercaseGroupNames, false));
                }
                // Ensure this is last in the list (appears at the bottom)
                if (MainPage && !Deleted)
                {
                    groupedItems.Add(new GroupingsPageListGroup(new List<GroupingsPageListItem>()
                    {
                        new GroupingsPageListItem()
                        {
                            IsTrash = true,
                            ItemCount = _deletedCount.ToString("N0")
                        }
                    }, AppResources.Trash, _deletedCount, uppercaseGroupNames, false));
                }

                // TODO: refactor this
                if (DeviceInfo.Platform == DevicePlatform.Android
                    ||
                    GroupedItems.Any())
                {
                    var items = new List<IGroupingsPageListItem>();
                    foreach (var itemGroup in groupedItems)
                    {
                        items.Add(new GroupingsPageHeaderListItem(itemGroup.Name, itemGroup.ItemCount));
                        items.AddRange(itemGroup);
                    }

                    await MainThread.InvokeOnMainThreadAsync(() =>
                    {
                        GroupedItems.ReplaceRange(items);
                    });
                }
                else
                {
                    // HACK: we need this on iOS, so that it doesn't crash when adding coming from an empty list
                    var first = true;
                    var items = new List<IGroupingsPageListItem>();
                    foreach (var itemGroup in groupedItems)
                    {
                        if (!first)
                        {
                            items.Add(new GroupingsPageHeaderListItem(itemGroup.Name, itemGroup.ItemCount));
                        }
                        else
                        {
                            first = false;
                        }
                        items.AddRange(itemGroup);
                    }

                    await MainThread.InvokeOnMainThreadAsync(() =>
                    {
                        if (!groupedItems.Any())
                        {
                            GroupedItems.Clear();
                            return;
                        }

                        GroupedItems.ReplaceRange(new List<IGroupingsPageListItem> { new GroupingsPageHeaderListItem(groupedItems[0].Name, groupedItems[0].ItemCount) });
                        GroupedItems.AddRange(items);
                    });
                }
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
                throw;
            }
            finally
            {
                _doingLoad = false;
                Loaded = true;
                Loading = false;
                await MainThread.InvokeOnMainThreadAsync(() =>
                {
                    ShowNoData = (MainPage && !HasCiphers) || !groupedItems.Any();
                    ShowList = !ShowNoData;
                    DisableRefreshing();
                });
            }
        }

        private void AddTotpGroupItem(List<GroupingsPageListGroup> groupedItems, bool uppercaseGroupNames)
        {
            if (TOTPCiphers?.Any() == true)
            {
                groupedItems.Insert(0, new GroupingsPageListGroup(
                    AppResources.Totp, 1, uppercaseGroupNames, false)
                        {
                            new GroupingsPageListItem
                            {
                                IsTotpCode = true,
                                Type = CipherType.Login,
                                ItemCount = TOTPCiphers.Count().ToString("N0")
                            }
                        });
            }
        }

        private void CreateCipherGroupedItems(List<GroupingsPageListGroup> groupedItems)
        {
#if IOS
            var uppercaseGroupNames = true;
#else
            var uppercaseGroupNames = false;
#endif
            _totpTickCts?.Cancel();
            if (ShowTotp)
            {
                var ciphersListItems = TOTPCiphers.Select(c => new GroupingsPageTOTPListItem(c, _websiteIconsEnabled)).ToList();
                groupedItems.Add(new GroupingsPageListGroup(ciphersListItems, AppResources.Items,
                    ciphersListItems.Count, uppercaseGroupNames, !MainPage && !groupedItems.Any()));

                StartCiphersTotpTick(ciphersListItems);
            }
            else
            {
                var ciphersListItems = Ciphers.Where(c => c.IsDeleted == Deleted)
                    .Select(c => new CipherItemViewModel(c, _websiteIconsEnabled)).ToList();
                groupedItems.Add(new GroupingsPageListGroup(ciphersListItems, AppResources.Items,
                    ciphersListItems.Count, uppercaseGroupNames, !MainPage && !groupedItems.Any()));
            }
        }

        private void StartCiphersTotpTick(List<GroupingsPageTOTPListItem> ciphersListItems)
        {
            _totpTickCts?.Cancel();
            _totpTickCts = new CancellationTokenSource();
            _totpTickTask = new TimerTask(logger, () => ciphersListItems.ForEach(i => i.TotpTickAsync()), _totpTickCts).RunPeriodic();
        }

        public async Task StopCiphersTotpTick()
        {
            _totpTickCts?.Cancel();
            if (_totpTickTask != null)
            {
                await _totpTickTask;
            }
        }

        public void DisableRefreshing()
        {
            Refreshing = false;
            SyncRefreshing = false;
        }

        protected override async Task OnVaultFilterSelectedAsync()
        {
            await LoadAsync();
        }

        public async Task SelectCipherAsync(CipherView cipher)
        {
            var page = new CipherDetailsPage(cipher.Id);
            await Page.Navigation.PushModalAsync(new NavigationPage(page));
        }

        public async Task SelectTypeAsync(CipherType type)
        {
            string title = null;
            switch (type)
            {
                case CipherType.Login:
                    title = AppResources.Logins;
                    break;
                case CipherType.SecureNote:
                    title = AppResources.SecureNotes;
                    break;
                case CipherType.Card:
                    title = AppResources.Cards;
                    break;
                case CipherType.Identity:
                    title = AppResources.Identities;
                    break;
                default:
                    break;
            }
            var page = new GroupingsPage(false, type, null, null, title, _vaultFilterSelection);
            await Page.Navigation.PushAsync(page);
        }

        public async Task SelectFolderAsync(FolderView folder)
        {
            var page = new GroupingsPage(false, null, folder.Id ?? "none", null, folder.Name, _vaultFilterSelection);
            await Page.Navigation.PushAsync(page);
        }

        public async Task SelectCollectionAsync(Core.Models.View.CollectionView collection)
        {
            var page = new GroupingsPage(false, null, null, collection.Id, collection.Name, _vaultFilterSelection);
            await Page.Navigation.PushAsync(page);
        }

        public async Task SelectTrashAsync()
        {
            var page = new GroupingsPage(false, null, null, null, AppResources.Trash, _vaultFilterSelection, null,
                true);
            await Page.Navigation.PushAsync(page);
        }

        public async Task SelectTotpCodesAsync()
        {
            var page = new GroupingsPage(false, CipherType.Login, null, null, AppResources.VerificationCodes, _vaultFilterSelection, null,
                false, true);
            await Page.Navigation.PushAsync(page);
        }

        public async Task ExitAsync()
        {
            var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.ExitConfirmation,
                   AppResources.Exit, AppResources.Yes, AppResources.Cancel);
            if (confirmed)
            {
                _messagingService.Send("exit");
            }
        }

        public async Task SyncAsync()
        {
            if (Microsoft.Maui.Networking.Connectivity.NetworkAccess == Microsoft.Maui.Networking.NetworkAccess.None)
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

        private async Task LoadDataAsync()
        {
            var canAccessPremium = await _stateService.CanAccessPremiumAsync();
            NoDataText = AppResources.NoItems;
            _allCiphers = await GetAllCiphersAsync();
            HasCiphers = _allCiphers.Any();
            TOTPCiphers = _allCiphers.Where(c => c.IsDeleted == Deleted && c.Type == CipherType.Login && !string.IsNullOrEmpty(c.Login?.Totp) && (c.OrganizationUseTotp || canAccessPremium)).ToList();
            FavoriteCiphers?.Clear();
            NoFolderCiphers?.Clear();
            _folderCounts.Clear();
            _collectionCounts.Clear();
            _typeCounts.Clear();
            HasFolders = false;
            HasCollections = false;
            Filter = null;
            _deletedCount = 0;

            if (MainPage)
            {
                await FillFoldersAndCollectionsAsync();
                NestedFolders = await _folderService.GetAllNestedAsync(Folders);
                HasFolders = NestedFolders.Any(f => f.Node?.Id != null);
                NestedCollections = Collections != null ? await _collectionService.GetAllNestedAsync(Collections) : null;
                HasCollections = NestedCollections?.Any() ?? false;
            }
            else
            {
                if (Deleted)
                {
                    Filter = c => c.IsDeleted;
                    NoDataText = AppResources.NoItemsTrash;
                }
                else if (ShowTotp)
                {
                    Filter = c => c.Type == CipherType.Login && !c.IsDeleted && !string.IsNullOrEmpty(c.Login?.Totp);
                }
                else if (Type != null)
                {
                    Filter = c => !c.IsDeleted
                                  &&
                                  Type.Value == c.Type;
                }
                else if (FolderId != null)
                {
                    NoDataText = AppResources.NoItemsFolder;
                    var folderId = FolderId == "none" ? null : FolderId;
                    if (folderId != null)
                    {
                        var folderNode = await _folderService.GetNestedAsync(folderId);
                        if (folderNode?.Node != null)
                        {
                            PageTitle = folderNode.Node.Name;
                            NestedFolders = (folderNode.Children?.Count ?? 0) > 0 ? folderNode.Children : null;
                        }
                    }
                    else
                    {
                        PageTitle = AppResources.FolderNone;
                    }
                    Filter = c => c.FolderId == folderId && !c.IsDeleted;
                }
                else if (CollectionId != null)
                {
                    ShowAddCipherButton = false;
                    NoDataText = AppResources.NoItemsCollection;
                    var collectionNode = await _collectionService.GetNestedAsync(CollectionId);
                    if (collectionNode?.Node != null)
                    {
                        PageTitle = collectionNode.Node.Name;
                        NestedCollections = (collectionNode.Children?.Count ?? 0) > 0 ? collectionNode.Children : null;
                    }
                    Filter = c => c.CollectionIds?.Contains(CollectionId) ?? false && !c.IsDeleted;
                }
                else
                {
                    PageTitle = AppResources.AllItems;
                }
                Ciphers = Filter != null ? _allCiphers.Where(Filter).ToList() : _allCiphers;
            }

            foreach (var c in _allCiphers)
            {
                if (MainPage)
                {
                    if (c.IsDeleted)
                    {
                        _deletedCount++;
                        continue;
                    }

                    if (c.Favorite)
                    {
                        if (FavoriteCiphers == null)
                        {
                            FavoriteCiphers = new List<CipherView>();
                        }
                        FavoriteCiphers.Add(c);
                    }
                    if (c.FolderId == null)
                    {
                        if (NoFolderCiphers == null)
                        {
                            NoFolderCiphers = new List<CipherView>();
                        }
                        NoFolderCiphers.Add(c);
                    }

                    _typeCounts[c.Type] = _typeCounts.TryGetValue(c.Type, out var currentTypeCount)
                                                ? currentTypeCount + 1
                                                : 1;
                }

                if (c.IsDeleted)
                {
                    continue;
                }

                var fId = c.FolderId ?? "none";
                if (_folderCounts.ContainsKey(fId))
                {
                    _folderCounts[fId] = _folderCounts[fId] + 1;
                }
                else
                {
                    _folderCounts.Add(fId, 1);
                }

                if (c.CollectionIds != null)
                {
                    foreach (var colId in c.CollectionIds)
                    {
                        if (_collectionCounts.ContainsKey(colId))
                        {
                            _collectionCounts[colId] = _collectionCounts[colId] + 1;
                        }
                        else
                        {
                            _collectionCounts.Add(colId, 1);
                        }
                    }
                }
            }
        }

        private async Task FillFoldersAndCollectionsAsync()
        {
            var orgId = GetVaultFilterOrgId();
            var decFolders = await _folderService.GetAllDecryptedAsync();
            var decCollections = await _collectionService.GetAllDecryptedAsync();
            if (IsVaultFilterMyVault)
            {
                Folders = BuildFolders(decFolders);
                Collections = null;
            }
            else if (IsVaultFilterOrgVault && !string.IsNullOrWhiteSpace(orgId))
            {
                Folders = BuildFolders(decFolders);
                Collections = decCollections?.Where(c => c.OrganizationId == orgId).ToList();
            }
            else
            {
                Folders = decFolders;
                Collections = decCollections;
            }
        }

        private List<FolderView> BuildFolders(List<FolderView> decFolders)
        {
            var folders = decFolders.Where(f => _allCiphers.Any(c => c.FolderId == f.Id)).ToList();
            return folders.Any() ? folders : null;
        }

        internal async Task CheckOrganizationUnassignedItemsAsync()
        {
            try
            {
                if (AppOptions?.HasJustLoggedInOrUnlocked != true)
                {
                    return;
                }

                AppOptions.HasJustLoggedInOrUnlocked = false;

                if (!await _configService.GetFeatureFlagBoolAsync(Core.Constants.UnassignedItemsBannerFlag)
                    ||
                    !await _stateService.GetShouldCheckOrganizationUnassignedItemsAsync())
                {
                    return;
                }

                var waitSyncTask = Task.Run(async () =>
                {
                    while (_syncService.SyncInProgress)
                    {
                        await Task.Delay(100);
                    }
                });
                await waitSyncTask.WaitAsync(TimeSpan.FromMinutes(5));

                if (!await _cipherService.VerifyOrganizationHasUnassignedItemsAsync())
                {
                    return;
                }

                var message = _environmentService.SelectedRegion == Core.Enums.Region.SelfHosted
                    ? AppResources.OrganizationUnassignedItemsMessageSelfHost041624DescriptionLong
                    : AppResources.OrganizationUnassignedItemsMessageUSEUDescriptionLong;

                var response = await _deviceActionService.DisplayAlertAsync(AppResources.Notice,
                    message,
                    null,
                    AppResources.RemindMeLater,
                    AppResources.Ok);

                if (response == AppResources.Ok)
                {
                    await _stateService.SetShouldCheckOrganizationUnassignedItemsAsync(false);
                }
            }
            catch (TimeoutException) { }
            catch (Exception ex)
            {
                _logger.Exception(ex);
            }
        }
    }
}
