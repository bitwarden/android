using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Newtonsoft.Json.Linq;
using System;
using System.Text;
using System.Threading.Tasks;
using System.Linq;

namespace Bit.Core.Services
{
    public class TokenService : ITokenService
    {
        private readonly IStateService _stateService;

        private string _token;
        private JObject _decodedToken;
        private string _refreshToken;

        public TokenService(IStateService stateService)
        {
            _stateService = stateService;
        }

        public async Task SetTokensAsync(string accessToken, string refreshToken)
        {
            await Task.WhenAll(
                SetTokenAsync(accessToken),
                SetRefreshTokenAsync(refreshToken));
        }

        public async Task SetTokenAsync(string token)
        {
            _token = token;
            _decodedToken = null;

            if (await SkipTokenStorage())
            {
                // If we have a vault timeout and the action is log out, don't store token
                return;
            }
            
            // await _stateService.SetAccessTokenAsync(token);
        }

        public async Task<string> GetTokenAsync()
        {
            if (_token != null)
            {
                return _token;
            }
            _token = await _stateService.GetAccessTokenAsync();
            return _token;
        }

        public async Task SetRefreshTokenAsync(string refreshToken)
        {
            _refreshToken = refreshToken;

            if (await SkipTokenStorage())
            {
                // If we have a vault timeout and the action is log out, don't store token
                return;
            }
            
            // await _stateService.SetRefreshTokenAsync(refreshToken);
        }

        public async Task<string> GetRefreshTokenAsync()
        {
            if (_refreshToken != null)
            {
                return _refreshToken;
            }
            _refreshToken = await _stateService.GetRefreshTokenAsync();
            return _refreshToken;
        }

        public async Task ToggleTokensAsync()
        {
            var token = await GetTokenAsync();
            var refreshToken = await GetRefreshTokenAsync();
            if (await SkipTokenStorage())
            {
                await ClearTokenAsync();
                _token = token;
                _refreshToken = refreshToken;
                return;
            }

            await SetTokenAsync(token);
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
                _stateService.SetAccessTokenAsync(null, userId),
                _stateService.SetRefreshTokenAsync(null, userId));
        }

        public void ClearCache()
        {
            _token = null;
            _decodedToken = null;
            _refreshToken = null;
        }

        public JObject DecodeToken()
        {
            if (_decodedToken != null)
            {
                return _decodedToken;
            }
            if (_token == null)
            {
                throw new InvalidOperationException("Token not found.");
            }
            var parts = _token.Split('.');
            if (parts.Length != 3)
            {
                throw new InvalidOperationException("JWT must have 3 parts.");
            }
            var decodedBytes = CoreHelpers.Base64UrlDecode(parts[1]);
            if (decodedBytes == null || decodedBytes.Length < 1)
            {
                throw new InvalidOperationException("Cannot decode the token.");
            }
            _decodedToken = JObject.Parse(Encoding.UTF8.GetString(decodedBytes));
            return _decodedToken;
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
            if (_token == null)
            {
                await GetTokenAsync();
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
            return timeout.HasValue && action == "logOut";
        }
    }
}
