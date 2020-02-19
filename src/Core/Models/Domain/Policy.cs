using System.Collections.Generic;
using Bit.Core.Enums;
using Bit.Core.Models.Data;

namespace Bit.Core.Models.Domain
{
    public class Policy : Domain
    {
        public Policy() { }

        public Policy(PolicyData obj)
        {
            Id = obj.Id;
            OrganizationId = obj.OrganizationId;
            Type = obj.Type;
            Data = obj.Data;
            Enabled = obj.Enabled;
        }
        
        public string Id { get; set; }
        public string OrganizationId { get; set; }
        public PolicyType Type { get; set; }
        public Dictionary<string, PolicyData> Data { get; set; }
        public bool Enabled { get; set; }
    }
}
