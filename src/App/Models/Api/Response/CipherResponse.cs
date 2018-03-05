using Bit.App.Enums;
using System;
using System.Collections.Generic;

namespace Bit.App.Models.Api
{
    public class CipherResponse
    {
        public string Id { get; set; }
        public string FolderId { get; set; }
        public string UserId { get; set; }
        public string OrganizationId { get; set; }
        public CipherType Type { get; set; }
        public bool Favorite { get; set; }
        public bool Edit { get; set; }
        public bool OrganizationUseTotp { get; set; }
        public string Name { get; set; }
        public string Notes { get; set; }
        public LoginType Login { get; set; }
        public CardType Card { get; set; }
        public IdentityType Identity { get; set; }
        public SecureNoteType SecureNote { get; set; }
        public IEnumerable<FieldType> Fields { get; set; }
        public IEnumerable<AttachmentResponse> Attachments { get; set; }
        public IEnumerable<string> CollectionIds { get; set; }
        public DateTime RevisionDate { get; set; }
    }
}
