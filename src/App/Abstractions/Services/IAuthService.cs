using Bit.App.Enums;
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

        bool BelongsToOrganization(string orgId);
        void LogOut();
        Task<FullLoginResult> TokenPostAsync(string email, string masterPassword);
        Task<LoginResult> TokenPostTwoFactorAsync(TwoFactorProviderType type, string token, bool remember, string email,
            string masterPasswordHash, SymmetricCryptoKey key);
    }
}
