using System;

namespace Bit.App.Abstractions
{
    public interface ITokenService
    {
        string Token { get; set; }
        string RefreshToken { get; set; }
        [Obsolete("Old auth scheme")]
        string AuthBearer { get; set; }
        DateTime TokenExpiration { get; }
        bool TokenExpired { get; }
        TimeSpan TokenTimeRemaining { get; }
        bool TokenNeedseRefresh { get; }
        string TokenUserId { get; }
        string TokenEmail { get; }
        string TokenName { get; }
    }
}
