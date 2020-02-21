using System.Collections.Generic;
using Bit.Core.Enums;

namespace Bit.Core.Models.Response
{
    public class PolicyResponse
    {
        public string Id { get; set; }
        public string OrganizationId { get; set; }
        public PolicyType Type { get; set; }
        public Dictionary<string, object> Data { get; set; }
        public bool Enabled { get; set; }
    }
}
