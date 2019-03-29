using Bit.Core.Models.View;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class GroupingsPageViewModel : BaseViewModel
    {
        private bool _loading = false;

        public GroupingsPageViewModel()
        {
            PageTitle = "My Vault";
            Items = new ExtendedObservableCollection<GroupingsPageListItem>();
            LoadCommand = new Command(async () => await LoadAsync());
        }

        public bool Loading
        {
            get => _loading;
            set => SetProperty(ref _loading, value);
        }
        public ExtendedObservableCollection<GroupingsPageListItem> Items { get; set; }
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
                Items.ResetWithRange(new List<GroupingsPageListItem>
                {
                    new GroupingsPageListItem
                    {
                        Cipher = new CipherView { Name = "Cipher 1" }
                    },
                    new GroupingsPageListItem
                    {
                        Cipher = new CipherView { Name = "Cipher 2" }
                    },
                    new GroupingsPageListItem
                    {
                        Cipher = new CipherView { Name = "Cipher 3" }
                    },
                    new GroupingsPageListItem
                    {
                        Cipher = new CipherView { Name = "Cipher 4" }
                    },
                    new GroupingsPageListItem
                    {
                        Folder = new FolderView { Name = "Folder 1" }
                    },
                    new GroupingsPageListItem
                    {
                        Folder = new FolderView { Name = "Folder 2" }
                    },
                    new GroupingsPageListItem
                    {
                        Folder = new FolderView { Name = "Folder 3" }
                    },
                    new GroupingsPageListItem
                    {
                        Collection = new Core.Models.View.CollectionView { Name = "Collection 1" }
                    },
                    new GroupingsPageListItem
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
}
