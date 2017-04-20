using System;

namespace Bit.App.Models.Api
{
    public class LoginResponse
    {
        public string Id { get; set; }
        public string FolderId { get; set; }
        public string UserId { get; set; }
        public string OrganizationId { get; set; }
        public string Name { get; set; }
        public string Uri { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string Notes { get; set; }
        public bool Favorite { get; set; }
        public DateTime RevisionDate { get; set; }
    }
}
