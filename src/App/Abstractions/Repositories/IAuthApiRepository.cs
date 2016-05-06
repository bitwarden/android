using System.Threading.Tasks;
using Bit.App.Models.Api;

namespace Bit.App.Abstractions
{
    public interface IAuthApiRepository
    {
        Task<ApiResult<TokenResponse>> PostTokenAsync(TokenRequest requestObj);
        Task<ApiResult<TokenResponse>> PostTokenTwoFactorAsync(TokenTwoFactorRequest requestObj);
    }
}