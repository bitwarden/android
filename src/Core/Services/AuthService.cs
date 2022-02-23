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
        private readonly ICryptoFunctionService _cryptoFunctionService;
        private readonly IApiService _apiService;
        private readonly IStateService _stateService;
        private readonly ITokenService _tokenService;
        private readonly IAppIdService _appIdService;
        private readonly II18nService _i18nService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IMessagingService _messagingService;
        private readonly IKeyConnectorService _keyConnectorService;
        private readonly bool _setCryptoKeys;

        private SymmetricCryptoKey _key;

        public AuthService(
            ICryptoService cryptoService,
            ICryptoFunctionService cryptoFunctionService,
            IApiService apiService,
            IStateService stateService,
            ITokenService tokenService,
            IAppIdService appIdService,
            II18nService i18nService,
            IPlatformUtilsService platformUtilsService,
            IMessagingService messagingService,
            IVaultTimeoutService vaultTimeoutService,
            IKeyConnectorService keyConnectorService,
            bool setCryptoKeys = true)
        {
            _cryptoService = cryptoService;
            _cryptoFunctionService = cryptoFunctionService;
            _apiService = apiService;
            _stateService = stateService;
            _tokenService = tokenService;
            _appIdService = appIdService;
            _i18nService = i18nService;
            _platformUtilsService = platformUtilsService;
            _messagingService = messagingService;
            _keyConnectorService = keyConnectorService;
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
            TwoFactorProviders.Add(TwoFactorProviderType.Fido2WebAuthn, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.Fido2WebAuthn,
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
        public string CaptchaToken { get; set; }
        public string MasterPasswordHash { get; set; }
        public string LocalMasterPasswordHash { get; set; }
        public string Code { get; set; }
        public string CodeVerifier { get; set; }
        public string SsoRedirectUrl { get; set; }
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
            TwoFactorProviders[TwoFactorProviderType.Fido2WebAuthn].Name = _i18nService.T("Fido2Title");
            TwoFactorProviders[TwoFactorProviderType.Fido2WebAuthn].Description = _i18nService.T("Fido2Desc");
            TwoFactorProviders[TwoFactorProviderType.YubiKey].Name = _i18nService.T("YubiKeyTitle");
            TwoFactorProviders[TwoFactorProviderType.YubiKey].Description = _i18nService.T("YubiKeyDesc");
        }

        public async Task<AuthResult> LogInAsync(string email, string masterPassword, string captchaToken)
        {
            SelectedTwoFactorProviderType = null;
            var key = await MakePreloginKeyAsync(masterPassword, email);
            var hashedPassword = await _cryptoService.HashPasswordAsync(masterPassword, key);
            var localHashedPassword = await _cryptoService.HashPasswordAsync(masterPassword, key, HashPurpose.LocalAuthorization);
            return await LogInHelperAsync(email, hashedPassword, localHashedPassword, null, null, null, key, null, null,
                null, captchaToken);
        }

        public async Task<AuthResult> LogInSsoAsync(string code, string codeVerifier, string redirectUrl, string orgId)
        {
            SelectedTwoFactorProviderType = null;
            return await LogInHelperAsync(null, null, null, code, codeVerifier, redirectUrl, null, orgId: orgId);
        }

        public Task<AuthResult> LogInTwoFactorAsync(TwoFactorProviderType twoFactorProvider, string twoFactorToken,
            bool? remember = null)
        {
            return LogInHelperAsync(Email, MasterPasswordHash, LocalMasterPasswordHash, Code, CodeVerifier, SsoRedirectUrl, _key,
                twoFactorProvider, twoFactorToken, remember, CaptchaToken);
        }

        public async Task<AuthResult> LogInCompleteAsync(string email, string masterPassword,
            TwoFactorProviderType twoFactorProvider, string twoFactorToken, bool? remember = null)
        {
            SelectedTwoFactorProviderType = null;
            var key = await MakePreloginKeyAsync(masterPassword, email);
            var hashedPassword = await _cryptoService.HashPasswordAsync(masterPassword, key);
            var localHashedPassword = await _cryptoService.HashPasswordAsync(masterPassword, key, HashPurpose.LocalAuthorization);
            return await LogInHelperAsync(email, hashedPassword, localHashedPassword, null, null, null, key, twoFactorProvider,
                twoFactorToken, remember);
        }

        public async Task<AuthResult> LogInSsoCompleteAsync(string code, string codeVerifier, string redirectUrl,
            TwoFactorProviderType twoFactorProvider, string twoFactorToken, bool? remember = null)
        {
            SelectedTwoFactorProviderType = null;
            return await LogInHelperAsync(null, null, null, code, codeVerifier, redirectUrl, null, twoFactorProvider,
                twoFactorToken, remember);
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
            if (TwoFactorProvidersData.ContainsKey(TwoFactorProviderType.Fido2WebAuthn) &&
                _platformUtilsService.SupportsFido2())
            {
                providers.Add(TwoFactorProviders[TwoFactorProviderType.Fido2WebAuthn]);
            }
            if (TwoFactorProvidersData.ContainsKey(TwoFactorProviderType.Email))
            {
                providers.Add(TwoFactorProviders[TwoFactorProviderType.Email]);
            }
            return providers;
        }

        public TwoFactorProviderType? GetDefaultTwoFactorProvider(bool fido2Supported)
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
                        if (providerKvp.Key == TwoFactorProviderType.Fido2WebAuthn && !fido2Supported)
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

        public bool AuthingWithSso()
        {
            return Code != null && CodeVerifier != null && SsoRedirectUrl != null;
        }

        public bool AuthingWithPassword()
        {
            return Email != null && MasterPasswordHash != null;
        }

        // Helpers

        private async Task<SymmetricCryptoKey> MakePreloginKeyAsync(string masterPassword, string email)
        {
            email = email.Trim().ToLower();
            KdfType? kdf = null;
            int? kdfIterations = null;
            try
            {
                var preloginResponse = await _apiService.PostPreloginAsync(new PreloginRequest { Email = email });
                if (preloginResponse != null)
                {
                    kdf = preloginResponse.Kdf;
                    kdfIterations = preloginResponse.KdfIterations;
                }
            }
            catch (ApiException e)
            {
                if (e.Error == null || e.Error.StatusCode != System.Net.HttpStatusCode.NotFound)
                {
                    throw;
                }
            }
            return await _cryptoService.MakeKeyAsync(masterPassword, email, kdf, kdfIterations);
        }

        private async Task<AuthResult> LogInHelperAsync(string email, string hashedPassword, string localHashedPassword,
            string code, string codeVerifier, string redirectUrl, SymmetricCryptoKey key,
            TwoFactorProviderType? twoFactorProvider = null, string twoFactorToken = null, bool? remember = null,
            string captchaToken = null, string orgId = null)
        {
            var storedTwoFactorToken = await _tokenService.GetTwoFactorTokenAsync(email);
            var appId = await _appIdService.GetAppIdAsync();
            var deviceRequest = new DeviceRequest(appId, _platformUtilsService);

            string[] emailPassword;
            string[] codeCodeVerifier;
            if (email != null && hashedPassword != null)
            {
                emailPassword = new[] { email, hashedPassword };
            }
            else
            {
                emailPassword = null;
            }
            if (code != null && codeVerifier != null && redirectUrl != null)
            {
                codeCodeVerifier = new[] { code, codeVerifier, redirectUrl };
            }
            else
            {
                codeCodeVerifier = null;
            }

            TokenRequest request;
            if (twoFactorToken != null && twoFactorProvider != null)
            {
                request = new TokenRequest(emailPassword, codeCodeVerifier, twoFactorProvider, twoFactorToken, remember,
                    captchaToken, deviceRequest);
            }
            else if (storedTwoFactorToken != null)
            {
                request = new TokenRequest(emailPassword, codeCodeVerifier, TwoFactorProviderType.Remember,
                    storedTwoFactorToken, false, captchaToken, deviceRequest);
            }
            else
            {
                request = new TokenRequest(emailPassword, codeCodeVerifier, null, null, false, captchaToken, deviceRequest);
            }

            var response = await _apiService.PostIdentityTokenAsync(request);
            ClearState();
            var result = new AuthResult { TwoFactor = response.TwoFactorNeeded, CaptchaSiteKey = response.CaptchaResponse?.SiteKey };

            if (result.CaptchaNeeded)
            {
                return result;
            }

            if (result.TwoFactor)
            {
                // Two factor required.
                Email = email;
                MasterPasswordHash = hashedPassword;
                LocalMasterPasswordHash = localHashedPassword;
                Code = code;
                CodeVerifier = codeVerifier;
                SsoRedirectUrl = redirectUrl;
                _key = _setCryptoKeys ? key : null;
                TwoFactorProvidersData = response.TwoFactorResponse.TwoFactorProviders2;
                result.TwoFactorProviders = response.TwoFactorResponse.TwoFactorProviders2;
                CaptchaToken = response.TwoFactorResponse.CaptchaToken;
                return result;
            }

            var tokenResponse = response.TokenResponse;
            result.ResetMasterPassword = tokenResponse.ResetMasterPassword;
            result.ForcePasswordReset = tokenResponse.ForcePasswordReset;
            if (tokenResponse.TwoFactorToken != null)
            {
                await _tokenService.SetTwoFactorTokenAsync(tokenResponse.TwoFactorToken, email);
            }
            await _tokenService.SetAccessTokenAsync(tokenResponse.AccessToken, true);
            await _stateService.AddAccountAsync(
                new Account(
                    new Account.AccountProfile()
                    {
                        UserId = _tokenService.GetUserId(),
                        Email = _tokenService.GetEmail(),
                        Name = _tokenService.GetName(),
                        KdfType = tokenResponse.Kdf,
                        KdfIterations = tokenResponse.KdfIterations,
                        HasPremiumPersonally = _tokenService.GetPremium(),
                    },
                    new Account.AccountTokens()
                    {
                        AccessToken = tokenResponse.AccessToken,
                        RefreshToken = tokenResponse.RefreshToken,
                    }
                )
            );
            _messagingService.Send("accountAdded");
            if (_setCryptoKeys)
            {
                if (key != null)
                {
                    await _cryptoService.SetKeyAsync(key);
                }

                if (localHashedPassword != null)
                {
                    await _cryptoService.SetKeyHashAsync(localHashedPassword);
                }

                if (code == null || tokenResponse.Key != null)
                {
                    if (tokenResponse.KeyConnectorUrl != null)
                    {
                        await _keyConnectorService.GetAndSetKey(tokenResponse.KeyConnectorUrl);
                    }

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
                else if (tokenResponse.KeyConnectorUrl != null)
                {
                    // SSO Key Connector Onboarding
                    var password = await _cryptoFunctionService.RandomBytesAsync(64);
                    var k = await _cryptoService.MakeKeyAsync(Convert.ToBase64String(password), _tokenService.GetEmail(), tokenResponse.Kdf, tokenResponse.KdfIterations);
                    var keyConnectorRequest = new KeyConnectorUserKeyRequest(k.EncKeyB64);
                    await _cryptoService.SetKeyAsync(k);

                    var encKey = await _cryptoService.MakeEncKeyAsync(k);
                    await _cryptoService.SetEncKeyAsync(encKey.Item2.EncryptedString);
                    var keyPair = await _cryptoService.MakeKeyPairAsync();

                    try
                    {
                        await _apiService.PostUserKeyToKeyConnector(tokenResponse.KeyConnectorUrl, keyConnectorRequest);
                    }
                    catch (Exception e)
                    {
                        throw new Exception("Unable to reach Key Connector", e);
                    }

                    var keys = new KeysRequest
                    {
                        PublicKey = keyPair.Item1,
                        EncryptedPrivateKey = keyPair.Item2.EncryptedString
                    };
                    var setPasswordRequest = new SetKeyConnectorKeyRequest(
                        encKey.Item2.EncryptedString, keys, tokenResponse.Kdf, tokenResponse.KdfIterations, orgId
                    );
                    await _apiService.PostSetKeyConnectorKey(setPasswordRequest);
                }

            }

            _stateService.BiometricLocked = false;
            _messagingService.Send("loggedIn");
            return result;
        }

        private void ClearState()
        {
            _key = null;
            Email = null;
            CaptchaToken = null;
            MasterPasswordHash = null;
            Code = null;
            CodeVerifier = null;
            SsoRedirectUrl = null;
            TwoFactorProvidersData = null;
            SelectedTwoFactorProviderType = null;
        }
    }
}
