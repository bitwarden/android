using System;
using Bit.Core.Enums;
using Bit.Core.Models.Api;

namespace Bit.Core.Models.Response
{
    public class SendResponse
    {
        public string Id { get; set; }
        public string AccessId { get; set; }
        public SendType Type { get; set; }
        public string Name { get; set; }
        public string Notes { get; set; }
        public SendFileApi File { get; set; }
        public SendTextApi Text { get; set; }
        public string Key { get; set; }
        public int? MaxAccessCount { get; set; }
        public int AccessCount { get; set; }
        public DateTime RevisionDate { get; set; }
        public DateTime? ExpirationDate { get; set; }
        public DateTime DeletionDate { get; set; }
        public string Password { get; set; }
        public bool Disabled { get; set; }
        public bool? HideEmail { get; set; }
    }
}
