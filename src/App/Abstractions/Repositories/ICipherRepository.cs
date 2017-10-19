using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models.Data;

namespace Bit.App.Abstractions
{
    public interface ICipherRepository : IRepository<CipherData, string>
    {
        Task<IEnumerable<CipherData>> GetAllByUserIdAsync(string userId);
        Task<IEnumerable<CipherData>> GetAllByUserIdAsync(string userId, bool favorite);
    }
}
