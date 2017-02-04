using System.Threading.Tasks;
using Bit.App.Models.Api;

namespace Bit.App.Abstractions
{
    public interface IConnectApiRepository
    {
        Task<ApiResult<TokenResponse>> PostTokenAsync(TokenRequest requestObj);
    }
}