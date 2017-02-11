using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface ISettingsService
    {
        Task<IEnumerable<IEnumerable<string>>> GetEquivalentDomainsAsync();
    }
}
