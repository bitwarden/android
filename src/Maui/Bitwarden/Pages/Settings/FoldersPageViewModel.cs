using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public class FoldersPageViewModel : BaseViewModel
    {
        private readonly IFolderService _folderService;

        private bool _showNoData;

        public FoldersPageViewModel()
        {
            _folderService = ServiceContainer.Resolve<IFolderService>("folderService");

            PageTitle = AppResources.Folders;
            Folders = new ExtendedObservableCollection<FolderView>();
        }

        public ExtendedObservableCollection<FolderView> Folders { get; set; }

        public bool ShowNoData
        {
            get => _showNoData;
            set => SetProperty(ref _showNoData, value);
        }

        public async Task InitAsync()
        {
            var folders = await _folderService.GetAllDecryptedAsync();
            // Remove "No Folder"
            if (folders?.Any() ?? false)
            {
                folders = folders.GetRange(0, folders.Count - 1);
            }
            Folders.ResetWithRange(folders ?? new List<FolderView>());
            ShowNoData = Folders.Count == 0;
        }
    }
}
