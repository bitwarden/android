using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Data;

namespace Bit.App.Repositories
{
    public class LoginRepository : Repository<LoginData, string>, ILoginRepository
    {
        public LoginRepository(ISqlService sqlService)
            : base(sqlService)
        { }

        public Task<IEnumerable<LoginData>> GetAllByUserIdAsync(string userId)
        {
            var logins = Connection.Table<LoginData>().Where(l => l.UserId == userId).Cast<LoginData>();
            return Task.FromResult(logins);
        }

        public Task<IEnumerable<LoginData>> GetAllByUserIdAsync(string userId, bool favorite)
        {
            var logins = Connection.Table<LoginData>().Where(l => l.UserId == userId && l.Favorite == favorite)
                .Cast<LoginData>();
            return Task.FromResult(logins);
        }
    }
}
