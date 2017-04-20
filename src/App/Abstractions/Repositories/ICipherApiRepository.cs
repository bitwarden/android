using System;
using System.Threading.Tasks;
using Bit.App.Models.Api;

namespace Bit.App.Abstractions
{
    public interface ICipherApiRepository
    {
        Task<ApiResult<CipherResponse>> GetByIdAsync(string id);
        Task<ApiResult<ListResponse<CipherResponse>>> GetAsync();
    }
}