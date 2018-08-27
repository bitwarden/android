using System;
using System.Text;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Plugin.Settings.Abstractions;
using Bit.App.Models;
using System.Linq;
using Bit.App.Enums;
using Xamarin.Forms;
using Bit.App.Pages;
using Bit.App.Controls;
using XLabs.Ioc;

namespace Bit.App.Services
{
    public class AuthService : IAuthService
    {
        private const string EmailKey = "email";
        private const string KdfKey = "kdf";
        private const string KdfIterationsKey = "kdfIterations";
        private const string UserIdKey = "userId";
        private const string PreviousUserIdKey = "previousUserId";
        private const string PinKey = "pin";

        private readonly ISecureStorageService _secureStorage;
        private readonly ITokenService _tokenService;
        private readonly ISettings _settings;
        private readonly IAppSettingsService _appSettingsService;
        private readonly ICryptoService _cryptoService;
        private readonly IConnectApiRepository _connectApiRepository;
        private readonly IAccountsApiRepository _accountsApiRepository;
        private readonly IAppIdService _appIdService;
        private readonly IDeviceInfoService _deviceInfoService;
        private readonly IDeviceApiRepository _deviceApiRepository;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;

        private string _email;
        private KdfType? _kdf;
        private int? _kdfIterations;
        private string _userId;
        private string _previousUserId;
        private string _pin;

        public AuthService(
            ISecureStorageService secureStorage,
            ITokenService tokenService,
            ISettings settings,
            IAppSettingsService appSettingsService,
            ICryptoService cryptoService,
            IConnectApiRepository connectApiRepository,
            IAccountsApiRepository accountsApiRepository,
            IAppIdService appIdService,
            IDeviceInfoService deviceInfoService,
            IDeviceApiRepository deviceApiRepository,
            IGoogleAnalyticsService googleAnalyticsService)
        {
            _secureStorage = secureStorage;
            _tokenService = tokenService;
            _settings = settings;
            _appSettingsService = appSettingsService;
            _cryptoService = cryptoService;
            _connectApiRepository = connectApiRepository;
            _accountsApiRepository = accountsApiRepository;
            _appIdService = appIdService;
            _deviceInfoService = deviceInfoService;
            _deviceApiRepository = deviceApiRepository;
            _googleAnalyticsService = googleAnalyticsService;
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

        public KdfType Kdf
        {
            get
            {
                if(!_kdf.HasValue)
                {
                    _kdf = (KdfType)_settings.GetValueOrDefault(KdfKey, (short)KdfType.PBKDF2_SHA256);
                }
                return _kdf.Value;
            }
            set
            {
                _settings.AddOrUpdateValue(KdfKey, (short)value);
                _kdf = value;
            }
        }

        public int KdfIterations
        {
            get
            {
                if(!_kdfIterations.HasValue)
                {
                    _kdfIterations = _settings.GetValueOrDefault(KdfIterationsKey, 5000);
                }
                return _kdfIterations.Value;
            }
            set
            {
                _settings.AddOrUpdateValue(KdfIterationsKey, value);
                _kdfIterations = value;
            }
        }

        public bool IsAuthenticated
        {
            get
            {
                return _cryptoService.Key != null &&
                    !string.IsNullOrWhiteSpace(_tokenService.Token) &&
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

        public bool BelongsToOrganization(string orgId)
        {
            return !string.IsNullOrWhiteSpace(orgId) && (_cryptoService.OrgKeys?.ContainsKey(orgId) ?? false);
        }

        public void LogOut(string logoutMessage = null)
        {
            CipherService.CachedCiphers = null;
            _tokenService.Token = null;
            UserId = null;
            Email = null;
            _cryptoService.ClearKeys();
            _settings.Remove(Constants.SecurityStamp);
            _settings.Remove(Constants.PushLastRegistrationDate);
            _settings.Remove(Constants.Locked);

            Task.Run(async () => await _deviceApiRepository.PutClearTokenAsync(_appIdService.AppId));

            _googleAnalyticsService.TrackAppEvent("LoggedOut");

            Device.BeginInvokeOnMainThread(() => Application.Current.MainPage = new ExtendedNavigationPage(new HomePage()));
            if(!string.IsNullOrWhiteSpace(logoutMessage))
            {
                Resolver.Resolve<IDeviceActionService>()?.Toast(logoutMessage);
            }
        }

        public async Task<FullLoginResult> TokenPostAsync(string email, string masterPassword)
        {
            Kdf = KdfType.PBKDF2_SHA256;
            KdfIterations = 5000;
            var preloginResponse = await _accountsApiRepository.PostPreloginAsync(
                new PreloginRequest { Email = email });
            if(preloginResponse.Succeeded)
            {
                Kdf = preloginResponse.Result.Kdf;
                KdfIterations = preloginResponse.Result.KdfIterations;
            }

            var result = new FullLoginResult();

            var normalizedEmail = email.Trim().ToLower();
            var key = _cryptoService.MakeKeyFromPassword(masterPassword, normalizedEmail, Kdf, KdfIterations);

            var request = new TokenRequest
            {
                Email = normalizedEmail,
                MasterPasswordHash = _cryptoService.HashPasswordBase64(key, masterPassword),
                Device = new DeviceRequest(_appIdService, _deviceInfoService)
            };

            var twoFactorToken = _tokenService.GetTwoFactorToken(normalizedEmail);
            if(!string.IsNullOrWhiteSpace(twoFactorToken))
            {
                request.Token = twoFactorToken;
                request.Provider = TwoFactorProviderType.Remember;
                request.Remember = false;
            }

            var response = await _connectApiRepository.PostTokenAsync(request);
            if(!response.Succeeded)
            {
                result.Success = false;
                result.ErrorMessage = response.Errors.FirstOrDefault()?.Message;
                return result;
            }

            result.Success = true;
            if(response.Result.TwoFactorProviders2 != null && response.Result.TwoFactorProviders2.Count > 0)
            {
                result.Key = key;
                result.MasterPasswordHash = request.MasterPasswordHash;
                result.TwoFactorProviders = response.Result.TwoFactorProviders2;
                return result;
            }

            await ProcessLoginSuccessAsync(key, response.Result);
            return result;
        }

        public async Task<LoginResult> TokenPostTwoFactorAsync(TwoFactorProviderType type, string token, bool remember,
            string email, string masterPasswordHash, SymmetricCryptoKey key)
        {
            var result = new LoginResult();

            var request = new TokenRequest
            {
                Remember = remember,
                Email = email.Trim().ToLower(),
                MasterPasswordHash = masterPasswordHash,
                Token = token,
                Provider = type,
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

        private async Task ProcessLoginSuccessAsync(SymmetricCryptoKey key, TokenResponse response)
        {
            if(response.Key != null)
            {
                _cryptoService.SetEncKey(new CipherString(response.Key));
            }

            if(response.PrivateKey != null)
            {
                _cryptoService.SetPrivateKey(new CipherString(response.PrivateKey));
            }

            _cryptoService.Key = key;
            _tokenService.Token = response.AccessToken;
            _tokenService.RefreshToken = response.RefreshToken;
            UserId = _tokenService.TokenUserId;
            Email = _tokenService.TokenEmail;
            _settings.AddOrUpdateValue(Constants.LastLoginEmail, Email);
            _appSettingsService.FailedPinAttempts = 0;

            if(response.PrivateKey != null)
            {
                var profile = await _accountsApiRepository.GetProfileAsync();
                if(profile.Succeeded)
                {
                    _cryptoService.SetOrgKeys(profile.Result);
                }
            }

            if(!string.IsNullOrWhiteSpace(response.TwoFactorToken))
            {
                _tokenService.SetTwoFactorToken(_tokenService.TokenEmail, response.TwoFactorToken);
            }
        }
    }
}
