using System.Collections.Generic;
using Bit.Core.Enums;
using Bit.Core.Models.Response;

namespace Bit.Core.Models.Data
{
    public class PolicyData : Data
    {
        public PolicyData() { }

        public PolicyData(PolicyResponse response)
        {
            Id = response.Id;
            OrganizationId = response.OrganizationId;
            Type = response.Type;
            Data = response.Data;
            Enabled = response.Enabled;
        }

        public string Id { get; set; }
        public string OrganizationId { get; set; }
        public PolicyType Type { get; set; }
        public Dictionary<string, object> Data { get; set; }
        public bool Enabled { get; set; }
    }
}
