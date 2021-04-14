using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using System;
using System.Collections.Generic;
using System.Linq;

namespace Bit.Core.Models.View
{
    public class CipherView : View
    {
        public CipherView() { }

        public CipherView(Cipher c)
        {
            Id = c.Id;
            OrganizationId = c.OrganizationId;
            FolderId = c.FolderId;
            Favorite = c.Favorite;
            OrganizationUseTotp = c.OrganizationUseTotp;
            Edit = c.Edit;
            ViewPassword = c.ViewPassword;
            Type = c.Type;
            LocalData = c.LocalData;
            CollectionIds = c.CollectionIds;
            RevisionDate = c.RevisionDate;
            DeletedDate = c.DeletedDate;
            Reprompt = c.Reprompt;
        }

        public string Id { get; set; }
        public string OrganizationId { get; set; }
        public string FolderId { get; set; }
        public string Name { get; set; }
        public string Notes { get; set; }
        public CipherType Type { get; set; }
        public bool Favorite { get; set; }
        public bool OrganizationUseTotp { get; set; }
        public bool Edit { get; set; }
        public bool ViewPassword { get; set; } = true;
        public Dictionary<string, object> LocalData { get; set; }
        public LoginView Login { get; set; }
        public IdentityView Identity { get; set; }
        public CardView Card { get; set; }
        public SecureNoteView SecureNote { get; set; }
        public List<AttachmentView> Attachments { get; set; }
        public List<FieldView> Fields { get; set; }
        public List<PasswordHistoryView> PasswordHistory { get; set; }
        public HashSet<string> CollectionIds { get; set; }
        public DateTime RevisionDate { get; set; }
        public DateTime? DeletedDate { get; set; }
        public CipherRepromptType Reprompt { get; set; }

        public string SubTitle
        {
            get
            {
                switch (Type)
                {
                    case CipherType.Login:
                        return Login.SubTitle;
                    case CipherType.SecureNote:
                        return SecureNote.SubTitle;
                    case CipherType.Card:
                        return Card.SubTitle;
                    case CipherType.Identity:
                        return Identity.SubTitle;
                    default:
                        break;
                }
                return null;
            }
        }

        public bool Shared => OrganizationId != null;
        public bool HasPasswordHistory => PasswordHistory?.Any() ?? false;
        public bool HasAttachments => Attachments?.Any() ?? false;
        public bool HasOldAttachments
        {
            get
            {
                if (HasAttachments)
                {
                    return Attachments.Any(a => a.Key == null);
                }
                return false;
            }
        }
        public bool HasFields => Fields?.Any() ?? false;
        public DateTime? PasswordRevisionDisplayDate
        {
            get
            {
                if (Type != CipherType.Login || Login == null)
                {
                    return null;
                }
                else if (string.IsNullOrWhiteSpace(Login.Password))
                {
                    return null;
                }
                return Login.PasswordRevisionDate;
            }
        }
        public bool IsDeleted => DeletedDate.HasValue;
    }
}
