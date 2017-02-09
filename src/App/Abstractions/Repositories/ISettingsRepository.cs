using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface ISettingsRepository
    {
        Task<IEnumerable<IEnumerable<string>>> GetEquivablentDomains(string userId);
    }
}
