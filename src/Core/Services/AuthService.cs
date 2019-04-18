using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class AuthService : IAuthService
    {
        private readonly ICryptoService _cryptoService;
        private readonly IApiService _apiService;
        private readonly IUserService _userService;
        private readonly ITokenService _tokenService;
        private readonly IAppIdService _appIdService;
        private readonly II18nService _i18nService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IMessagingService _messagingService;
        private readonly bool _setCryptoKeys;

        private SymmetricCryptoKey _key;
        private KdfType? _kdf;
        private int? _kdfIterations;
        private Dictionary<TwoFactorProviderType, TwoFactorProvider> _twoFactorProviders;

        public AuthService(
            ICryptoService cryptoService,
            IApiService apiService,
            IUserService userService,
            ITokenService tokenService,
            IAppIdService appIdService,
            II18nService i18nService,
            IPlatformUtilsService platformUtilsService,
            IMessagingService messagingService,
            bool setCryptoKeys = true)
        {
            _cryptoService = cryptoService;
            _apiService = apiService;
            _userService = userService;
            _tokenService = tokenService;
            _appIdService = appIdService;
            _i18nService = i18nService;
            _platformUtilsService = platformUtilsService;
            _messagingService = messagingService;
            _setCryptoKeys = setCryptoKeys;

            _twoFactorProviders = new Dictionary<TwoFactorProviderType, TwoFactorProvider>();
            _twoFactorProviders.Add(TwoFactorProviderType.Authenticator, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.Authenticator,
                Priority = 1,
                Sort = 1
            });
            _twoFactorProviders.Add(TwoFactorProviderType.YubiKey, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.YubiKey,
                Priority = 3,
                Sort = 2,
                Premium = true
            });
            _twoFactorProviders.Add(TwoFactorProviderType.Duo, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.Duo,
                Name = "Duo",
                Priority = 2,
                Sort = 3,
                Premium = true
            });
            _twoFactorProviders.Add(TwoFactorProviderType.OrganizationDuo, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.OrganizationDuo,
                Name = "Duo (Organization)",
                Priority = 10,
                Sort = 4
            });
            _twoFactorProviders.Add(TwoFactorProviderType.U2f, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.U2f,
                Priority = 4,
                Sort = 5,
                Premium = true
            });
            _twoFactorProviders.Add(TwoFactorProviderType.Email, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.Email,
                Priority = 0,
                Sort = 6,
            });
        }

        public string Email { get; set; }
        public string MasterPasswordHash { get; set; }
        public Dictionary<TwoFactorProviderType, Dictionary<string, object>> TwoFactorProviders { get; set; }
        public TwoFactorProviderType? SelectedTwoFactorProviderType { get; set; }

        public void Init()
        {
            _twoFactorProviders[TwoFactorProviderType.Email].Name = _i18nService.T("EmailTitle");
            _twoFactorProviders[TwoFactorProviderType.Email].Description = _i18nService.T("EmailDesc");
            _twoFactorProviders[TwoFactorProviderType.Authenticator].Name = _i18nService.T("AuthenticatorAppTitle");
            _twoFactorProviders[TwoFactorProviderType.Authenticator].Description =
                _i18nService.T("AuthenticatorAppDesc");
            _twoFactorProviders[TwoFactorProviderType.Duo].Description = _i18nService.T("DuoDesc");
            _twoFactorProviders[TwoFactorProviderType.OrganizationDuo].Name =
                string.Format("Duo ({0})", _i18nService.T("Organization"));
            _twoFactorProviders[TwoFactorProviderType.OrganizationDuo].Description =
                _i18nService.T("DuoOrganizationDesc");
            _twoFactorProviders[TwoFactorProviderType.U2f].Name = _i18nService.T("U2fTitle");
            _twoFactorProviders[TwoFactorProviderType.U2f].Description = _i18nService.T("U2fDesc");
            _twoFactorProviders[TwoFactorProviderType.YubiKey].Name = _i18nService.T("YubiKeyTitle");
            _twoFactorProviders[TwoFactorProviderType.YubiKey].Description = _i18nService.T("YubiKeyDesc");
        }

        public async Task<AuthResult> LogInAsync(string email, string masterPassword)
        {
            SelectedTwoFactorProviderType = null;
            var key = await MakePreloginKeyAsync(masterPassword, email);
            var hashedPassword = await _cryptoService.HashPasswordAsync(masterPassword, key);
            return await LogInHelperAsync(email, hashedPassword, key);
        }

        public Task<AuthResult> LogInTwoFactorAsync(TwoFactorProviderType twoFactorProvider, string twoFactorToken,
            bool? remember = null)
        {
            return LogInHelperAsync(Email, MasterPasswordHash, _key, twoFactorProvider, twoFactorToken, remember);
        }

        public async Task<AuthResult> LogInCompleteAsync(string email, string masterPassword,
            TwoFactorProviderType twoFactorProvider, string twoFactorToken, bool? remember = null)
        {
            SelectedTwoFactorProviderType = null;
            var key = await MakePreloginKeyAsync(masterPassword, email);
            var hashedPassword = await _cryptoService.HashPasswordAsync(masterPassword, key);
            return await LogInHelperAsync(email, hashedPassword, key, twoFactorProvider, twoFactorToken, remember);
        }

        public void LogOut(Action callback)
        {
            callback.Invoke();
            _messagingService.Send("loggedOut");
        }

        public List<TwoFactorProvider> GetSupportedTwoFactorProviders()
        {
            var providers = new List<TwoFactorProvider>();
            if(TwoFactorProviders == null)
            {
                return providers;
            }
            if(TwoFactorProviders.ContainsKey(TwoFactorProviderType.OrganizationDuo) &&
                _platformUtilsService.SupportsDuo())
            {
                providers.Add(_twoFactorProviders[TwoFactorProviderType.OrganizationDuo]);
            }
            if(TwoFactorProviders.ContainsKey(TwoFactorProviderType.Authenticator))
            {
                providers.Add(_twoFactorProviders[TwoFactorProviderType.Authenticator]);
            }
            if(TwoFactorProviders.ContainsKey(TwoFactorProviderType.YubiKey))
            {
                providers.Add(_twoFactorProviders[TwoFactorProviderType.YubiKey]);
            }
            if(TwoFactorProviders.ContainsKey(TwoFactorProviderType.Duo) && _platformUtilsService.SupportsDuo())
            {
                providers.Add(_twoFactorProviders[TwoFactorProviderType.Duo]);
            }
            if(TwoFactorProviders.ContainsKey(TwoFactorProviderType.U2f) && _platformUtilsService.SupportsU2f())
            {
                providers.Add(_twoFactorProviders[TwoFactorProviderType.U2f]);
            }
            if(TwoFactorProviders.ContainsKey(TwoFactorProviderType.Email))
            {
                providers.Add(_twoFactorProviders[TwoFactorProviderType.Email]);
            }
            return providers;
        }

        public TwoFactorProviderType? GetDefaultTwoFactorProvider(bool u2fSupported)
        {
            if(TwoFactorProviders == null)
            {
                return null;
            }
            if(SelectedTwoFactorProviderType != null &&
                TwoFactorProviders.ContainsKey(SelectedTwoFactorProviderType.Value))
            {
                return SelectedTwoFactorProviderType.Value;
            }
            TwoFactorProviderType? providerType = null;
            var providerPriority = -1;
            foreach(var providerKvp in TwoFactorProviders)
            {
                if(_twoFactorProviders.ContainsKey(providerKvp.Key))
                {
                    var provider = _twoFactorProviders[providerKvp.Key];
                    if(provider.Priority > providerPriority)
                    {
                        if(providerKvp.Key == TwoFactorProviderType.U2f && !u2fSupported)
                        {
                            continue;
                        }
                        providerType = providerKvp.Key;
                        providerPriority = provider.Priority;
                    }
                }
            }
            return providerType;
        }

        // Helpers

        private async Task<SymmetricCryptoKey> MakePreloginKeyAsync(string masterPassword, string email)
        {
            email = email.Trim().ToLower();
            _kdf = null;
            _kdfIterations = null;
            try
            {
                var preloginResponse = await _apiService.PostPreloginAsync(new PreloginRequest { Email = email });
                if(preloginResponse != null)
                {
                    _kdf = preloginResponse.Kdf;
                    _kdfIterations = preloginResponse.KdfIterations;
                }
            }
            catch(ApiException e)
            {
                if(e.Error == null || e.Error.StatusCode != System.Net.HttpStatusCode.NotFound)
                {
                    throw e;
                }
            }
            return await _cryptoService.MakeKeyAsync(masterPassword, email, _kdf, _kdfIterations);
        }

        private async Task<AuthResult> LogInHelperAsync(string email, string hashedPassword, SymmetricCryptoKey key,
            TwoFactorProviderType? twoFactorProvider = null, string twoFactorToken = null, bool? remember = null)
        {
            var storedTwoFactorToken = await _tokenService.GetTwoFactorTokenAsync(email);
            var appId = await _appIdService.GetAppIdAsync();
            var deviceRequest = new DeviceRequest(appId, _platformUtilsService);
            var request = new TokenRequest
            {
                Email = email,
                MasterPasswordHash = hashedPassword,
                Device = deviceRequest,
                Remember = false
            };
            if(twoFactorToken != null && twoFactorProvider != null)
            {
                request.Provider = twoFactorProvider;
                request.Token = twoFactorToken;
                request.Remember = remember.GetValueOrDefault();
            }
            else if(storedTwoFactorToken != null)
            {
                request.Provider = TwoFactorProviderType.Remember;
                request.Token = storedTwoFactorToken;
            }

            var response = await _apiService.PostIdentityTokenAsync(request);
            ClearState();
            var result = new AuthResult
            {
                TwoFactor = response.Item2 != null
            };
            if(result.TwoFactor)
            {
                // Two factor required.
                var twoFactorResponse = response.Item2;
                Email = email;
                MasterPasswordHash = hashedPassword;
                _key = _setCryptoKeys ? key : null;
                TwoFactorProviders = twoFactorResponse.TwoFactorProviders2;
                result.TwoFactorProviders = twoFactorResponse.TwoFactorProviders2;
                return result;
            }

            var tokenResponse = response.Item1;
            if(tokenResponse.TwoFactorToken != null)
            {
                await _tokenService.SetTwoFactorTokenAsync(tokenResponse.TwoFactorToken, email);
            }
            await _tokenService.SetTokensAsync(tokenResponse.AccessToken, tokenResponse.RefreshToken);
            await _userService.SetInformationAsync(_tokenService.GetUserId(), _tokenService.GetEmail(),
                _kdf.Value, _kdfIterations.Value);
            if(_setCryptoKeys)
            {
                await _cryptoService.SetKeyAsync(key);
                await _cryptoService.SetKeyHashAsync(hashedPassword);
                await _cryptoService.SetEncKeyAsync(tokenResponse.Key);

                // User doesn't have a key pair yet (old account), let's generate one for them.
                if(tokenResponse.PrivateKey == null)
                {
                    try
                    {
                        var keyPair = await _cryptoService.MakeKeyPairAsync();
                        await _apiService.PostAccountKeysAsync(new KeysRequest
                        {
                            PublicKey = keyPair.Item1,
                            EncryptedPrivateKey = keyPair.Item2.EncryptedString
                        });
                        tokenResponse.PrivateKey = keyPair.Item2.EncryptedString;
                    }
                    catch { }
                }

                await _cryptoService.SetEncPrivateKeyAsync(tokenResponse.PrivateKey);
            }

            _messagingService.Send("loggedIn");
            return result;
        }

        private void ClearState()
        {
            Email = null;
            MasterPasswordHash = null;
            TwoFactorProviders = null;
            SelectedTwoFactorProviderType = null;
        }
    }
}
