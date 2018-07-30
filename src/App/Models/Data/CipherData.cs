using System;
using SQLite;
using Bit.App.Abstractions;
using Newtonsoft.Json;
using System.Linq;
using Bit.App.Enums;
using Bit.App.Models.Api;
using Newtonsoft.Json.Linq;

namespace Bit.App.Models.Data
{
    // Old table that has just carried over for backward compat. sake. Should really be "Cipher"
    [Table("Site")]
    public class CipherData : IDataObject<string>
    {
        public CipherData()
        { }

        public CipherData(CipherResponse cipher, string userId)
        {
            Id = cipher.Id;
            FolderId = cipher.FolderId;
            UserId = userId;
            OrganizationId = cipher.OrganizationId;
            Favorite = cipher.Favorite;
            Edit = cipher.Edit;
            OrganizationUseTotp = cipher.OrganizationUseTotp;
            RevisionDateTime = cipher.RevisionDate;
            Type = cipher.Type;
            Data = null;

            switch(cipher.Type)
            {
                case CipherType.Login:
                    var loginObj = JObject.FromObject(new LoginDataModel(cipher),
                        new JsonSerializer { NullValueHandling = NullValueHandling.Ignore });
                    loginObj[nameof(LoginDataModel.Uri)]?.Parent?.Remove();
                    Login = loginObj.ToString(Formatting.None);
                    break;
                case CipherType.SecureNote:
                    var noteData = new SecureNoteDataModel(cipher);
                    SecureNote = JsonConvert.SerializeObject(noteData);
                    break;
                case CipherType.Card:
                    var cardData = new CardDataModel(cipher);
                    Card = JsonConvert.SerializeObject(cardData);
                    break;
                case CipherType.Identity:
                    var idData = new IdentityDataModel(cipher);
                    Identity = JsonConvert.SerializeObject(idData);
                    break;
                default:
                    throw new ArgumentException(nameof(cipher.Type));
            }

            Name = cipher.Name;
            Notes = cipher.Notes;

            if(cipher.Fields != null && cipher.Fields.Any())
            {
                try
                {
                    Fields = JsonConvert.SerializeObject(cipher.Fields.Select(f => new FieldDataModel(f)));
                }
                catch(JsonSerializationException) { }
            }

            if(cipher.PasswordHistory != null && cipher.PasswordHistory.Any())
            {
                try
                {
                    PasswordHistory = JsonConvert.SerializeObject(
                        cipher.PasswordHistory.Select(h => new PasswordHistoryDataModel(h)));
                }
                catch(JsonSerializationException) { }
            }
        }

        [PrimaryKey]
        public string Id { get; set; }
        public string FolderId { get; set; }
        [Indexed]
        public string UserId { get; set; }
        public string OrganizationId { get; set; }
        public string Name { get; set; }
        public string Notes { get; set; }
        public string Fields { get; set; }
        public string PasswordHistory { get; set; }
        public string Login { get; set; }
        public string Card { get; set; }
        public string Identity { get; set; }
        public string SecureNote { get; set; }
        public bool Favorite { get; set; }
        public bool Edit { get; set; }
        public bool OrganizationUseTotp { get; set; }
        public DateTime RevisionDateTime { get; set; } = DateTime.UtcNow;
        [Indexed]
        public CipherType Type { get; set; } = CipherType.Login;
        [Obsolete]
        public string Data { get; set; }
    }
}
