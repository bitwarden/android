using System.Collections.Generic;
using Bit.Core.Enums;
using Bit.Core.Models.Data;

namespace Bit.Core.Models.Domain
{
    public class Policy : Domain
    {
        public const string MINUTES_KEY = "minutes";
        public const string ACTION_KEY = "action";
        public const string ACTION_LOCK = "lock";
        public const string ACTION_LOGOUT = "logOut";

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
        public Dictionary<string, object> Data { get; set; }
        public bool Enabled { get; set; }

        public int? GetInt(string key)
        {
            if (Data.TryGetValue(key, out var val) && val != null)
            {
                return (int)(long)val;
            }
            return null;
        }

        public bool? GetBool(string key)
        {
            if (Data.TryGetValue(key, out var val) && val != null)
            {
                return (bool)val;
            }
            return null;
        }

        public string GetString(string key)
        {
            if (Data.TryGetValue(key, out var val))
            {
                return (string)val;
            }
            return null;
        }
    }
}
