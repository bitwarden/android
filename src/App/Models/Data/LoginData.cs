using System;
using SQLite;
using Bit.App.Abstractions;
using Bit.App.Models.Api;

namespace Bit.App.Models.Data
{
    [Table("Site")]
    public class LoginData : IDataObject<string>
    {
        public LoginData()
        { }

        public LoginData(Login login, string userId)
        {
            Id = login.Id;
            FolderId = login.FolderId;
            UserId = userId;
            OrganizationId = login.OrganizationId;
            Name = login.Name?.EncryptedString;
            Uri = login.Uri?.EncryptedString;
            Username = login.Username?.EncryptedString;
            Password = login.Password?.EncryptedString;
            Notes = login.Notes?.EncryptedString;
            Totp = login?.Notes?.EncryptedString;
            Favorite = login.Favorite;
            Edit = login.Edit;
            OrganizationUseTotp = login.OrganizationUseTotp;
        }

        public LoginData(LoginResponse login, string userId)
        {
            Id = login.Id;
            FolderId = login.FolderId;
            UserId = userId;
            OrganizationId = login.OrganizationId;
            Name = login.Name;
            Uri = login.Uri;
            Username = login.Username;
            Password = login.Password;
            Notes = login.Notes;
            Totp = login.Totp;
            Favorite = login.Favorite;
            RevisionDateTime = login.RevisionDate;
            Edit = login.Edit;
            OrganizationUseTotp = login.OrganizationUseTotp;
        }

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
