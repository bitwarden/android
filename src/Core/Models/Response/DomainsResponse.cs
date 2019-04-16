using System.Collections.Generic;

namespace Bit.Core.Models.Response
{
    public class DomainsResponse
    {
        public List<List<string>> EquivalentDomains { get; set; }
        public List<GlobalDomainResponse> GlobalEquivalentDomains { get; set; } = new List<GlobalDomainResponse>();
    }
}
