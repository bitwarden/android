using Bit.App.Models.Api;
using Bit.App.Models.Data;

namespace Bit.App.Models
{
    public class Site : Cipher
    {
        public Site()
        { }

        public Site(SiteData data)
        {
            Id = data.Id;
            ServerId = data.ServerId;
            FolderId = data.FolderId;
            ServerFolderId = data.ServerFolderId;
            Name = data.Name != null ? new CipherString(data.Name) : null;
            Uri = data.Uri != null ? new CipherString(data.Uri) : null;
            Username = data.Username != null ? new CipherString(data.Username) : null;
            Password = data.Password != null ? new CipherString(data.Password) : null;
            Notes = data.Notes != null ? new CipherString(data.Notes) : null;
        }

        public Site(SiteResponse response)
        {
            ServerId = response.Id;
            ServerFolderId = response.FolderId;
            Name = response.Name != null ? new CipherString(response.Name) : null;
            Uri = response.Uri != null ? new CipherString(response.Uri) : null;
            Username = response.Username != null ? new CipherString(response.Username) : null;
            Password = response.Password != null ? new CipherString(response.Password) : null;
            Notes = response.Notes != null ? new CipherString(response.Notes) : null;
        }

        public int? FolderId { get; set; }
        public string ServerFolderId { get; set; }
        public CipherString Uri { get; set; }
        public CipherString Username { get; set; }
        public CipherString Password { get; set; }
        public CipherString Notes { get; set; }

        public SiteData ToSiteData()
        {
            return new SiteData(this);
        }
    }
}
