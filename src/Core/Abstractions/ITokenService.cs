using System;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq;

namespace Bit.Core.Abstractions
{
    public interface ITokenService
    {
        Task ClearTokenAsync();
        Task ClearTwoFactorTokenAsync(string email);
        JObject DecodeToken();
        string GetEmail();
        bool GetEmailVerified();
        string GetIssuer();
        string GetName();
        bool GetPremium();
        Task<string> GetRefreshTokenAsync();
        Task<string> GetTokenAsync();
        DateTime? GetTokenExpirationDate();
        Task<string> GetTwoFactorTokenAsync(string email);
        string GetUserId();
        Task SetRefreshTokenAsync(string refreshToken);
        Task SetTokenAsync(string token);
        Task SetTokensAsync(string accessToken, string refreshToken);
        Task SetTwoFactorTokenAsync(string token, string email);
        bool TokenNeedsRefresh(int minutes = 5);
        int TokenSecondsRemaining();
    }
}