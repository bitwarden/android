using System;
using SQLite;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using System.Linq;

namespace Bit.App.Models.Data
{
    [Table("Site")]
    public class LoginData : IDataObject<string>
    {
        public LoginData()
        { }

        public LoginData(CipherResponse cipher, string userId)
        {
            if(cipher.Type != Enums.CipherType.Login)
            {
                throw new ArgumentException(nameof(cipher.Type));
            }

            var data = cipher.Data.ToObject<LoginDataModel>();

            Id = cipher.Id;
            FolderId = cipher.FolderId;
            UserId = userId;
            OrganizationId = cipher.OrganizationId;
            Name = data.Name;
            Uri = data.Uri;
            Username = data.Username;
            Password = data.Password;
            Notes = data.Notes;
            Totp = data.Totp;
            Favorite = cipher.Favorite;
            Edit = cipher.Edit;
            OrganizationUseTotp = cipher.OrganizationUseTotp;
            RevisionDateTime = cipher.RevisionDate;

            if(data.Fields != null && data.Fields.Any())
            {
                try
                {
                    Fields = JsonConvert.SerializeObject(data.Fields);
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
        public string Uri { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string Notes { get; set; }
        public string Totp { get; set; }
        public string Fields { get; set; }
        public bool Favorite { get; set; }
        public bool Edit { get; set; }
        public bool OrganizationUseTotp { get; set; }
        public DateTime RevisionDateTime { get; set; } = DateTime.UtcNow;

        public Login ToLogin()
        {
            return new Login(this);
        }
    }
}
