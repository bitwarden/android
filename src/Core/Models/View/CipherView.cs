using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Resources.Localization;
using Bit.Core.Utilities;

namespace Bit.Core.Models.View
{
    public class CipherView : View, ILaunchableView
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
            CreationDate = c.CreationDate;
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
        public DateTime CreationDate { get; set; }
        public DateTime? DeletedDate { get; set; }
        public CipherRepromptType Reprompt { get; set; }
        public CipherKey Key { get; set; }
        
        public ItemView Item
        {
            get
            {
                switch (Type)
                {
                    case CipherType.Login:
                        return Login;
                    case CipherType.SecureNote:
                        return SecureNote;
                    case CipherType.Card:
                        return Card;
                    case CipherType.Identity:
                        return Identity;
                    default:
                        break;
                }
                return null;
            }
        }

        public List<KeyValuePair<string, LinkedIdType>> LinkedFieldOptions => Item.LinkedFieldOptions;
        public string SubTitle => Item.SubTitle;
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

        public string LinkedFieldI18nKey(LinkedIdType id)
        {
            return LinkedFieldOptions.Find(lfo => lfo.Value == id).Key;
        }

        public string ComparableName => Name + Login?.Username;

        public bool CanLaunch => Login?.CanLaunch == true;

        public string LaunchUri => Login?.LaunchUri;

        public bool IsClonable => OrganizationId is null;

        public bool HasFido2Credential => Type == CipherType.Login && Login?.HasFido2Credentials == true;

        public string GetMainFido2CredentialUsername()
        {
            return Login?.MainFido2Credential?.UserName
                    .FallbackOnNullOrWhiteSpace(Login?.MainFido2Credential?.UserDisplayName)
                    .FallbackOnNullOrWhiteSpace(Login?.Username)
                    .FallbackOnNullOrWhiteSpace(Name)
                    .FallbackOnNullOrWhiteSpace(AppResources.UnknownAccount);
        }
    }
}
