using System;

namespace Bit.App.Abstractions
{
    public interface ITokenService
    {
        string Token { get; set; }
        string RefreshToken { get; set; }
        string GetTwoFactorToken(string email);
        void SetTwoFactorToken(string email, string token);
        DateTime TokenExpiration { get; }
        string TokenIssuer { get; }
        bool TokenExpired { get; }
        TimeSpan TokenTimeRemaining { get; }
        bool TokenNeedsRefresh { get; }
        string TokenUserId { get; }
        string TokenEmail { get; }
        string TokenName { get; }
        bool TokenPremium { get; }
    }
}
