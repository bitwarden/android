using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Data;
using System.Collections.Generic;
using System.Linq;

namespace Bit.App.Repositories
{
    public class CipherCollectionRepository : BaseRepository, ICipherCollectionRepository
    {
        public CipherCollectionRepository(ISqlService sqlService)
            : base(sqlService)
        { }

        public Task<IEnumerable<CipherCollectionData>> GetAllByUserIdAsync(string userId)
        {
            var cipherCollections = Connection.Table<CipherCollectionData>().Where(f => f.UserId == userId)
                .Cast<CipherCollectionData>();
            return Task.FromResult(cipherCollections);
        }

        public virtual Task InsertAsync(CipherCollectionData obj)
        {
            Connection.Insert(obj);
            return Task.FromResult(0);
        }

        public virtual Task DeleteAsync(CipherCollectionData obj)
        {
            Connection.Delete<CipherCollectionData>(obj.Id);
            return Task.FromResult(0);
        }

        public virtual Task DeleteByUserIdAsync(string userId)
        {
            Connection.Execute("DELETE FROM CipherCollection WHERE UserId = ?", userId);
            return Task.FromResult(0);
        }
    }
}
