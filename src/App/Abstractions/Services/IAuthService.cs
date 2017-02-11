using System.Threading.Tasks;
using Bit.App.Models.Api;
using System;

namespace Bit.App.Abstractions
{
    public interface IAuthService
    {
        bool IsAuthenticated { get; }
        string UserId { get; set; }
        string PreviousUserId { get; }
        bool UserIdChanged { get; }
        string Email { get; set; }
        string PIN { get; set; }

        void LogOut();
        Task<ApiResult<TokenResponse>> TokenPostAsync(TokenRequest request);
    }
}
