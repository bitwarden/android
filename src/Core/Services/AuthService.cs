using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Models.Response;
using Bit.Core.Utilities;

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
        private readonly IPasswordGenerationService _passwordGenerationService;
        private readonly IPolicyService _policyService;
        private readonly bool _setCryptoKeys;

        private readonly LazyResolve<IWatchDeviceService> _watchDeviceService = new LazyResolve<IWatchDeviceService>();
        private SymmetricCryptoKey _key;

        private string _authedUserId;
        private MasterPasswordPolicyOptions _masterPasswordPolicy;
        private ForcePasswordResetReason? _2faForcePasswordResetReason;

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
            IPasswordGenerationService passwordGenerationService,
            IPolicyService policyService,
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
            _passwordGenerationService = passwordGenerationService;
            _policyService = policyService;
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
        public string AuthRequestId { get; set; }
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
            _2faForcePasswordResetReason = null;
            var key = await MakePreloginKeyAsync(masterPassword, email);
            var hashedPassword = await _cryptoService.HashPasswordAsync(masterPassword, key);
            var localHashedPassword = await _cryptoService.HashPasswordAsync(masterPassword, key, HashPurpose.LocalAuthorization);
            var result = await LogInHelperAsync(email, hashedPassword, localHashedPassword, null, null, null, key, null, null, null, captchaToken);

            if (await RequirePasswordChangeAsync(email, masterPassword))
            {
                if (!string.IsNullOrEmpty(_authedUserId))
                {
                    // Authentication was successful, save the WeakMasterPasswordOnLogin flag for the user
                    result.ForcePasswordReset = true;
                    await _stateService.SetForcePasswordResetReasonAsync(
                        ForcePasswordResetReason.WeakMasterPasswordOnLogin, _authedUserId);
                }
                else
                {
                    // Authentication not fully successful (likely 2FA), store flag for LogInTwoFactorAsync()
                    _2faForcePasswordResetReason = ForcePasswordResetReason.WeakMasterPasswordOnLogin;
                }
            }

            return result;
        }

        /// <summary>
        /// Evaluates the supplied master password against the master password policy provided by the Identity response.
        /// </summary>
        /// <param name="email"></param>
        /// <param name="masterPassword"></param>
        /// <returns>True if the master password does NOT meet any policy requirements, false otherwise (or if no policy present)</returns>
        private async Task<bool> RequirePasswordChangeAsync(string email, string masterPassword)
        {
            // No policy with EnforceOnLogin enabled, we're done. 
            if (!(_masterPasswordPolicy is { EnforceOnLogin: true }))
            {
                return false;
            }

            var strength = _passwordGenerationService.PasswordStrength(
                masterPassword,
                _passwordGenerationService.GetPasswordStrengthUserInput(email)
            )?.Score;

            if (!strength.HasValue)
            {
                return false;
            }

            return !await _policyService.EvaluateMasterPassword(strength.Value, masterPassword, _masterPasswordPolicy);
        }

        public async Task<AuthResult> LogInPasswordlessAsync(string email, string accessCode, string authRequestId, byte[] decryptionKey, string userKeyCiphered, string localHashedPasswordCiphered)
        {
            var decKey = await _cryptoService.RsaDecryptAsync(userKeyCiphered, decryptionKey);
            var decPasswordHash = await _cryptoService.RsaDecryptAsync(localHashedPasswordCiphered, decryptionKey);
            return await LogInHelperAsync(email, accessCode, Encoding.UTF8.GetString(decPasswordHash), null, null, null, new SymmetricCryptoKey(decKey), null, null,
                null, null, authRequestId: authRequestId);
        }

        public async Task<AuthResult> LogInSsoAsync(string code, string codeVerifier, string redirectUrl, string orgId)
        {
            SelectedTwoFactorProviderType = null;
            return await LogInHelperAsync(null, null, null, code, codeVerifier, redirectUrl, null, orgId: orgId);
        }

        public async Task<AuthResult> LogInTwoFactorAsync(TwoFactorProviderType twoFactorProvider, string twoFactorToken,
            string captchaToken, bool? remember = null)
        {
            if (captchaToken != null)
            {
                CaptchaToken = captchaToken;
            }
            var result = await LogInHelperAsync(Email, MasterPasswordHash, LocalMasterPasswordHash, Code, CodeVerifier, SsoRedirectUrl, _key,
                twoFactorProvider, twoFactorToken, remember, CaptchaToken, authRequestId: AuthRequestId);

            // If we successfully authenticated and we have a saved _2faForcePasswordResetReason reason from LogInAsync()
            if (!string.IsNullOrEmpty(_authedUserId) && _2faForcePasswordResetReason.HasValue)
            {
                // Save the forcePasswordReset reason with the state service to force a password reset for the user
                result.ForcePasswordReset = true;
                await _stateService.SetForcePasswordResetReasonAsync(
                    _2faForcePasswordResetReason, _authedUserId);
            }

            return result;
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
            _messagingService.Send(AccountsManagerMessageCommands.LOGGED_OUT);
            _watchDeviceService.Value.SyncDataToWatchAsync().FireAndForget();
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
            KdfConfig kdfConfig = KdfConfig.Default;
            try
            {
                var preloginResponse = await _apiService.PostPreloginAsync(new PreloginRequest { Email = email });
                if (preloginResponse != null)
                {
                    kdfConfig = preloginResponse.KdfConfig;
                }
            }
            catch (ApiException e)
            {
                if (e.Error == null || e.Error.StatusCode != System.Net.HttpStatusCode.NotFound)
                {
                    throw;
                }
            }
            return await _cryptoService.MakeKeyAsync(masterPassword, email, kdfConfig);
        }

        private async Task<AuthResult> LogInHelperAsync(string email, string hashedPassword, string localHashedPassword,
            string code, string codeVerifier, string redirectUrl, SymmetricCryptoKey key,
            TwoFactorProviderType? twoFactorProvider = null, string twoFactorToken = null, bool? remember = null,
            string captchaToken = null, string orgId = null, string authRequestId = null)
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
                    captchaToken, deviceRequest, authRequestId);
            }
            else if (storedTwoFactorToken != null)
            {
                request = new TokenRequest(emailPassword, codeCodeVerifier, TwoFactorProviderType.Remember,
                    storedTwoFactorToken, false, captchaToken, deviceRequest, authRequestId);
            }
            else if (authRequestId != null)
            {
                request = new TokenRequest(emailPassword, null, null, null, false, null, deviceRequest, authRequestId);
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
                AuthRequestId = authRequestId;
                Code = code;
                CodeVerifier = codeVerifier;
                SsoRedirectUrl = redirectUrl;
                _key = _setCryptoKeys ? key : null;
                TwoFactorProvidersData = response.TwoFactorResponse.TwoFactorProviders2;
                result.TwoFactorProviders = response.TwoFactorResponse.TwoFactorProviders2;
                CaptchaToken = response.TwoFactorResponse.CaptchaToken;
                _masterPasswordPolicy = response.TwoFactorResponse.MasterPasswordPolicy;
                await _tokenService.ClearTwoFactorTokenAsync(email);
                return result;
            }

            var tokenResponse = response.TokenResponse;
            result.ResetMasterPassword = tokenResponse.ResetMasterPassword;
            result.ForcePasswordReset = tokenResponse.ForcePasswordReset;
            _masterPasswordPolicy = tokenResponse.MasterPasswordPolicy;
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
                        KdfMemory = tokenResponse.KdfMemory,
                        KdfParallelism = tokenResponse.KdfParallelism,
                        HasPremiumPersonally = _tokenService.GetPremium(),
                        ForcePasswordResetReason = result.ForcePasswordReset
                            ? ForcePasswordResetReason.AdminForcePasswordReset
                            : (ForcePasswordResetReason?)null,
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
                    var k = await _cryptoService.MakeKeyAsync(Convert.ToBase64String(password), _tokenService.GetEmail(), tokenResponse.KdfConfig);
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
                        encKey.Item2.EncryptedString, keys, tokenResponse.KdfConfig, orgId
                    );
                    await _apiService.PostSetKeyConnectorKey(setPasswordRequest);
                }

            }

            _authedUserId = _tokenService.GetUserId();
            await _stateService.SetBiometricLockedAsync(false);
            _messagingService.Send("loggedIn");
            return result;
        }

        private void ClearState()
        {
            _key = null;
            Email = null;
            CaptchaToken = null;
            MasterPasswordHash = null;
            AuthRequestId = null;
            Code = null;
            CodeVerifier = null;
            SsoRedirectUrl = null;
            TwoFactorProvidersData = null;
            SelectedTwoFactorProviderType = null;
            _masterPasswordPolicy = null;
            _authedUserId = null;
        }

        public async Task<List<PasswordlessLoginResponse>> GetPasswordlessLoginRequestsAsync()
        {
            var response = await _apiService.GetAuthRequestAsync();
            return await PopulateFingerprintPhrasesAsync(response);
        }

        public async Task<List<PasswordlessLoginResponse>> GetActivePasswordlessLoginRequestsAsync()
        {
            var requests = await GetPasswordlessLoginRequestsAsync();
            var activeRequests = requests.Where(r => !r.IsAnswered && !r.IsExpired).OrderByDescending(r => r.CreationDate).ToList();
            return await PopulateFingerprintPhrasesAsync(activeRequests);
        }

        public async Task<PasswordlessLoginResponse> GetPasswordlessLoginRequestByIdAsync(string id)
        {
            var response = await _apiService.GetAuthRequestAsync(id);
            return await PopulateFingerprintPhraseAsync(response, await _stateService.GetEmailAsync());
        }

        public async Task<PasswordlessLoginResponse> GetPasswordlessLoginResponseAsync(string id, string accessCode)
        {
            return await _apiService.GetAuthResponseAsync(id, accessCode);
        }

        public async Task<PasswordlessLoginResponse> PasswordlessLoginAsync(string id, string pubKey, bool requestApproved)
        {
            var publicKey = CoreHelpers.Base64UrlDecode(pubKey);
            var masterKey = await _cryptoService.GetKeyAsync();
            var encryptedKey = await _cryptoService.RsaEncryptAsync(masterKey.EncKey, publicKey);
            var keyHash = await _stateService.GetKeyHashAsync();
            EncString encryptedMasterPassword = null;
            if (!string.IsNullOrEmpty(keyHash))
            {
                encryptedMasterPassword = await _cryptoService.RsaEncryptAsync(Encoding.UTF8.GetBytes(keyHash), publicKey);
            }
            var deviceId = await _appIdService.GetAppIdAsync();
            var response = await _apiService.PutAuthRequestAsync(id, encryptedKey.EncryptedString, encryptedMasterPassword?.EncryptedString, deviceId, requestApproved);
            return await PopulateFingerprintPhraseAsync(response, await _stateService.GetEmailAsync());
        }

        public async Task<PasswordlessLoginResponse> PasswordlessCreateLoginRequestAsync(string email)
        {
            var deviceId = await _appIdService.GetAppIdAsync();
            var keyPair = await _cryptoFunctionService.RsaGenerateKeyPairAsync(2048);
            var generatedFingerprintPhrase = await _cryptoService.GetFingerprintAsync(email, keyPair.Item1);
            var fingerprintPhrase = string.Join("-", generatedFingerprintPhrase);
            var publicB64 = Convert.ToBase64String(keyPair.Item1);
            var accessCode = await _passwordGenerationService.GeneratePasswordAsync(PasswordGenerationOptions.CreateDefault.WithLength(25));
            var passwordlessCreateLoginRequest = new PasswordlessCreateLoginRequest(email, publicB64, deviceId, accessCode, AuthRequestType.AuthenticateAndUnlock, fingerprintPhrase);
            var response = await _apiService.PostCreateRequestAsync(passwordlessCreateLoginRequest);

            if (response != null)
            {
                response.RequestKeyPair = keyPair;
                response.RequestAccessCode = accessCode;
                response.FingerprintPhrase = fingerprintPhrase;
            }

            return response;
        }

        private async Task<List<PasswordlessLoginResponse>> PopulateFingerprintPhrasesAsync(List<PasswordlessLoginResponse> passwordlessLoginList)
        {
            if (passwordlessLoginList == null)
            {
                return null;
            }
            var userEmail = await _stateService.GetEmailAsync();
            foreach (var passwordlessLogin in passwordlessLoginList)
            {
                await PopulateFingerprintPhraseAsync(passwordlessLogin, userEmail);
            }
            return passwordlessLoginList;
        }

        private async Task<PasswordlessLoginResponse> PopulateFingerprintPhraseAsync(PasswordlessLoginResponse passwordlessLogin, string userEmail)
        {
            passwordlessLogin.FingerprintPhrase = string.Join("-", await _cryptoService.GetFingerprintAsync(userEmail, CoreHelpers.Base64UrlDecode(passwordlessLogin.PublicKey)));
            return passwordlessLogin;
        }
    }
}
