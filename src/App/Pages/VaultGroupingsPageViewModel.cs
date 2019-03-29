using Bit.Core.Models.View;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class VaultGroupingsPageViewModel : BaseViewModel
    {
        private bool _loading = false;

        public VaultGroupingsPageViewModel()
        {
            PageTitle = "My Vault";
            Items = new ExtendedObservableCollection<VaultGroupingsPageListItem>();
            LoadCommand = new Command(async () => await LoadAsync());
        }

        public bool Loading
        {
            get => _loading;
            set => SetProperty(ref _loading, value);
        }
        public ExtendedObservableCollection<VaultGroupingsPageListItem> Items { get; set; }
        public Command LoadCommand { get; set; }

        public Task LoadAsync()
        {
            if(Loading)
            {
                return Task.FromResult(0);
            }
            Loading = true;

            try
            {
                Items.ResetWithRange(new List<VaultGroupingsPageListItem>
                {
                    new VaultGroupingsPageListItem
                    {
                        Cipher = new CipherView { Name = "Cipher 1" }
                    },
                    new VaultGroupingsPageListItem
                    {
                        Cipher = new CipherView { Name = "Cipher 2" }
                    },
                    new VaultGroupingsPageListItem
                    {
                        Cipher = new CipherView { Name = "Cipher 3" }
                    },
                    new VaultGroupingsPageListItem
                    {
                        Cipher = new CipherView { Name = "Cipher 4" }
                    },
                    new VaultGroupingsPageListItem
                    {
                        Folder = new FolderView { Name = "Folder 1" }
                    },
                    new VaultGroupingsPageListItem
                    {
                        Folder = new FolderView { Name = "Folder 2" }
                    },
                    new VaultGroupingsPageListItem
                    {
                        Folder = new FolderView { Name = "Folder 3" }
                    },
                    new VaultGroupingsPageListItem
                    {
                        Collection = new Core.Models.View.CollectionView { Name = "Collection 1" }
                    },
                    new VaultGroupingsPageListItem
                    {
                        Collection = new Core.Models.View.CollectionView { Name = "Collection 2" }
                    },
                });
            }
            finally
            {
                Loading = false;
            }

            return Task.FromResult(0);
        }
    }

    public class VaultGroupingsPageListItem
    {
        public FolderView Folder { get; set; }
        public Core.Models.View.CollectionView Collection { get; set; }
        public CipherView Cipher { get; set; }
    }
}
