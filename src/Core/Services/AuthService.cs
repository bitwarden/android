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
        private readonly IDeviceTrustCryptoService _deviceTrustCryptoService;
        private readonly IPasswordResetEnrollmentService _passwordResetEnrollmentService;
        private readonly bool _setCryptoKeys;

        private readonly LazyResolve<IWatchDeviceService> _watchDeviceService = new LazyResolve<IWatchDeviceService>();
        private MasterKey _masterKey;
        private UserKey _userKey;

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
            IKeyConnectorService keyConnectorService,
            IPasswordGenerationService passwordGenerationService,
            IPolicyService policyService,
            IDeviceTrustCryptoService deviceTrustCryptoService,
            IPasswordResetEnrollmentService passwordResetEnrollmentService,
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
            _deviceTrustCryptoService = deviceTrustCryptoService;
            _passwordResetEnrollmentService = passwordResetEnrollmentService;
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
        public string SsoEmail2FaSessionToken { get; set; }
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
            var hashedPassword = await _cryptoService.HashMasterKeyAsync(masterPassword, key);
            var localHashedPassword = await _cryptoService.HashMasterKeyAsync(masterPassword, key, HashPurpose.LocalAuthorization);
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

        public async Task<AuthResult> LogInPasswordlessAsync(bool authingWithSso, string email, string accessCode, string authRequestId, byte[] decryptionKey, string encryptedAuthRequestKey, string masterKeyHash)
        {
            var decryptedKey = await _cryptoService.RsaDecryptAsync(encryptedAuthRequestKey, decryptionKey);

            // If the user is already authenticated, we can just set the key
            // Note: We can't check for the existance of an access token here because the active user id may not be null
            if (authingWithSso)
            {
                if (string.IsNullOrEmpty(masterKeyHash))
                {
                    await _cryptoService.SetUserKeyAsync(new UserKey(decryptedKey));
                }
                else
                {
                    var masterKey = new MasterKey(decryptedKey);
                    var userKey = await _cryptoService.DecryptUserKeyWithMasterKeyAsync(masterKey);
                    await _cryptoService.SetMasterKeyAsync(masterKey);
                    await _cryptoService.SetUserKeyAsync(userKey);
                }
                await _deviceTrustCryptoService.TrustDeviceIfNeededAsync();
                return null;
            }

            // The approval device may not have a master key hash if it authenticated with a passwordless method
            if (string.IsNullOrEmpty(masterKeyHash) && decryptionKey != null)
            {
                return await LogInHelperAsync(email, accessCode, null, null, null, null, null, null, null, null, null, authRequestId: authRequestId, userKey2FA: new UserKey(decryptedKey));
            }

            var decKeyHash = await _cryptoService.RsaDecryptAsync(masterKeyHash, decryptionKey);
            return await LogInHelperAsync(email, accessCode, Encoding.UTF8.GetString(decKeyHash), null, null, null, new MasterKey(decryptedKey), null, null,
                null, null, authRequestId: authRequestId);
        }

        public async Task<AuthResult> LogInSsoAsync(string code, string codeVerifier, string redirectUrl, string orgId)
        {
            SelectedTwoFactorProviderType = null;
            var result = await LogInHelperAsync(null, null, null, code, codeVerifier, redirectUrl, null, orgId: orgId);
            if (result.ForcePasswordReset)
            {
                await _stateService.SetForcePasswordResetReasonAsync(ForcePasswordResetReason.AdminForcePasswordReset);
            }

            return result;
        }

        public async Task<AuthResult> LogInTwoFactorAsync(TwoFactorProviderType twoFactorProvider, string twoFactorToken,
            string captchaToken, bool? remember = null)
        {
            if (captchaToken != null)
            {
                CaptchaToken = captchaToken;
            }
            var result = await LogInHelperAsync(Email, MasterPasswordHash, LocalMasterPasswordHash, Code, CodeVerifier, SsoRedirectUrl, _masterKey,
                twoFactorProvider, twoFactorToken, remember, CaptchaToken, authRequestId: AuthRequestId, userKey2FA: _userKey);

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
            var hashedPassword = await _cryptoService.HashMasterKeyAsync(masterPassword, key);
            var localHashedPassword = await _cryptoService.HashMasterKeyAsync(masterPassword, key, HashPurpose.LocalAuthorization);
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

        private async Task<MasterKey> MakePreloginKeyAsync(string masterPassword, string email)
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
            return await _cryptoService.MakeMasterKeyAsync(masterPassword, email, kdfConfig);
        }

        private async Task<AuthResult> LogInHelperAsync(string email, string hashedPassword, string localHashedPassword,
            string code, string codeVerifier, string redirectUrl, MasterKey masterKey,
            TwoFactorProviderType? twoFactorProvider = null, string twoFactorToken = null, bool? remember = null,
            string captchaToken = null, string orgId = null, string authRequestId = null, UserKey userKey2FA = null)
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
                Email = response.TwoFactorResponse.Email ?? email;
                MasterPasswordHash = hashedPassword;
                LocalMasterPasswordHash = localHashedPassword;
                AuthRequestId = authRequestId;
                Code = code;
                CodeVerifier = codeVerifier;
                SsoRedirectUrl = redirectUrl;
                SsoEmail2FaSessionToken = response.TwoFactorResponse.SsoEmail2faSessionToken;
                _masterKey = _setCryptoKeys ? masterKey : null;
                _userKey = userKey2FA;
                TwoFactorProvidersData = response.TwoFactorResponse.TwoFactorProviders2;
                result.TwoFactorProviders = response.TwoFactorResponse.TwoFactorProviders2;
                CaptchaToken = response.TwoFactorResponse.CaptchaToken;
                _masterPasswordPolicy = response.TwoFactorResponse.MasterPasswordPolicy;
                await _tokenService.ClearTwoFactorTokenAsync(Email);
                return result;
            }

            var tokenResponse = response.TokenResponse;
            if (localHashedPassword != null && tokenResponse.Key == null)
            {
                // Only check for legacy if there is no key on token
                if (await _cryptoService.IsLegacyUserAsync(masterKey))
                {
                    // Legacy users must migrate on web vault;
                    result.RequiresEncryptionKeyMigration = true;
                    return result;
                }
            }

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
                        UserDecryptionOptions = tokenResponse.UserDecryptionOptions,
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
                if (localHashedPassword != null)
                {
                    await _cryptoService.SetMasterKeyHashAsync(localHashedPassword);
                    await _cryptoService.SetMasterKeyAsync(masterKey);
                }

                // Trusted Device
                var decryptOptions = await _stateService.GetAccountDecryptionOptions();
                var hasUserKey = await _cryptoService.HasUserKeyAsync();
                if (decryptOptions?.TrustedDeviceOption != null && !hasUserKey &&
                    decryptOptions.TrustedDeviceOption.EncryptedPrivateKey != null &&
                    decryptOptions.TrustedDeviceOption.EncryptedUserKey != null)
                {
                    var key = await _deviceTrustCryptoService.DecryptUserKeyWithDeviceKeyAsync(decryptOptions.TrustedDeviceOption.EncryptedPrivateKey,
                            decryptOptions.TrustedDeviceOption.EncryptedUserKey);
                    if (key != null)
                    {
                        await _cryptoService.SetUserKeyAsync(key);
                    }
                }

                if (code == null || tokenResponse.Key != null)
                {
                    await _cryptoService.SetMasterKeyEncryptedUserKeyAsync(tokenResponse.Key);

                    // Key Connector
                    if (!string.IsNullOrEmpty(tokenResponse.KeyConnectorUrl) || !string.IsNullOrEmpty(decryptOptions?.KeyConnectorOption?.KeyConnectorUrl))
                    {
                        var url = tokenResponse.KeyConnectorUrl ?? decryptOptions.KeyConnectorOption.KeyConnectorUrl;
                        await _keyConnectorService.SetMasterKeyFromUrlAsync(url);
                    }

                    // Login with Device
                    if (masterKey != null && !string.IsNullOrEmpty(authRequestId))
                    {
                        await _cryptoService.SetMasterKeyAsync(masterKey);
                    }
                    else if (userKey2FA != null)
                    {
                        await _cryptoService.SetUserKeyAsync(userKey2FA);
                    }

                    // Decrypt UserKey with MasterKey
                    masterKey ??= await _stateService.GetMasterKeyAsync();
                    if (masterKey != null)
                    {
                        var userKey = await _cryptoService.DecryptUserKeyWithMasterKeyAsync(masterKey);
                        await _cryptoService.SetUserKeyAsync(userKey);
                    }

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

                    await _cryptoService.SetUserPrivateKeyAsync(tokenResponse.PrivateKey);
                }
                else if (tokenResponse.KeyConnectorUrl != null)
                {
                    // New User has tokenResponse.Key == null
                    if (tokenResponse.Key == null)
                    {
                        await _keyConnectorService.ConvertNewUserToKeyConnectorAsync(orgId, tokenResponse);
                    }
                    else
                    {
                        await _keyConnectorService.SetMasterKeyFromUrlAsync(tokenResponse.KeyConnectorUrl);
                    }
                }
            }

            _authedUserId = _tokenService.GetUserId();
            await _stateService.SetBiometricLockedAsync(false);
            _messagingService.Send("loggedIn");
            return result;
        }

        private void ClearState()
        {
            _masterKey = null;
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
            try
            {
                var response = await _apiService.GetAuthRequestAsync(id);
                return await PopulateFingerprintPhraseAsync(response, await _stateService.GetEmailAsync());
            }
            catch (ApiException ex) when (ex.Error?.StatusCode == System.Net.HttpStatusCode.NotFound)
            {
                // Thrown when request expires and purge job erases it from the db
                return null;
            }
        }

        /// <inheritdoc />
        public async Task<PasswordlessLoginResponse> GetPasswordlessLoginResquestAsync(string id, string accessCode)
        {
            return await _apiService.GetAuthResponseAsync(id, accessCode);
        }

        public async Task<PasswordlessLoginResponse> PasswordlessLoginAsync(string id, string pubKey, bool requestApproved)
        {
            var publicKey = CoreHelpers.Base64UrlDecode(pubKey);
            var masterKey = await _cryptoService.GetMasterKeyAsync();
            byte[] keyToEncrypt = null;
            EncString encryptedMasterPassword = null;

            if (masterKey == null)
            {
                var userKey = await _cryptoService.GetUserKeyAsync();
                if (userKey == null)
                {
                    throw new UserAndMasterKeysNullException();
                }
                keyToEncrypt = userKey.Key;
            }
            else
            {
                keyToEncrypt = masterKey.Key;
                var keyHash = await _stateService.GetKeyHashAsync();
                if (!string.IsNullOrEmpty(keyHash))
                {
                    encryptedMasterPassword = await _cryptoService.RsaEncryptAsync(Encoding.UTF8.GetBytes(keyHash), publicKey);
                }
            }

            var encryptedKey = await _cryptoService.RsaEncryptAsync(keyToEncrypt, publicKey);
            var deviceId = await _appIdService.GetAppIdAsync();
            var response = await _apiService.PutAuthRequestAsync(id, encryptedKey.EncryptedString, encryptedMasterPassword?.EncryptedString, deviceId, requestApproved);
            return await PopulateFingerprintPhraseAsync(response, await _stateService.GetEmailAsync());
        }

        public async Task<PasswordlessLoginResponse> PasswordlessCreateLoginRequestAsync(string email, AuthRequestType authRequestType)
        {
            var deviceId = await _appIdService.GetAppIdAsync();
            var keyPair = await _cryptoFunctionService.RsaGenerateKeyPairAsync(2048);
            var generatedFingerprintPhrase = await _cryptoService.GetFingerprintAsync(email, keyPair.Item1);
            var fingerprintPhrase = string.Join("-", generatedFingerprintPhrase);
            var publicB64 = Convert.ToBase64String(keyPair.Item1);
            var accessCode = await _passwordGenerationService.GeneratePasswordAsync(PasswordGenerationOptions.CreateDefault.WithLength(25));
            var passwordlessCreateLoginRequest = new PasswordlessCreateLoginRequest(email, publicB64, deviceId, accessCode, authRequestType, fingerprintPhrase);
            var response = await _apiService.PostCreateRequestAsync(passwordlessCreateLoginRequest, authRequestType);

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

        public async Task CreateNewSsoUserAsync(string organizationSsoId)
        {
            var orgAutoEnrollStatusResponse = await _apiService.GetOrganizationAutoEnrollStatusAsync(organizationSsoId);
            var randomBytes = _cryptoFunctionService.RandomBytes(64);
            var userKey = new UserKey(randomBytes);
            var (userPubKey, userPrivKey) = await _cryptoService.MakeKeyPairAsync(userKey);
            await _apiService.PostAccountKeysAsync(new KeysRequest
            {
                PublicKey = userPubKey,
                EncryptedPrivateKey = userPrivKey.EncryptedString
            });

            await _stateService.SetUserKeyAsync(userKey);
            await _stateService.SetPrivateKeyEncryptedAsync(userPrivKey.EncryptedString);
            await _passwordResetEnrollmentService.EnrollAsync(orgAutoEnrollStatusResponse.Id);
        }
    }
}
