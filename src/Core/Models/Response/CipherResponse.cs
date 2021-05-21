using Bit.Core.Enums;
using Bit.Core.Models.Api;
using System;
using System.Collections.Generic;

namespace Bit.Core.Models.Response
{
    public class CipherResponse
    {
        public string Id { get; set; }
        public string OrganizationId { get; set; }
        public string FolderId { get; set; }
        public Enums.CipherType Type { get; set; }
        public string Name { get; set; }
        public string Notes { get; set; }
        public List<FieldApi> Fields { get; set; }
        public LoginApi Login { get; set; }
        public CardApi Card { get; set; }
        public IdentityApi Identity { get; set; }
        public SecureNoteApi SecureNote { get; set; }
        public bool Favorite { get; set; }
        public bool Edit { get; set; }
        public bool ViewPassword { get; set; } = true; // Fallback for old server versions
        public bool OrganizationUseTotp { get; set; }
        public DateTime RevisionDate { get; set; }
        public List<AttachmentResponse> Attachments { get; set; }
        public List<PasswordHistoryResponse> PasswordHistory { get; set; }
        public List<string> CollectionIds { get; set; }
        public DateTime? DeletedDate { get; set; }
        public CipherRepromptType Reprompt { get; set; }
    }
}
