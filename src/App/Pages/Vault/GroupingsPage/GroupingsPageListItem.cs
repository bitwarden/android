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

        public string Name
        {
            get
            {
                if (_name != null)
                {
                    return _name;
                }
                if (Folder != null)
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
                if (Folder != null)
                {
                    _icon = Folder.Id == null ? "" : "";
                }
                else if (Collection != null)
                {
                    _icon = "";
                }
                else if (Type != null)
                {
                    switch (Type.Value)
                    {
                        case CipherType.Login:
                            _icon = "";
                            break;
                        case CipherType.SecureNote:
                            _icon = "";
                            break;
                        case CipherType.Card:
                            _icon = "";
                            break;
                        case CipherType.Identity:
                            _icon = "";
                            break;
                        default:
                            _icon = "";
                            break;
                    }
                }
                return _icon;
            }
        }
    }
}
