using Bit.Core.Models.View;

namespace Bit.App.Pages
{
    public class GroupingsPageListItem
    {
        public FolderView Folder { get; set; }
        public Core.Models.View.CollectionView Collection { get; set; }
        public CipherView Cipher { get; set; }
    }
}
