using Bit.App.Models;
using System.Threading.Tasks;

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
        Task<FullLoginResult> TokenPostAsync(string email, string masterPassword);
        Task<LoginResult> TokenPostTwoFactorAsync(string token, string email, string masterPasswordHash, byte[] key);
    }
}
