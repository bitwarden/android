using System.Threading.Tasks;
using Bit.App.Models.Api;

namespace Bit.App.Abstractions
{
    public interface IAuthService
    {
        bool IsAuthenticated { get; }
        string Token { get; set; }
        string UserId { get; set; }
        string PIN { get; set; }

        void LogOut();
        Task<ApiResult<TokenResponse>> TokenPostAsync(TokenRequest request);
    }
}
