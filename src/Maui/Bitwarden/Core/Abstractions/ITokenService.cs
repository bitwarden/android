using System;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq;

namespace Bit.Core.Abstractions
{
    public interface ITokenService
    {
        Task ClearTokenAsync(string userId = null);
        Task ClearTwoFactorTokenAsync(string email);
        void ClearCache();
        JObject DecodeToken();
        string GetEmail();
        bool GetEmailVerified();
        string GetIssuer();
        string GetName();
        bool GetPremium();
        Task<bool> GetIsExternal();
        Task<string> GetRefreshTokenAsync();
        Task<string> GetTokenAsync();
        Task ToggleTokensAsync();
        DateTime? GetTokenExpirationDate();
        Task<string> GetTwoFactorTokenAsync(string email);
        string GetUserId();
        Task SetRefreshTokenAsync(string refreshToken);
        Task SetAccessTokenAsync(string token, bool forDecodeOnly = false);
        Task SetTokensAsync(string accessToken, string refreshToken);
        Task SetTwoFactorTokenAsync(string token, string email);
        bool TokenNeedsRefresh(int minutes = 5);
        int TokenSecondsRemaining();
        Task PrepareTokenForDecodingAsync();
    }
}
