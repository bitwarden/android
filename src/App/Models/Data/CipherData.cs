using System;
using SQLite;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using System.Linq;
using Bit.App.Enums;

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
            Data = JsonConvert.SerializeObject(cipher.Data);

            CipherDataModel cipherData = null;
            switch(cipher.Type)
            {
                case CipherType.Login:
                    var loginData = cipher.Data.ToObject<LoginDataModel>();
                    cipherData = loginData;

                    Uri = loginData.Uri;
                    Username = loginData.Username;
                    Password = loginData.Password;
                    Totp = loginData.Totp;
                    break;
                case CipherType.SecureNote:
                    var noteData = cipher.Data.ToObject<SecureNoteDataModel>();
                    cipherData = noteData;

                    SecureNoteType = noteData.Type;
                    break;
                case CipherType.Card:
                    var cardData = cipher.Data.ToObject<CardDataModel>();
                    cipherData = cardData;
                    break;
                case CipherType.Identity:
                    var idData = cipher.Data.ToObject<IdentityDataModel>();
                    cipherData = idData;
                    break;
                default:
                    throw new ArgumentException(nameof(cipher.Type));
            }

            Name = cipherData.Name;
            Notes = cipherData.Notes;

            if(cipherData.Fields != null && cipherData.Fields.Any())
            {
                try
                {
                    Fields = JsonConvert.SerializeObject(cipherData.Fields);
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
        public bool Favorite { get; set; }
        public bool Edit { get; set; }
        public bool OrganizationUseTotp { get; set; }
        public DateTime RevisionDateTime { get; set; } = DateTime.UtcNow;
        [Indexed]
        public CipherType Type { get; set; } = CipherType.Login;
        public string Data { get; set; }

        // Login metadata
        public string Uri { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string Totp { get; set; }

        // Secure Note metadata
        public SecureNoteType? SecureNoteType { get; set; }
    }
}
