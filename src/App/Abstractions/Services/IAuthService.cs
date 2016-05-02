using System.Threading.Tasks;
using Bit.App.Models.Api;

namespace Bit.App.Abstractions
{
    public interface IAuthService
    {
        bool IsAuthenticated { get; }
        string Token { get; set; }

        Task<ApiResult<TokenResponse>> TokenPostAsync(TokenRequest request);
    }
}
