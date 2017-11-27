using System.Threading.Tasks;
using Bit.App.Models.Data;
using System.Collections.Generic;

namespace Bit.App.Abstractions
{
    public interface ICipherCollectionRepository
    {
        Task<IEnumerable<CipherCollectionData>> GetAllByUserIdAsync(string userId);
        Task<IEnumerable<CipherCollectionData>> GetAllByUserIdCollectionAsync(string userId, string collectionId);
        Task InsertAsync(CipherCollectionData obj);
        Task DeleteAsync(CipherCollectionData obj);
        Task DeleteByUserIdAsync(string userId);
    }
}
