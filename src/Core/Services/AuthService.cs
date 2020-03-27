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
        private readonly ILockService _lockService;
        private readonly bool _setCryptoKeys;

        private SymmetricCryptoKey _key;
        private KdfType? _kdf;
        private int? _kdfIterations;

        public AuthService(
            ICryptoService cryptoService,
            IApiService apiService,
            IUserService userService,
            ITokenService tokenService,
            IAppIdService appIdService,
            II18nService i18nService,
            IPlatformUtilsService platformUtilsService,
            IMessagingService messagingService,
            ILockService lockService,
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
            _lockService = lockService;
            _setCryptoKeys = setCryptoKeys;

            TwoFactorProviders = new Dictionary<TwoFactorProviderType, TwoFactorProvider>();
            TwoFactorProviders.Add(TwoFactorProviderType.Authenticator, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.Authenticator,
                Priority = 1,
                Sort = 1
            });
            TwoFactorProviders.Add(TwoFactorProviderType.YubiKey, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.YubiKey,
                Priority = 3,
                Sort = 2,
                Premium = true
            });
            TwoFactorProviders.Add(TwoFactorProviderType.Duo, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.Duo,
                Name = "Duo",
                Priority = 2,
                Sort = 3,
                Premium = true
            });
            TwoFactorProviders.Add(TwoFactorProviderType.OrganizationDuo, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.OrganizationDuo,
                Name = "Duo (Organization)",
                Priority = 10,
                Sort = 4
            });
            TwoFactorProviders.Add(TwoFactorProviderType.U2f, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.U2f,
                Priority = 4,
                Sort = 5,
                Premium = true
            });
            TwoFactorProviders.Add(TwoFactorProviderType.Email, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.Email,
                Priority = 0,
                Sort = 6,
            });
        }

        public string Email { get; set; }
        public string MasterPasswordHash { get; set; }
        public Dictionary<TwoFactorProviderType, TwoFactorProvider> TwoFactorProviders { get; set; }
        public Dictionary<TwoFactorProviderType, Dictionary<string, object>> TwoFactorProvidersData { get; set; }
        public TwoFactorProviderType? SelectedTwoFactorProviderType { get; set; }

        public void Init()
        {
            TwoFactorProviders[TwoFactorProviderType.Email].Name = _i18nService.T("Email");
            TwoFactorProviders[TwoFactorProviderType.Email].Description = _i18nService.T("EmailDesc");
            TwoFactorProviders[TwoFactorProviderType.Authenticator].Name = _i18nService.T("AuthenticatorAppTitle");
            TwoFactorProviders[TwoFactorProviderType.Authenticator].Description =
                _i18nService.T("AuthenticatorAppDesc");
            TwoFactorProviders[TwoFactorProviderType.Duo].Description = _i18nService.T("DuoDesc");
            TwoFactorProviders[TwoFactorProviderType.OrganizationDuo].Name =
                string.Format("Duo ({0})", _i18nService.T("Organization"));
            TwoFactorProviders[TwoFactorProviderType.OrganizationDuo].Description =
                _i18nService.T("DuoOrganizationDesc");
            TwoFactorProviders[TwoFactorProviderType.U2f].Name = _i18nService.T("U2fTitle");
            TwoFactorProviders[TwoFactorProviderType.U2f].Description = _i18nService.T("U2fDesc");
            TwoFactorProviders[TwoFactorProviderType.YubiKey].Name = _i18nService.T("YubiKeyTitle");
            TwoFactorProviders[TwoFactorProviderType.YubiKey].Description = _i18nService.T("YubiKeyDesc");
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
            if (TwoFactorProvidersData == null)
            {
                return providers;
            }
            if (TwoFactorProvidersData.ContainsKey(TwoFactorProviderType.OrganizationDuo) &&
                _platformUtilsService.SupportsDuo())
            {
                providers.Add(TwoFactorProviders[TwoFactorProviderType.OrganizationDuo]);
            }
            if (TwoFactorProvidersData.ContainsKey(TwoFactorProviderType.Authenticator))
            {
                providers.Add(TwoFactorProviders[TwoFactorProviderType.Authenticator]);
            }
            if (TwoFactorProvidersData.ContainsKey(TwoFactorProviderType.YubiKey))
            {
                providers.Add(TwoFactorProviders[TwoFactorProviderType.YubiKey]);
            }
            if (TwoFactorProvidersData.ContainsKey(TwoFactorProviderType.Duo) && _platformUtilsService.SupportsDuo())
            {
                providers.Add(TwoFactorProviders[TwoFactorProviderType.Duo]);
            }
            if (TwoFactorProvidersData.ContainsKey(TwoFactorProviderType.U2f) && _platformUtilsService.SupportsU2f())
            {
                providers.Add(TwoFactorProviders[TwoFactorProviderType.U2f]);
            }
            if (TwoFactorProvidersData.ContainsKey(TwoFactorProviderType.Email))
            {
                providers.Add(TwoFactorProviders[TwoFactorProviderType.Email]);
            }
            return providers;
        }

        public TwoFactorProviderType? GetDefaultTwoFactorProvider(bool u2fSupported)
        {
            if (TwoFactorProvidersData == null)
            {
                return null;
            }
            if (SelectedTwoFactorProviderType != null &&
                TwoFactorProvidersData.ContainsKey(SelectedTwoFactorProviderType.Value))
            {
                return SelectedTwoFactorProviderType.Value;
            }
            TwoFactorProviderType? providerType = null;
            var providerPriority = -1;
            foreach (var providerKvp in TwoFactorProvidersData)
            {
                if (TwoFactorProviders.ContainsKey(providerKvp.Key))
                {
                    var provider = TwoFactorProviders[providerKvp.Key];
                    if (provider.Priority > providerPriority)
                    {
                        if (providerKvp.Key == TwoFactorProviderType.U2f && !u2fSupported)
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
                if (preloginResponse != null)
                {
                    _kdf = preloginResponse.Kdf;
                    _kdfIterations = preloginResponse.KdfIterations;
                }
            }
            catch (ApiException e)
            {
                if (e.Error == null || e.Error.StatusCode != System.Net.HttpStatusCode.NotFound)
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
            if (twoFactorToken != null && twoFactorProvider != null)
            {
                request.Provider = twoFactorProvider;
                request.Token = twoFactorToken;
                request.Remember = remember.GetValueOrDefault();
            }
            else if (storedTwoFactorToken != null)
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
            if (result.TwoFactor)
            {
                // Two factor required.
                var twoFactorResponse = response.Item2;
                Email = email;
                MasterPasswordHash = hashedPassword;
                _key = _setCryptoKeys ? key : null;
                TwoFactorProvidersData = twoFactorResponse.TwoFactorProviders2;
                result.TwoFactorProviders = twoFactorResponse.TwoFactorProviders2;
                return result;
            }

            var tokenResponse = response.Item1;
            if (tokenResponse.TwoFactorToken != null)
            {
                await _tokenService.SetTwoFactorTokenAsync(tokenResponse.TwoFactorToken, email);
            }
            await _tokenService.SetTokensAsync(tokenResponse.AccessToken, tokenResponse.RefreshToken);
            await _userService.SetInformationAsync(_tokenService.GetUserId(), _tokenService.GetEmail(),
                _kdf.Value, _kdfIterations.Value);
            if (_setCryptoKeys)
            {
                await _cryptoService.SetKeyAsync(key);
                await _cryptoService.SetKeyHashAsync(hashedPassword);
                await _cryptoService.SetEncKeyAsync(tokenResponse.Key);

                // User doesn't have a key pair yet (old account), let's generate one for them.
                if (tokenResponse.PrivateKey == null)
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

            _lockService.FingerprintLocked = false;
            _messagingService.Send("loggedIn");
            return result;
        }

        private void ClearState()
        {
            Email = null;
            MasterPasswordHash = null;
            TwoFactorProvidersData = null;
            SelectedTwoFactorProviderType = null;
        }
    }
}
