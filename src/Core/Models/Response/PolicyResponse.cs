using System.Collections.Generic;
using Bit.Core.Enums;
using Bit.Core.Models.Data;

namespace Bit.Core.Models.Response
{
    public class PolicyResponse
    {
        public string Id { get; set; }
        public string OrganizationId { get; set; }
        public PolicyType Type { get; set; }
        public Dictionary<string, PolicyData> Data { get; set; }
        public bool Enabled { get; set; }
    }
}
