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
            Favorite = site.Favorite;
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
            Favorite = site.Favorite;
            RevisionDateTime = site.RevisionDate;
        }

        public SiteData(CipherResponse cipher, string userId)
        {
            if(cipher.Type != Enums.CipherType.Site)
            {
                throw new ArgumentException(nameof(cipher.Type));
            }

            var data = cipher.Data.ToObject<SiteDataModel>();

            Id = cipher.Id;
            FolderId = cipher.FolderId;
            UserId = userId;
            Name = data.Name;
            Uri = data.Uri;
            Username = data.Username;
            Password = data.Password;
            Notes = data.Notes;
            Favorite = cipher.Favorite;
            RevisionDateTime = cipher.RevisionDate;
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
        public bool Favorite { get; set; }
        public DateTime RevisionDateTime { get; set; } = DateTime.UtcNow;

        public Site ToSite()
        {
            return new Site(this);
        }
    }
}
