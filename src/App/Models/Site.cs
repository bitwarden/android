using Bit.App.Models.Api;
using Bit.App.Models.Data;

namespace Bit.App.Models
{
    public class Login : Cipher
    {
        public Login()
        { }

        public Login(LoginData data)
        {
            Id = data.Id;
            FolderId = data.FolderId;
            Name = data.Name != null ? new CipherString(data.Name) : null;
            Uri = data.Uri != null ? new CipherString(data.Uri) : null;
            Username = data.Username != null ? new CipherString(data.Username) : null;
            Password = data.Password != null ? new CipherString(data.Password) : null;
            Notes = data.Notes != null ? new CipherString(data.Notes) : null;
            Favorite = data.Favorite;
        }

        public Login(LoginResponse response)
        {
            Id = response.Id;
            FolderId = response.FolderId;
            Name = response.Name != null ? new CipherString(response.Name) : null;
            Uri = response.Uri != null ? new CipherString(response.Uri) : null;
            Username = response.Username != null ? new CipherString(response.Username) : null;
            Password = response.Password != null ? new CipherString(response.Password) : null;
            Notes = response.Notes != null ? new CipherString(response.Notes) : null;
            Favorite = response.Favorite;
        }

        public string FolderId { get; set; }
        public CipherString Uri { get; set; }
        public CipherString Username { get; set; }
        public CipherString Password { get; set; }
        public CipherString Notes { get; set; }
        public bool Favorite { get; set; }

        public LoginRequest ToLoginRequest()
        {
            return new LoginRequest(this);
        }

        public LoginData ToLoginData(string userId)
        {
            return new LoginData(this, userId);
        }
    }
}
