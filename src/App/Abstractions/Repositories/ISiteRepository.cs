using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models.Data;

namespace Bit.App.Abstractions
{
    public interface ILoginRepository : IRepository<LoginData, string>
    {
        Task<IEnumerable<LoginData>> GetAllByUserIdAsync(string userId);
        Task<IEnumerable<LoginData>> GetAllByUserIdAsync(string userId, bool favorite);
    }
}
