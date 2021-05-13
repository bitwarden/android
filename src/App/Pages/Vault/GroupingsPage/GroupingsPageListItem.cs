using Bit.App.Resources;
using Bit.Core.Enums;
using Bit.Core.Models.View;

namespace Bit.App.Pages
{
    public class GroupingsPageListItem
    {
        private string _icon;
        private string _name;

        public FolderView Folder { get; set; }
        public CollectionView Collection { get; set; }
        public CipherView Cipher { get; set; }
        public CipherType? Type { get; set; }
        public string ItemCount { get; set; }
        public bool FuzzyAutofill { get; set; }
        public bool IsTrash { get; set; }

        public string Name
        {
            get
            {
                if (_name != null)
                {
                    return _name;
                }
                if (IsTrash)
                {
                    _name = AppResources.Trash;
                }
                else if (Folder != null)
                {
                    _name = Folder.Name;
                }
                else if (Collection != null)
                {
                    _name = Collection.Name;
                }
                else if (Type != null)
                {
                    switch (Type.Value)
                    {
                        case CipherType.Login:
                            _name = AppResources.TypeLogin;
                            break;
                        case CipherType.SecureNote:
                            _name = AppResources.TypeSecureNote;
                            break;
                        case CipherType.Card:
                            _name = AppResources.TypeCard;
                            break;
                        case CipherType.Identity:
                            _name = AppResources.TypeIdentity;
                            break;
                        default:
                            break;
                    }
                }
                return _name;
            }
        }

        public string Icon
        {
            get
            {
                if (_icon != null)
                {
                    return _icon;
                }
                if (IsTrash)
                {
                    _icon = "\uf014"; // fa-trash-o
                }
                else if (Folder != null)
                {
                    _icon = Folder.Id == null ? "\uf115" : "\uf07c"; // fa-folder-open-o : fa-folder-open
                }
                else if (Collection != null)
                {
                    _icon = "\uf1b2"; // fa-cube
                }
                else if (Type != null)
                {
                    switch (Type.Value)
                    {
                        case CipherType.Login:
                            _icon = "\uf0ac"; // fa-globe
                            break;
                        case CipherType.SecureNote:
                            _icon = "\uf24a"; // fa-sticky-note-o
                            break;
                        case CipherType.Card:
                            _icon = "\uf09d"; // fa-credit-card
                            break;
                        case CipherType.Identity:
                            _icon = "\uf2c3"; // fa-id-card-o
                            break;
                        default:
                            _icon = "\uf0ac"; // fa-globe
                            break;
                    }
                }
                return _icon;
            }
        }
    }
}
