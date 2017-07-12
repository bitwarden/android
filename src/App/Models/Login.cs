using Bit.App.Models.Api;
using Bit.App.Models.Data;
using System.Collections.Generic;
using System.Linq;

namespace Bit.App.Models
{
    public class Login
    {
        public Login()
        { }

        public Login(LoginData data, IEnumerable<AttachmentData> attachments = null)
        {
            Id = data.Id;
            UserId = data.UserId;
            OrganizationId = data.OrganizationId;
            FolderId = data.FolderId;
            Name = data.Name != null ? new CipherString(data.Name) : null;
            Uri = data.Uri != null ? new CipherString(data.Uri) : null;
            Username = data.Username != null ? new CipherString(data.Username) : null;
            Password = data.Password != null ? new CipherString(data.Password) : null;
            Notes = data.Notes != null ? new CipherString(data.Notes) : null;
            Totp = data.Totp != null ? new CipherString(data.Totp) : null;
            Favorite = data.Favorite;
            Edit = data.Edit;
            OrganizationUseTotp = data.OrganizationUseTotp;
            Attachments = attachments?.Select(a => new Attachment(a));
        }

        public Login(LoginResponse response)
        {
            Id = response.Id;
            UserId = response.UserId;
            OrganizationId = response.OrganizationId;
            FolderId = response.FolderId;
            Name = response.Name != null ? new CipherString(response.Name) : null;
            Uri = response.Uri != null ? new CipherString(response.Uri) : null;
            Username = response.Username != null ? new CipherString(response.Username) : null;
            Password = response.Password != null ? new CipherString(response.Password) : null;
            Notes = response.Notes != null ? new CipherString(response.Notes) : null;
            Totp = response.Totp != null ? new CipherString(response.Totp) : null;
            Favorite = response.Favorite;
            Edit = response.Edit;
            OrganizationUseTotp = response.OrganizationUseTotp;
            Attachments = response.Attachments?.Select(a => new Attachment(a));
        }

        public string Id { get; set; }
        public string UserId { get; set; }
        public string OrganizationId { get; set; }
        public string FolderId { get; set; }
        public CipherString Name { get; set; }
        public CipherString Uri { get; set; }
        public CipherString Username { get; set; }
        public CipherString Password { get; set; }
        public CipherString Notes { get; set; }
        public CipherString Totp { get; set; }
        public bool Favorite { get; set; }
        public bool Edit { get; set; }
        public bool OrganizationUseTotp { get; set; }
        public IEnumerable<Attachment> Attachments { get; set; }

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
