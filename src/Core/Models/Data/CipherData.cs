using Bit.Core.Models.Response;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Bit.Core.Models.Data
{
    public class CipherData : Data
    {
        public CipherData() { }

        public CipherData(CipherResponse response, string userId = null, HashSet<string> collectionIds = null)
        {
            Id = response.Id;
            OrganizationId = response.OrganizationId;
            FolderId = response.FolderId;
            UserId = userId;
            Edit = response.Edit;
            ViewPassword = response.ViewPassword;
            OrganizationUseTotp = response.OrganizationUseTotp;
            Favorite = response.Favorite;
            RevisionDate = response.RevisionDate;
            Type = response.Type;
            Name = response.Name;
            Notes = response.Notes;
            CollectionIds = collectionIds?.ToList() ?? response.CollectionIds;
            Reprompt = response.Reprompt;

            try // Added to address Issue (https://github.com/bitwarden/mobile/issues/1006)
            {
                switch (Type)
                {
                    case Enums.CipherType.Login:
                        Login = new LoginData(response.Login);
                        break;
                    case Enums.CipherType.SecureNote:
                        SecureNote = new SecureNoteData(response.SecureNote);
                        break;
                    case Enums.CipherType.Card:
                        Card = new CardData(response.Card);
                        break;
                    case Enums.CipherType.Identity:
                        Identity = new IdentityData(response.Identity);
                        break;
                    default:
                        break;
                }
            }
            catch
            {
                System.Diagnostics.Trace.WriteLine(new StringBuilder()
                        .Append("BitWarden CipherData constructor failed to initialize CyperType '")
                        .Append(Type)
                        .Append("'; id = {")
                        .Append(Id)
                        .AppendLine("}")
                    .ToString(), "BitWarden CipherData constructor");
            }

            Fields = response.Fields?.Select(f => new FieldData(f)).ToList();
            Attachments = response.Attachments?.Select(a => new AttachmentData(a)).ToList();
            PasswordHistory = response.PasswordHistory?.Select(ph => new PasswordHistoryData(ph)).ToList();
            DeletedDate = response.DeletedDate;
        }

        public string Id { get; set; }
        public string OrganizationId { get; set; }
        public string FolderId { get; set; }
        public string UserId { get; set; }
        public bool Edit { get; set; }
        public bool ViewPassword { get; set; } = true; // Fallback for old server versions
        public bool OrganizationUseTotp { get; set; }
        public bool Favorite { get; set; }
        public DateTime RevisionDate { get; set; }
        public Enums.CipherType Type { get; set; }
        public string Name { get; set; }
        public string Notes { get; set; }
        public LoginData Login { get; set; }
        public SecureNoteData SecureNote { get; set; }
        public CardData Card { get; set; }
        public IdentityData Identity { get; set; }
        public List<FieldData> Fields { get; set; }
        public List<AttachmentData> Attachments { get; set; }
        public List<PasswordHistoryData> PasswordHistory { get; set; }
        public List<string> CollectionIds { get; set; }
        public DateTime? DeletedDate { get; set; }
        public Enums.CipherRepromptType Reprompt { get; set; }
    }
}
