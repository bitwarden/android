using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Models.Response;

namespace Bit.Core.Abstractions
{
    public interface IAuditService
    {
        Task<List<BreachAccountResponse>> BreachedAccountsAsync(string username);
        Task<int> PasswordLeakedAsync(string password);
    }
}
