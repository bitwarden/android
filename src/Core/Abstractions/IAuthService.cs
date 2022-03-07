using System;
using System.Threading.Tasks;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request.IdentityToken;

namespace Bit.Core.Abstractions
{
    public interface IAuthService
    {
        string Email { get; }
        string MasterPasswordHash { get; }

        Task<AuthResult> LogInAsync(LogInCredentials credentials);
        Task<AuthResult> LogInTwoFactorAsync(TokenRequestTwoFactor twoFactor, string captchaResponse);
        void LogOut(Action callback);
        Task<SymmetricCryptoKey> MakePreloginKeyAsync(string masterPassword, string email);
        bool AuthingWithSso();
        bool AuthingWithPassword();
    }
}
