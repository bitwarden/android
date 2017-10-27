using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Data;

namespace Bit.App.Repositories
{
    public class CipherRepository : Repository<CipherData, string>, ICipherRepository
    {
        public CipherRepository(ISqlService sqlService)
            : base(sqlService)
        { }

        public Task<IEnumerable<CipherData>> GetAllByUserIdAsync(string userId)
        {
            var ciphers = Connection.Table<CipherData>().Where(l => l.UserId == userId).Cast<CipherData>();
            return Task.FromResult(ciphers);
        }

        public Task<IEnumerable<CipherData>> GetAllByUserIdAsync(string userId, bool favorite)
        {
            var ciphers = Connection.Table<CipherData>().Where(l => l.UserId == userId && l.Favorite == favorite)
                .Cast<CipherData>();
            return Task.FromResult(ciphers);
        }
    }
}
