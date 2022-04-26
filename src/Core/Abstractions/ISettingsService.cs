using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface ISettingsService
    {
        Task ClearAsync(string userId);
        void ClearCache();
        Task<List<List<string>>> GetEquivalentDomainsAsync();
        Task SetEquivalentDomainsAsync(List<List<string>> equivalentDomains);
    }
}
