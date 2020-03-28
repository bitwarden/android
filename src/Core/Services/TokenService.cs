using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Newtonsoft.Json.Linq;
using System;
using System.Text;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class TokenService : ITokenService
    {
        private readonly IStorageService _storageService;

        private string _token;
        private JObject _decodedToken;
        private string _refreshToken;

        private const string Keys_AccessToken = "accessToken";
        private const string Keys_RefreshToken = "refreshToken";
        private const string Keys_TwoFactorTokenFormat = "twoFactorToken_{0}";

        public TokenService(IStorageService storageService)
        {
            _storageService = storageService;
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
            await _storageService.SaveAsync(Keys_AccessToken, token);
        }

        public async Task<string> GetTokenAsync()
        {
            if (_token != null)
            {
                return _token;
            }
            _token = await _storageService.GetAsync<string>(Keys_AccessToken);
            return _token;
        }

        public async Task SetRefreshTokenAsync(string refreshToken)
        {
            _refreshToken = refreshToken;
            await _storageService.SaveAsync(Keys_RefreshToken, refreshToken);
        }

        public async Task<string> GetRefreshTokenAsync()
        {
            if (_refreshToken != null)
            {
                return _refreshToken;
            }
            _refreshToken = await _storageService.GetAsync<string>(Keys_RefreshToken);
            return _refreshToken;
        }

        public async Task SetTwoFactorTokenAsync(string token, string email)
        {
            await _storageService.SaveAsync(string.Format(Keys_TwoFactorTokenFormat, email), token);
        }

        public async Task<string> GetTwoFactorTokenAsync(string email)
        {
            return await _storageService.GetAsync<string>(string.Format(Keys_TwoFactorTokenFormat, email));
        }

        public async Task ClearTwoFactorTokenAsync(string email)
        {
            await _storageService.RemoveAsync(string.Format(Keys_TwoFactorTokenFormat, email));
        }

        public async Task ClearTokenAsync()
        {
            _token = null;
            _decodedToken = null;
            _refreshToken = null;
            await Task.WhenAll(
                _storageService.RemoveAsync(Keys_AccessToken),
                _storageService.RemoveAsync(Keys_RefreshToken));
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
            var decodedBytes = Base64UrlDecode(parts[1]);
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

        private byte[] Base64UrlDecode(string input)
        {
            var output = input;
            // 62nd char of encoding
            output = output.Replace('-', '+');
            // 63rd char of encoding
            output = output.Replace('_', '/');
            // Pad with trailing '='s
            switch (output.Length % 4)
            {
                case 0:
                    // No pad chars in this case
                    break;
                case 2:
                    // Two pad chars
                    output += "=="; break;
                case 3:
                    // One pad char
                    output += "="; break;
                default:
                    throw new InvalidOperationException("Illegal base64url string!");
            }
            // Standard base64 decoder
            return Convert.FromBase64String(output);
        }
    }
}
