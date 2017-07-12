using Bit.App.Enums;
using System;
using Newtonsoft.Json.Linq;
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
        public JObject Data { get; set; }
        public IEnumerable<AttachmentResponse> Attachments { get; set; }
        public DateTime RevisionDate { get; set; }
    }
}
