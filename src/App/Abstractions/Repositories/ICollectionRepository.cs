using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models.Data;

namespace Bit.App.Abstractions
{
    public interface ICollectionRepository : IRepository<CollectionData, string>
    {
        Task<IEnumerable<CollectionData>> GetAllByUserIdAsync(string userId);
    }
}
