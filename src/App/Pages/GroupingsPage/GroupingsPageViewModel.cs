using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class GroupingsPageViewModel : BaseViewModel
    {
        private bool _loading = false;
        private bool _loaded = false;
        private List<CipherView> _allCiphers;

        private readonly ICipherService _cipherService;
        private readonly IFolderService _folderService;
        private readonly ICollectionService _collectionService;
        private readonly ISyncService _syncService;

        public GroupingsPageViewModel()
        {
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _folderService = ServiceContainer.Resolve<IFolderService>("folderService");
            _collectionService = ServiceContainer.Resolve<ICollectionService>("collectionService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");

            PageTitle = "My Vault";
            GroupedItems = new ExtendedObservableCollection<GroupingsPageListGroup>();
            LoadCommand = new Command(async () => await LoadAsync());
        }

        public bool ShowFavorites { get; set; } = true;
        public bool ShowFolders { get; set; } = true;
        public bool ShowCollections { get; set; } = true;

        public List<CipherView> Ciphers { get; set; }
        public List<CipherView> FavoriteCiphers { get; set; }
        public List<CipherView> NoFolderCiphers { get; set; }
        public List<FolderView> Folders { get; set; }
        public List<TreeNode<FolderView>> NestedFolders { get; set; }
        public List<Core.Models.View.CollectionView> Collections { get; set; }
        public List<TreeNode<Core.Models.View.CollectionView>> NestedCollections { get; set; }

        public bool Loading
        {
            get => _loading;
            set => SetProperty(ref _loading, value);
        }
        public bool Loaded
        {
            get => _loaded;
            set
            {
                SetProperty(ref _loaded, value);
                SetProperty(ref _loading, !value);
            }
        }
        public ExtendedObservableCollection<GroupingsPageListGroup> GroupedItems { get; set; }
        public Command LoadCommand { get; set; }

        public async Task LoadAsync()
        {
            try
            {
                await LoadFoldersAsync();
                await LoadCollectionsAsync();
                await LoadCiphersAsync();

                var favListItems = FavoriteCiphers?.Select(c => new GroupingsPageListItem { Cipher = c }).ToList();
                var folderListItems = NestedFolders?.Select(f => new GroupingsPageListItem { Folder = f.Node }).ToList();
                var collectionListItems = NestedCollections?.Select(c =>
                    new GroupingsPageListItem { Collection = c.Node }).ToList();

                var groupedItems = new List<GroupingsPageListGroup>();
                if(favListItems?.Any() ?? false)
                {
                    groupedItems.Add(new GroupingsPageListGroup(favListItems, AppResources.Favorites,
                        Device.RuntimePlatform == Device.iOS));
                }
                if(folderListItems?.Any() ?? false)
                {
                    groupedItems.Add(new GroupingsPageListGroup(folderListItems, AppResources.Folders,
                        Device.RuntimePlatform == Device.iOS));
                }
                if(collectionListItems?.Any() ?? false)
                {
                    groupedItems.Add(new GroupingsPageListGroup(collectionListItems, AppResources.Collections,
                        Device.RuntimePlatform == Device.iOS));
                }
                GroupedItems.ResetWithRange(groupedItems);
            }
            finally
            {
                Loaded = true;
            }
        }

        public async Task SelectCipherAsync(CipherView cipher)
        {
            var page = new ViewPage(cipher.Id);
            await Page.Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async Task LoadFoldersAsync()
        {
            if(!ShowFolders)
            {
                return;
            }
            Folders = await _folderService.GetAllDecryptedAsync();
            NestedFolders = await _folderService.GetAllNestedAsync();
        }

        private async Task LoadCollectionsAsync()
        {
            if(!ShowCollections)
            {
                return;
            }
            Collections = await _collectionService.GetAllDecryptedAsync();
            NestedCollections = await _collectionService.GetAllNestedAsync(Collections);
        }

        private async Task LoadCiphersAsync()
        {
            _allCiphers = await _cipherService.GetAllDecryptedAsync();
            Ciphers = _allCiphers;
            foreach(var c in _allCiphers)
            {
                if(c.Favorite)
                {
                    if(FavoriteCiphers == null)
                    {
                        FavoriteCiphers = new List<CipherView>();
                    }
                    FavoriteCiphers.Add(c);
                }
                if(c.FolderId == null)
                {
                    if(NoFolderCiphers == null)
                    {
                        NoFolderCiphers = new List<CipherView>();
                    }
                    NoFolderCiphers.Add(c);
                }
            }
            FavoriteCiphers = _allCiphers.Where(c => c.Favorite).ToList();
        }
    }
}
