using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models;
using Bit.App.Models.Api;
using System;

namespace Bit.App.Abstractions
{
    public interface ILoginService
    {
        Task<Login> GetByIdAsync(string id);
        Task<IEnumerable<Login>> GetAllAsync();
        Task<IEnumerable<Login>> GetAllAsync(bool favorites);
        Task<Tuple<IEnumerable<Login>, IEnumerable<Login>>> GetAllAsync(string uriString);
        Task<ApiResult<LoginResponse>> SaveAsync(Login login);
        Task<ApiResult> DeleteAsync(string id);
    }
}
