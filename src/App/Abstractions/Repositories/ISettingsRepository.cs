using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models.Data;

namespace Bit.App.Abstractions
{
    public interface ISettingsRepository : IRepository<SettingsData, string>
    {
        Task<IEnumerable<IEnumerable<string>>> GetEquivablentDomains(string userId);
    }
}
