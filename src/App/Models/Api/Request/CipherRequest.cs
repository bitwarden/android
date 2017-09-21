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
            Notes = login.Notes?.EncryptedString;
            Favorite = login.Favorite;

            switch(Type)
            {
                case CipherType.Login:
                    Login = new LoginType(login);
                    break;
                default:
                    break;
            }
        }

        public CipherType Type { get; set; }
        public string OrganizationId { get; set; }
        public string FolderId { get; set; }
        public bool Favorite { get; set; }
        public string Name { get; set; }
        public string Notes { get; set; }
        public LoginType Login { get; set; }

        public class LoginType
        {
            public LoginType(Login login)
            {
                Uri = login.Uri?.EncryptedString;
                Username = login.Username?.EncryptedString;
                Password = login.Password?.EncryptedString;
                Totp = login.Totp?.EncryptedString;
            }

            public string Uri { get; set; }
            public string Username { get; set; }
            public string Password { get; set; }
            public string Totp { get; set; }
        }
    }
}
