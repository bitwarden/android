using System.Collections.Generic;

namespace Bit.Core.Models.Domain
{
    public class State : Domain
    {
        public Dictionary<string, Account> Accounts { get; set; }
        public string ActiveUserId { get; set; }
    }
}
