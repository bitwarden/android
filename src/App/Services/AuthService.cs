using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using Plugin.Settings.Abstractions;

namespace Bit.App.Services
{
    public class AuthService : IAuthService
    {
        private const string TokenKey = "token";
        private const string UserIdKey = "userId";

        private readonly ISecureStorageService _secureStorage;
        private readonly ISettings _settings;
        private readonly ICryptoService _cryptoService;
        private readonly IAuthApiRepository _authApiRepository;

        private string _token;
        private string _userId;

        public AuthService(
            ISecureStorageService secureStorage,
            ISettings settings,
            ICryptoService cryptoService,
            IAuthApiRepository authApiRepository)
        {
            _secureStorage = secureStorage;
            _settings = settings;
            _cryptoService = cryptoService;
            _authApiRepository = authApiRepository;
        }

        public string Token
        {
            get
            {
                if(_token != null)
                {
                    return _token;
                }

                var tokenBytes = _secureStorage.Retrieve(TokenKey);
                if(tokenBytes == null)
                {
                    return null;
                }

                _token = Encoding.UTF8.GetString(tokenBytes, 0, tokenBytes.Length);
                return _token;
            }
            set
            {
                if(value != null)
                {
                    var tokenBytes = Encoding.UTF8.GetBytes(value);
                    _secureStorage.Store(TokenKey, tokenBytes);
                }
                else
                {
                    _secureStorage.Delete(TokenKey);
                    _token = null;
                }
            }
        }

        public string UserId
        {
            get
            {
                if(_userId != null)
                {
                    return _userId;
                }

                _userId = _settings.GetValueOrDefault<string>(UserIdKey);
                return _userId;
            }
            set
            {
                if(value != null)
                {
                    _settings.AddOrUpdateValue(UserIdKey, value);
                }
                else
                {
                    _settings.Remove(UserIdKey);
                    _userId = null;
                }
            }
        }

        public bool IsAuthenticated
        {
            get
            {
                return _cryptoService.Key != null && Token != null && UserId != null;
            }
        }

        public void LogOut()
        {
            Token = null;
            UserId = null;
            _cryptoService.Key = null;
        }

        public async Task<ApiResult<TokenResponse>> TokenPostAsync(TokenRequest request)
        {
            // TODO: move more logic in here
            return await _authApiRepository.PostTokenAsync(request);
        }
    }
}
