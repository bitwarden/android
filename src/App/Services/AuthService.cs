using System;
using System.Text;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Plugin.Settings.Abstractions;

namespace Bit.App.Services
{
    public class AuthService : IAuthService
    {
        private const string EmailKey = "email";
        private const string UserIdKey = "userId";
        private const string PreviousUserIdKey = "previousUserId";
        private const string PinKey = "pin";

        private readonly ISecureStorageService _secureStorage;
        private readonly ITokenService _tokenService;
        private readonly ISettings _settings;
        private readonly ICryptoService _cryptoService;
        private readonly IConnectApiRepository _connectApiRepository;

        private string _email;
        private string _userId;
        private string _previousUserId;
        private string _pin;

        public AuthService(
            ISecureStorageService secureStorage,
            ITokenService tokenService,
            ISettings settings,
            ICryptoService cryptoService,
            IConnectApiRepository connectApiRepository)
        {
            _secureStorage = secureStorage;
            _tokenService = tokenService;
            _settings = settings;
            _cryptoService = cryptoService;
            _connectApiRepository = connectApiRepository;
        }

        public string UserId
        {
            get
            {
                if(!string.IsNullOrWhiteSpace(_userId))
                {
                    return _userId;
                }

                var userId = _settings.GetValueOrDefault(UserIdKey, string.Empty);
                if(!string.IsNullOrWhiteSpace(userId))
                {
                    _userId = userId;
                }

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
                    PreviousUserId = _userId;
                    _settings.Remove(UserIdKey);
                }

                _userId = value;
            }
        }

        public string PreviousUserId
        {
            get
            {
                if(!string.IsNullOrWhiteSpace(_previousUserId))
                {
                    return _previousUserId;
                }

                var previousUserId = _settings.GetValueOrDefault(PreviousUserIdKey, string.Empty);
                if(!string.IsNullOrWhiteSpace(previousUserId))
                {
                    _previousUserId = previousUserId;
                }

                return _previousUserId;
            }
            private set
            {
                if(value != null)
                {
                    _settings.AddOrUpdateValue(PreviousUserIdKey, value);
                    _previousUserId = value;
                }
            }
        }

        public bool UserIdChanged => PreviousUserId != UserId;

        public string Email
        {
            get
            {
                if(!string.IsNullOrWhiteSpace(_email))
                {
                    return _email;
                }

                var email = _settings.GetValueOrDefault(EmailKey, string.Empty);
                if(!string.IsNullOrWhiteSpace(email))
                {
                    _email = email;
                }

                return _email;
            }
            set
            {
                if(value != null)
                {
                    _settings.AddOrUpdateValue(EmailKey, value);
                }
                else
                {
                    _settings.Remove(EmailKey);
                }

                _email = value;
            }
        }

        public bool IsAuthenticated
        {
            get
            {
                return _cryptoService.Key != null &&
                    (!string.IsNullOrWhiteSpace(_tokenService.Token) ||
                        !string.IsNullOrWhiteSpace(_tokenService.AuthBearer)) &&
                    !string.IsNullOrWhiteSpace(UserId);
            }
        }

        public string PIN
        {
            get
            {
                if(_pin != null)
                {
                    return _pin;
                }

                var pinBytes = _secureStorage.Retrieve(PinKey);
                if(pinBytes == null)
                {
                    return null;
                }

                _pin = Encoding.UTF8.GetString(pinBytes, 0, pinBytes.Length);
                return _pin;
            }
            set
            {
                if(value != null)
                {
                    var pinBytes = Encoding.UTF8.GetBytes(value);
                    _secureStorage.Store(PinKey, pinBytes);
                }
                else
                {
                    _secureStorage.Delete(PinKey);
                }

                _pin = value;
            }
        }

        public void LogOut()
        {
            _tokenService.Token = null;
            _tokenService.RefreshToken = null;
            _tokenService.AuthBearer = null;
            UserId = null;
            Email = null;
            _cryptoService.Key = null;
            _settings.Remove(Constants.FirstVaultLoad);
        }

        public async Task<ApiResult<TokenResponse>> TokenPostAsync(TokenRequest request)
        {
            // TODO: move more logic in here
            return await _connectApiRepository.PostTokenAsync(request);
        }
    }
}
