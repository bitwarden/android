using System;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Newtonsoft.Json.Linq;

namespace Bit.Core.Services
{
    public class TokenService : ITokenService
    {
        private readonly IStateService _stateService;

        private string _accessTokenForDecoding;
        private JObject _decodedAccessToken;

        public TokenService(IStateService stateService)
        {
            _stateService = stateService;
        }

        public async Task SetTokensAsync(string accessToken, string refreshToken)
        {
            await Task.WhenAll(
                SetAccessTokenAsync(accessToken),
                SetRefreshTokenAsync(refreshToken));
        }

        public async Task SetAccessTokenAsync(string accessToken, bool forDecodeOnly = false)
        {
            _accessTokenForDecoding = accessToken;
            _decodedAccessToken = null;
            if (!forDecodeOnly)
            {
                await _stateService.SetAccessTokenAsync(accessToken, await SkipTokenStorage());
            }
        }

        public async Task<string> GetTokenAsync()
        {
            _accessTokenForDecoding = await _stateService.GetAccessTokenAsync();
            return _accessTokenForDecoding;
        }

        public async Task PrepareTokenForDecodingAsync()
        {
            _accessTokenForDecoding = await _stateService.GetAccessTokenAsync();
        }

        public async Task SetRefreshTokenAsync(string refreshToken)
        {
            await _stateService.SetRefreshTokenAsync(refreshToken, await SkipTokenStorage());
        }

        public async Task<string> GetRefreshTokenAsync()
        {
            return await _stateService.GetRefreshTokenAsync();
        }

        public async Task ToggleTokensAsync()
        {
            // load and re-save tokens to reflect latest value of SkipTokenStorage()
            var token = await GetTokenAsync();
            var refreshToken = await GetRefreshTokenAsync();
            await SetAccessTokenAsync(token);
            await SetRefreshTokenAsync(refreshToken);
        }

        public async Task SetTwoFactorTokenAsync(string token, string email)
        {
            await _stateService.SetTwoFactorTokenAsync(token, email);
        }

        public async Task<string> GetTwoFactorTokenAsync(string email)
        {
            return await _stateService.GetTwoFactorTokenAsync(email);
        }

        public async Task ClearTwoFactorTokenAsync(string email)
        {
            await _stateService.SetTwoFactorTokenAsync(null, email);
        }

        public async Task ClearTokenAsync(string userId = null)
        {
            ClearCache();
            await Task.WhenAll(
                _stateService.SetAccessTokenAsync(null, false, userId),
                _stateService.SetRefreshTokenAsync(null, false, userId));
        }

        public void ClearCache()
        {
            _accessTokenForDecoding = null;
            _decodedAccessToken = null;
        }

        public JObject DecodeToken()
        {
            if (_decodedAccessToken != null)
            {
                return _decodedAccessToken;
            }
            if (_accessTokenForDecoding == null)
            {
                throw new InvalidOperationException("Access token not found.");
            }
            var parts = _accessTokenForDecoding.Split('.');
            if (parts.Length != 3)
            {
                throw new InvalidOperationException("JWT must have 3 parts.");
            }
            var decodedBytes = CoreHelpers.Base64UrlDecode(parts[1]);
            if (decodedBytes == null || decodedBytes.Length < 1)
            {
                throw new InvalidOperationException("Cannot decode the token.");
            }
            _decodedAccessToken = JObject.Parse(Encoding.UTF8.GetString(decodedBytes));
            return _decodedAccessToken;
        }

        public DateTime? GetTokenExpirationDate()
        {
            var decoded = DecodeToken();
            if (decoded?["exp"] == null)
            {
                return null;
            }
            return CoreHelpers.Epoc.AddSeconds(Convert.ToDouble(decoded["exp"].Value<long>()));
        }

        public int TokenSecondsRemaining()
        {
            var d = GetTokenExpirationDate();
            if (d == null)
            {
                return 0;
            }
            var timeRemaining = d.Value - DateTime.UtcNow;
            return (int)timeRemaining.TotalSeconds;
        }

        public bool TokenNeedsRefresh(int minutes = 5)
        {
            var sRemaining = TokenSecondsRemaining();
            return sRemaining < (60 * minutes);
        }

        public string GetUserId()
        {
            var decoded = DecodeToken();
            if (decoded?["sub"] == null)
            {
                throw new Exception("No user id found.");
            }
            return decoded["sub"].Value<string>();
        }

        public string GetEmail()
        {
            var decoded = DecodeToken();
            if (decoded?["email"] == null)
            {
                throw new Exception("No email found.");
            }
            return decoded["email"].Value<string>();
        }

        public bool GetEmailVerified()
        {
            var decoded = DecodeToken();
            if (decoded?["email_verified"] == null)
            {
                throw new Exception("No email verification found.");
            }
            return decoded["email_verified"].Value<bool>();
        }

        public string GetName()
        {
            var decoded = DecodeToken();
            if (decoded?["name"] == null)
            {
                return null;
            }
            return decoded["name"].Value<string>();
        }

        public bool GetPremium()
        {
            var decoded = DecodeToken();
            if (decoded?["premium"] == null)
            {
                return false;
            }
            return decoded["premium"].Value<bool>();
        }

        public string GetIssuer()
        {
            var decoded = DecodeToken();
            if (decoded?["iss"] == null)
            {
                throw new Exception("No issuer found.");
            }
            return decoded["iss"].Value<string>();
        }

        public async Task<bool> GetIsExternal()
        {
            if (_accessTokenForDecoding == null)
            {
                await GetTokenAsync();
                if (_accessTokenForDecoding == null)
                {
                    return false;
                }
            }
            var decoded = DecodeToken();
            if (decoded?["amr"] == null)
            {
                return false;
            }
            return decoded["amr"].Value<JArray>().Any(t => t.Value<string>() == "external");
        }

        private async Task<bool> SkipTokenStorage()
        {
            var timeout = await _stateService.GetVaultTimeoutAsync();
            var action = await _stateService.GetVaultTimeoutActionAsync();
            return timeout.HasValue && action == VaultTimeoutAction.Logout;
        }
    }
}
