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
        public int AccessCount { get; internal set; }
        public DateTime RevisionDate { get; internal set; }
        public DateTime ExpirationDate { get; internal set; }
        public DateTime DeletionDate { get; internal set; }
        public string Password { get; set; }
        public bool Disable { get; set; }
    }
}
