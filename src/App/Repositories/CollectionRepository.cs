using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Data;

namespace Bit.App.Repositories
{
    public class CollectionRepository : Repository<CollectionData, string>, ICollectionRepository
    {
        public CollectionRepository(ISqlService sqlService)
            : base(sqlService)
        { }

        public Task<IEnumerable<CollectionData>> GetAllByUserIdAsync(string userId)
        {
            var folders = Connection.Table<CollectionData>().Where(f => f.UserId == userId).Cast<CollectionData>();
            return Task.FromResult(folders);
        }
    }
}
