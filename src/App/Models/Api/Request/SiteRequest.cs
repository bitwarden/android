namespace Bit.App.Models.Api
{
    public class SiteRequest
    {
        public SiteRequest(Site site)
        {
            FolderId = site.FolderId;
            Name = site.Name?.EncryptedString;
            Uri = site.Uri?.EncryptedString;
            Username = site.Username?.EncryptedString;
            Password = site.Password?.EncryptedString;
            Notes = site.Notes?.EncryptedString;
            Favorite = site.Favorite;
        }

        public string FolderId { get; set; }
        public string Name { get; set; }
        public string Uri { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string Notes { get; set; }
        public bool Favorite { get; set; }
    }
}
