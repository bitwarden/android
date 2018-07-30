using Bit.App.Enums;
using Bit.App.Models.Data;
using Newtonsoft.Json;
using System.Collections.Generic;
using System.Linq;

namespace Bit.App.Models
{
    public class Cipher
    {
        public Cipher()
        { }

        public Cipher(CipherData data, IEnumerable<AttachmentData> attachments = null)
        {
            Id = data.Id;
            UserId = data.UserId;
            OrganizationId = data.OrganizationId;
            FolderId = data.FolderId;
            Type = data.Type;
            Name = data.Name != null ? new CipherString(data.Name) : null;
            Notes = data.Notes != null ? new CipherString(data.Notes) : null;
            Favorite = data.Favorite;
            Edit = data.Edit;
            OrganizationUseTotp = data.OrganizationUseTotp;
            Attachments = attachments?.Select(a => new Attachment(a));
            RevisionDate = data.RevisionDateTime;

            switch(Type)
            {
                case CipherType.Login:
                    Login = new Login(data);
                    break;
                case CipherType.SecureNote:
                    SecureNote = new SecureNote(data);
                    break;
                case CipherType.Card:
                    Card = new Card(data);
                    break;
                case CipherType.Identity:
                    Identity = new Identity(data);
                    break;
                default:
                    break;
            }

            if(!string.IsNullOrWhiteSpace(data.Fields))
            {
                try
                {
                    var fieldModels = JsonConvert.DeserializeObject<IEnumerable<FieldDataModel>>(data.Fields);
                    Fields = fieldModels?.Select(f => new Field(f));
                }
                catch(JsonSerializationException) { }
            }

            if(!string.IsNullOrWhiteSpace(data.PasswordHistory))
            {
                try
                {
                    var phModels = JsonConvert.DeserializeObject<IEnumerable<PasswordHistoryDataModel>>(
                        data.PasswordHistory);
                    PasswordHistory = phModels?.Select(f => new PasswordHistory(f));
                }
                catch(JsonSerializationException) { }
            }
        }

        public string Id { get; set; }
        public string UserId { get; set; }
        public string OrganizationId { get; set; }
        public string FolderId { get; set; }
        public CipherType Type { get; set; }
        public CipherString Name { get; set; }
        public CipherString Notes { get; set; }
        public IEnumerable<Field> Fields { get; set; }
        public IEnumerable<PasswordHistory> PasswordHistory { get; set; }
        public bool Favorite { get; set; }
        public bool Edit { get; set; }
        public bool OrganizationUseTotp { get; set; }
        public IEnumerable<Attachment> Attachments { get; set; }
        public System.DateTime RevisionDate { get; set; }

        public Login Login { get; set; }
        public Identity Identity { get; set; }
        public Card Card { get; set; }
        public SecureNote SecureNote { get; set; }

        public System.DateTime? PasswordRevisionDisplayDate =>
            Login?.Password == null ? (System.DateTime?)null : Login.PasswordRevisionDate ?? RevisionDate;
    }
}
