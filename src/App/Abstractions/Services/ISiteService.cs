using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models;

namespace Bit.App.Abstractions
{
    public interface ISiteService
    {
        Task<IEnumerable<Site>> GetAllAsync();
        Task SaveAsync(Site site);
    }
}
