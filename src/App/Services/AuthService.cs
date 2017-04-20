using System;
using System.Text;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Plugin.Settings.Abstractions;
using Bit.App.Models;
using System.Linq;
using System.Collections.Generic;
using System.Diagnostics;

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
        private readonly IAccountsApiRepository _accountsApiRepository;
        private readonly IAppIdService _appIdService;
        private readonly IDeviceInfoService _deviceInfoService;

        private string _email;
        private string _userId;
        private string _previousUserId;
        private string _pin;

        public AuthService(
            ISecureStorageService secureStorage,
            ITokenService tokenService,
            ISettings settings,
            ICryptoService cryptoService,
            IConnectApiRepository connectApiRepository,
            IAccountsApiRepository accountsApiRepository,
            IAppIdService appIdService,
            IDeviceInfoService deviceInfoService)
        {
            _secureStorage = secureStorage;
            _tokenService = tokenService;
            _settings = settings;
            _cryptoService = cryptoService;
            _connectApiRepository = connectApiRepository;
            _accountsApiRepository = accountsApiRepository;
            _appIdService = appIdService;
            _deviceInfoService = deviceInfoService;
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
            _cryptoService.ClearKeys();
            _settings.Remove(Constants.FirstVaultLoad);
            _settings.Remove(Constants.PushLastRegistrationDate);
            _settings.Remove(Constants.Locked);
        }

        public async Task<FullLoginResult> TokenPostAsync(string email, string masterPassword)
        {
            var result = new FullLoginResult();

            var normalizedEmail = email.Trim().ToLower();
            var key = _cryptoService.MakeKeyFromPassword(masterPassword, normalizedEmail);

            var request = new TokenRequest
            {
                Email = normalizedEmail,
                MasterPasswordHash = _cryptoService.HashPasswordBase64(key, masterPassword),
                Device = new DeviceRequest(_appIdService, _deviceInfoService)
            };

            var response = await _connectApiRepository.PostTokenAsync(request);
            if(!response.Succeeded)
            {
                result.Success = false;
                result.ErrorMessage = response.Errors.FirstOrDefault()?.Message;
                return result;
            }

            result.Success = true;
            if(response.Result.TwoFactorProviders != null && response.Result.TwoFactorProviders.Count > 0)
            {
                result.Key = key;
                result.MasterPasswordHash = request.MasterPasswordHash;
                result.TwoFactorRequired = true;
                return result;
            }

            await ProcessLoginSuccessAsync(key, response.Result);
            return result;
        }

        public async Task<LoginResult> TokenPostTwoFactorAsync(string token, string email, string masterPasswordHash,
            CryptoKey key)
        {
            var result = new LoginResult();

            var request = new TokenRequest
            {
                Email = email.Trim().ToLower(),
                MasterPasswordHash = masterPasswordHash,
                Token = token.Trim().Replace(" ", ""),
                Provider = 0, // Authenticator app (only 1 provider for now, so hard coded)
                Device = new DeviceRequest(_appIdService, _deviceInfoService)
            };

            var response = await _connectApiRepository.PostTokenAsync(request);
            if(!response.Succeeded)
            {
                result.Success = false;
                result.ErrorMessage = response.Errors.FirstOrDefault()?.Message;
                return result;
            }

            result.Success = true;
            await ProcessLoginSuccessAsync(key, response.Result);
            return result;
        }

        private async Task ProcessLoginSuccessAsync(CryptoKey key, TokenResponse response)
        {
            if(response.PrivateKey != null)
            {
                _cryptoService.SetPrivateKey(new CipherString(response.PrivateKey), key);
            }

            _cryptoService.Key = key;
            _tokenService.Token = response.AccessToken;
            _tokenService.RefreshToken = response.RefreshToken;
            UserId = _tokenService.TokenUserId;
            Email = _tokenService.TokenEmail;
            _settings.AddOrUpdateValue(Constants.LastLoginEmail, Email);

            if(response.PrivateKey != null)
            {
                var profile = await _accountsApiRepository.GetProfileAsync();
                var orgKeysDict = new Dictionary<string, CryptoKey>();

                if(profile.Succeeded && (profile.Result.Organizations?.Any() ?? false))
                {
                    foreach(var org in profile.Result.Organizations)
                    {
                        try
                        {
                            var decBytes = _cryptoService.RsaDecryptToBytes(new CipherString(org.Key), null);
                            orgKeysDict.Add(org.Id, new CryptoKey(decBytes));
                        }
                        catch
                        {
                            Debug.WriteLine($"Cannot set org key {org.Id}. Decryption failed.");
                        }
                    }
                }

                _cryptoService.OrgKeys = orgKeysDict;
            }
        }
    }
}
