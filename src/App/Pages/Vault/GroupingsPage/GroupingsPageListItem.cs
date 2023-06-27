﻿using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Enums;
using Bit.Core.Models.View;

namespace Bit.App.Pages
{
    public class GroupingsPageListItem : IGroupingsPageListItem
    {
        private string _icon;
        private string _name;
        private string _automationId;

        public FolderView Folder { get; set; }
        public CollectionView Collection { get; set; }
        public CipherView Cipher { get; set; }
        public CipherType? Type { get; set; }
        public string ItemCount { get; set; }
        public bool FuzzyAutofill { get; set; }
        public bool IsTrash { get; set; }
        public bool IsTotpCode { get; set; }

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
                else if (IsTotpCode)
                {
                    _name = AppResources.VerificationCodes;
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
                    _icon = BitwardenIcons.Trash;
                }
                else if (Folder != null)
                {
                    _icon = BitwardenIcons.Folder;
                }
                else if (Collection != null)
                {
                    _icon = BitwardenIcons.Collection;
                }
                else if (IsTotpCode)
                {
                    _icon = BitwardenIcons.Clock;
                }
                else if (Type != null)
                {
                    switch (Type.Value)
                    {
                        case CipherType.Login:
                            _icon = BitwardenIcons.Globe;
                            break;
                        case CipherType.SecureNote:
                            _icon = BitwardenIcons.StickyNote;
                            break;
                        case CipherType.Card:
                            _icon = BitwardenIcons.CreditCard;
                            break;
                        case CipherType.Identity:
                            _icon = BitwardenIcons.IdCard;
                            break;
                        default:
                            _icon = BitwardenIcons.Globe;
                            break;
                    }
                }
                return _icon;
            }
        }

        public string AutomationId
        {
            get
            {
                if (Type != null)
                {
                    switch (Type.Value)
                    {
                        case CipherType.Login:
                            return "LoginFilter";
                        case CipherType.SecureNote:
                            return "SecureNoteFilter";
                        case CipherType.Card:
                            return "CardFilter";
                        case CipherType.Identity:
                            return "IdentityFilter";
                    }
                }
                else if (IsTrash)
                {
                    return "TrashFilter";
                }
                else if (Folder != null)
                {
                    return "FolderFilter";
                }
                else if (Collection != null)
                {
                    return "CollectionFilter";
                }
                else if (IsTotpCode)
                {
                    return "TOTPListItem";
                }
                
                return null;
            }
        }
    }     
}
