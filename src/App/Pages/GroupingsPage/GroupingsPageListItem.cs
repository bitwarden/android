using Bit.Core.Models.View;

namespace Bit.App.Pages
{
    public class GroupingsPageListItem
    {
        private string _icon;

        public FolderView Folder { get; set; }
        public CollectionView Collection { get; set; }
        public CipherView Cipher { get; set; }
        public string Icon
        {
            get
            {
                if(_icon != null)
                {
                    return _icon;
                }
                if(Folder != null)
                {
                    _icon = Folder.Id == null ? "" : "";
                }
                else if(Collection != null)
                {
                    _icon = "";
                }
                return _icon;
            }
        }
    }
}
