using Bit.App.Enums;

namespace Bit.App.Models.Api
{
    public class CipherRequest
    {
        public CipherRequest(Login login)
        {
            Type = CipherType.Login;
            OrganizationId = login.OrganizationId;
            FolderId = login.FolderId;
            Name = login.Name?.EncryptedString;
            Uri = login.Uri?.EncryptedString;
            Username = login.Username?.EncryptedString;
            Password = login.Password?.EncryptedString;
            Notes = login.Notes?.EncryptedString;
            Totp = login.Totp?.EncryptedString;
            Favorite = login.Favorite;
        }

        public CipherType Type { get; set; }
        public string OrganizationId { get; set; }
        public string FolderId { get; set; }
        public string Name { get; set; }
        public string Uri { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string Notes { get; set; }
        public string Totp { get; set; }
        public bool Favorite { get; set; }
    }
}
