using System.Collections.Generic;

namespace Bit.Core.Models.Response
{
    public class GlobalDomainResponse
    {
        public int Type { get; set; }
        public List<string> Domains { get; set; }
        public bool Excluded { get; set; }
    }
}
