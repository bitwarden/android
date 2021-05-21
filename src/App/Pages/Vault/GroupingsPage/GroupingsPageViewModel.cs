using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class GroupingsPageViewModel : BaseViewModel
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
        private string _noDataText;
        private List<CipherView> _allCiphers;
        private Dictionary<string, int> _folderCounts = new Dictionary<string, int>();
        private Dictionary<string, int> _collectionCounts = new Dictionary<string, int>();
        private Dictionary<CipherType, int> _typeCounts = new Dictionary<CipherType, int>();
        private int _deletedCount = 0;

        private readonly ICipherService _cipherService;
        private readonly IFolderService _folderService;
        private readonly ICollectionService _collectionService;
        private readonly ISyncService _syncService;
        private readonly IUserService _userService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IMessagingService _messagingService;
        private readonly IStateService _stateService;
        private readonly IStorageService _storageService;
        private readonly IPasswordRepromptService _passwordRepromptService;

        public GroupingsPageViewModel()
        {
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _folderService = ServiceContainer.Resolve<IFolderService>("folderService");
            _collectionService = ServiceContainer.Resolve<ICollectionService>("collectionService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _userService = ServiceContainer.Resolve<IUserService>("userService");
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _passwordRepromptService = ServiceContainer.Resolve<IPasswordRepromptService>("passwordRepromptService");

            Loading = true;
            PageTitle = AppResources.MyVault;
            GroupedItems = new ExtendedObservableCollection<GroupingsPageListGroup>();
            RefreshCommand = new Command(async () =>
            {
                Refreshing = true;
                await LoadAsync();
            });
            CipherOptionsCommand = new Command<CipherView>(CipherOptionsAsync);
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
        public bool ShowNoFolderCiphers => (NoFolderCiphers?.Count ?? int.MaxValue) < NoFolderListSize &&
            (!Collections?.Any() ?? true);
        public List<CipherView> Ciphers { get; set; }
        public List<CipherView> FavoriteCiphers { get; set; }
        public List<CipherView> NoFolderCiphers { get; set; }
        public List<FolderView> Folders { get; set; }
        public List<TreeNode<FolderView>> NestedFolders { get; set; }
        public List<Core.Models.View.CollectionView> Collections { get; set; }
        public List<TreeNode<Core.Models.View.CollectionView>> NestedCollections { get; set; }

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
        public bool WebsiteIconsEnabled
        {
            get => _websiteIconsEnabled;
            set => SetProperty(ref _websiteIconsEnabled, value);
        }
        public ExtendedObservableCollection<GroupingsPageListGroup> GroupedItems { get; set; }
        public Command RefreshCommand { get; set; }
        public Command<CipherView> CipherOptionsCommand { get; set; }
        public bool LoadedOnce { get; set; }

        public async Task LoadAsync()
        {
            if (_doingLoad)
            {
                return;
            }
            var authed = await _userService.IsAuthenticatedAsync();
            if (!authed)
            {
                return;
            }
            if (await _vaultTimeoutService.IsLockedAsync())
            {
                return;
            }
            if (await _storageService.GetAsync<bool>(Constants.SyncOnRefreshKey) && Refreshing && !SyncRefreshing)
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
            ShowAddCipherButton = !Deleted;
            var groupedItems = new List<GroupingsPageListGroup>();
            var page = Page as GroupingsPage;

            WebsiteIconsEnabled = !(await _stateService.GetAsync<bool?>(Constants.DisableFaviconKey))
                .GetValueOrDefault();
            try
            {
                await LoadDataAsync();
                if (ShowNoFolderCiphers && (NestedFolders?.Any() ?? false))
                {
                    // Remove "No Folder" from folder listing
                    NestedFolders = NestedFolders.GetRange(0, NestedFolders.Count - 1);
                }

                var uppercaseGroupNames = _deviceActionService.DeviceType == DeviceType.iOS;
                var hasFavorites = FavoriteCiphers?.Any() ?? false;
                if (hasFavorites)
                {
                    var favListItems = FavoriteCiphers.Select(c => new GroupingsPageListItem { Cipher = c }).ToList();
                    groupedItems.Add(new GroupingsPageListGroup(favListItems, AppResources.Favorites,
                        favListItems.Count, uppercaseGroupNames, true));
                }
                if (MainPage)
                {
                    groupedItems.Add(new GroupingsPageListGroup(
                        AppResources.Types, 4, uppercaseGroupNames, !hasFavorites)
                    {
                        new GroupingsPageListItem
                        {
                            Type = CipherType.Login,
                            ItemCount = (_typeCounts.ContainsKey(CipherType.Login) ?
                                _typeCounts[CipherType.Login] : 0).ToString("N0")
                        },
                        new GroupingsPageListItem
                        {
                            Type = CipherType.Card,
                            ItemCount = (_typeCounts.ContainsKey(CipherType.Card) ?
                                _typeCounts[CipherType.Card] : 0).ToString("N0")
                        },
                        new GroupingsPageListItem
                        {
                            Type = CipherType.Identity,
                            ItemCount = (_typeCounts.ContainsKey(CipherType.Identity) ?
                                _typeCounts[CipherType.Identity] : 0).ToString("N0")
                        },
                        new GroupingsPageListItem
                        {
                            Type = CipherType.SecureNote,
                            ItemCount = (_typeCounts.ContainsKey(CipherType.SecureNote) ?
                                _typeCounts[CipherType.SecureNote] : 0).ToString("N0")
                        },
                    });
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
                    var ciphersListItems = Ciphers.Where(c => c.IsDeleted == Deleted)
                        .Select(c => new GroupingsPageListItem { Cipher = c }).ToList();
                    groupedItems.Add(new GroupingsPageListGroup(ciphersListItems, AppResources.Items,
                        ciphersListItems.Count, uppercaseGroupNames, !MainPage && !groupedItems.Any()));
                }
                if (ShowNoFolderCiphers)
                {
                    var noFolderCiphersListItems = NoFolderCiphers.Select(
                        c => new GroupingsPageListItem { Cipher = c }).ToList();
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
                GroupedItems.ResetWithRange(groupedItems);
            }
            finally
            {
                _doingLoad = false;
                Loaded = true;
                Loading = false;
                ShowNoData = (MainPage && !HasCiphers) || !groupedItems.Any();
                ShowList = !ShowNoData;
                DisableRefreshing();
            }
        }

        public void DisableRefreshing()
        {
            Refreshing = false;
            SyncRefreshing = false;
        }

        public async Task SelectCipherAsync(CipherView cipher)
        {
            var page = new ViewPage(cipher.Id);
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
            var page = new GroupingsPage(false, type, null, null, title);
            await Page.Navigation.PushAsync(page);
        }

        public async Task SelectFolderAsync(FolderView folder)
        {
            var page = new GroupingsPage(false, null, folder.Id ?? "none", null, folder.Name);
            await Page.Navigation.PushAsync(page);
        }

        public async Task SelectCollectionAsync(Core.Models.View.CollectionView collection)
        {
            var page = new GroupingsPage(false, null, null, collection.Id, collection.Name);
            await Page.Navigation.PushAsync(page);
        }

        public async Task SelectTrashAsync()
        {
            var page = new GroupingsPage(false, null, null, null, AppResources.Trash, null, true);
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
            if (Xamarin.Essentials.Connectivity.NetworkAccess == Xamarin.Essentials.NetworkAccess.None)
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
            NoDataText = AppResources.NoItems;
            _allCiphers = await _cipherService.GetAllDecryptedAsync();
            HasCiphers = _allCiphers.Any();
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
                Folders = await _folderService.GetAllDecryptedAsync();
                NestedFolders = await _folderService.GetAllNestedAsync();
                HasFolders = NestedFolders.Any(f => f.Node?.Id != null);
                Collections = await _collectionService.GetAllDecryptedAsync();
                NestedCollections = await _collectionService.GetAllNestedAsync(Collections);
                HasCollections = NestedCollections.Any();
            }
            else
            {
                if (Deleted)
                {
                    Filter = c => c.IsDeleted;
                    NoDataText = AppResources.NoItemsTrash;
                }
                else if (Type != null)
                {
                    Filter = c => c.Type == Type.Value && !c.IsDeleted;
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

                    if (_typeCounts.ContainsKey(c.Type))
                    {
                        _typeCounts[c.Type] = _typeCounts[c.Type] + 1;
                    }
                    else
                    {
                        _typeCounts.Add(c.Type, 1);
                    }
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

        private async void CipherOptionsAsync(CipherView cipher)
        {
            if ((Page as BaseContentPage).DoOnce())
            {
                await AppHelpers.CipherListOptions(Page, cipher, _passwordRepromptService);
            }
        }
    }
}
