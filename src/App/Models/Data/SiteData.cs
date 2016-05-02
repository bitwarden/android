using System;
using SQLite;
using Bit.App.Abstractions;

namespace Bit.App.Models.Data
{
    [Table("Site")]
    public class SiteData : IDataObject<int>
    {
        public SiteData()
        { }

        public SiteData(Site site)
        {
            Id = site.Id;
            ServerId = site.ServerId;
            FolderId = site.FolderId;
            ServerFolderId = site.ServerFolderId;
            Name = site.Name?.EncryptedString;
            Uri = site.Uri?.EncryptedString;
            Username = site.Username?.EncryptedString;
            Password = site.Password?.EncryptedString;
            Notes = site.Notes?.EncryptedString;
        }

        [PrimaryKey]
        [AutoIncrement]
        public int Id { get; set; }
        public string ServerId { get; set; }
        public int? FolderId { get; set; }
        public string ServerFolderId { get; set; }
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
