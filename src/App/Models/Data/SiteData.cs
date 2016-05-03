using System;
using SQLite;
using Bit.App.Abstractions;
using Bit.App.Models.Api;

namespace Bit.App.Models.Data
{
    [Table("Site")]
    public class SiteData : IDataObject<string>
    {
        public SiteData()
        { }

        public SiteData(Site site, string userId)
        {
            Id = site.Id;
            FolderId = site.FolderId;
            UserId = userId;
            Name = site.Name?.EncryptedString;
            Uri = site.Uri?.EncryptedString;
            Username = site.Username?.EncryptedString;
            Password = site.Password?.EncryptedString;
            Notes = site.Notes?.EncryptedString;
        }

        public SiteData(SiteResponse site, string userId)
        {
            Id = site.Id;
            FolderId = site.FolderId;
            UserId = userId;
            Name = site.Name;
            Uri = site.Uri;
            Username = site.Username;
            Password = site.Password;
            Notes = site.Notes;
        }

        [PrimaryKey]
        public string Id { get; set; }
        public string FolderId { get; set; }
        [Indexed]
        public string UserId { get; set; }
        public string Name { get; set; }
        public string Uri { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string Notes { get; set; }
        public DateTime RevisionDateTime { get; set; } = DateTime.UtcNow;

        public Site ToSite()
        {
            return new Site(this);
        }
    }
}
