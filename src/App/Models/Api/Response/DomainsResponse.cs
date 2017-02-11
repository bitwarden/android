using System.Collections.Generic;

namespace Bit.App.Models.Api
{
    public class DomainsResponse
    {
        public IEnumerable<IEnumerable<string>> EquivalentDomains { get; set; }
        public IEnumerable<GlobalDomains> GlobalEquivalentDomains { get; set; }

        public class GlobalDomains
        {
            public byte Type { get; set; }
            public IEnumerable<string> Domains { get; set; }
            public bool Excluded { get; set; }
        }
    }
}
