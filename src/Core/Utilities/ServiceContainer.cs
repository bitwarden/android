using System.Collections.Concurrent;
using System.Globalization;
using System.Text;
using Bit.Core.Abstractions;
using Bit.Core.Services;

namespace Bit.Core.Utilities
{
    public static class ServiceContainer
    {
        public static ConcurrentDictionary<string, object> RegisteredServices { get; set; } = new ConcurrentDictionary<string, object>();
        public static bool Inited { get; set; }

        public static void Init(string customUserAgent = null, string clearCipherCacheKey = null,
            string[] allClearCipherCacheKeys = null)
        {
            if (Inited)
            {
                return;
            }
            Inited = true;

            var platformUtilsService = Resolve<IPlatformUtilsService>("platformUtilsService");
            var storageService = Resolve<IStorageService>("storageService");
            var stateService = Resolve<IStateService>("stateService");
            var i18nService = Resolve<II18nService>("i18nService");
            var messagingService = Resolve<IMessagingService>("messagingService");
            var cryptoFunctionService = Resolve<ICryptoFunctionService>("cryptoFunctionService");
            var cryptoService = Resolve<ICryptoService>("cryptoService");
            var clipboardService = Resolve<IClipboardService>();
            var logger = Resolve<ILogger>();

            SearchService searchService = null;

            var conditionedRunner = new ConditionedAwaiterManager();
            var tokenService = new TokenService(stateService);
            var apiService = new ApiService(tokenService, platformUtilsService, (extras) =>
            {
                messagingService.Send("logout", extras);
                return Task.CompletedTask;
            }, customUserAgent);
            var appIdService = new AppIdService(storageService);
            var organizationService = new OrganizationService(stateService, apiService);
            var settingsService = new SettingsService(stateService);
            var fileUploadService = new FileUploadService(apiService);
            var configService = new ConfigService(apiService, stateService, logger);
            var totpService = new TotpService(cryptoFunctionService);
            var environmentService = new EnvironmentService(apiService, stateService, conditionedRunner);
            var cipherService = new CipherService(cryptoService, stateService, settingsService, apiService,
                fileUploadService, storageService, i18nService, () => searchService, configService, totpService, clipboardService, clearCipherCacheKey,
                allClearCipherCacheKeys);
            var folderService = new FolderService(cryptoService, stateService, apiService, i18nService, cipherService);
            var collectionService = new CollectionService(cryptoService, stateService, i18nService);
            var sendService = new SendService(cryptoService, stateService, apiService, fileUploadService, i18nService,
                cryptoFunctionService);
            searchService = new SearchService(cipherService, sendService);
            var policyService = new PolicyService(stateService, organizationService);
            var keyConnectorService = new KeyConnectorService(stateService, cryptoService, tokenService, apiService, cryptoFunctionService,
                organizationService);
            var userVerificationService = new UserVerificationService(apiService, platformUtilsService, i18nService,
                cryptoService, stateService, keyConnectorService);
            var vaultTimeoutService = new VaultTimeoutService(cryptoService, stateService, platformUtilsService,
                folderService, cipherService, collectionService, searchService, tokenService, userVerificationService,
                (extras) =>
                {
                    messagingService.Send("locked", extras);
                    return Task.CompletedTask;
                },
                (extras) =>
                {
                    messagingService.Send("logout", extras);
                    return Task.CompletedTask;
                });
            var syncService = new SyncService(stateService, apiService, settingsService, folderService, cipherService,
                cryptoService, collectionService, organizationService, messagingService, policyService, sendService,
                keyConnectorService, logger, (extras) =>
                {
                    messagingService.Send("logout", extras);
                    return Task.CompletedTask;
                });
            var passwordGenerationService = new PasswordGenerationService(cryptoService, stateService, cryptoFunctionService, policyService);
            var deviceTrustCryptoService = new DeviceTrustCryptoService(apiService, appIdService, cryptoFunctionService, cryptoService, stateService);
            var passwordResetEnrollmentService = new PasswordResetEnrollmentService(apiService, cryptoService, organizationService, stateService);
            var authService = new AuthService(cryptoService, cryptoFunctionService, apiService, stateService,
                tokenService, appIdService, i18nService, platformUtilsService, messagingService,
                keyConnectorService, passwordGenerationService, policyService, deviceTrustCryptoService, passwordResetEnrollmentService);
            var exportService = new ExportService(folderService, cipherService, cryptoService);
            var auditService = new AuditService(cryptoFunctionService, apiService);
            var eventService = new EventService(apiService, stateService, organizationService, cipherService);
            var usernameGenerationService = new UsernameGenerationService(cryptoService, apiService, stateService);

            Register<IConditionedAwaiterManager>(conditionedRunner);
            Register<ITokenService>("tokenService", tokenService);
            Register<IApiService>("apiService", apiService);
            Register<IAppIdService>("appIdService", appIdService);
            Register<IOrganizationService>("organizationService", organizationService);
            Register<ISettingsService>("settingsService", settingsService);
            Register<IConfigService>(configService);
            Register<ICipherService>("cipherService", cipherService);
            Register<IFolderService>("folderService", folderService);
            Register<ICollectionService>("collectionService", collectionService);
            Register<ISendService>("sendService", sendService);
            Register<ISearchService>("searchService", searchService);
            Register<IPolicyService>("policyService", policyService);
            Register<ISyncService>("syncService", syncService);
            Register<IKeyConnectorService>("keyConnectorService", keyConnectorService);
            Register<IUserVerificationService>(userVerificationService);
            Register<IVaultTimeoutService>("vaultTimeoutService", vaultTimeoutService);
            Register<IPasswordGenerationService>("passwordGenerationService", passwordGenerationService);
            Register<ITotpService>("totpService", totpService);
            Register<IAuthService>("authService", authService);
            Register<IExportService>("exportService", exportService);
            Register<IAuditService>("auditService", auditService);
            Register<IEnvironmentService>("environmentService", environmentService);
            Register<IEventService>("eventService", eventService);
            Register<IUsernameGenerationService>(usernameGenerationService);
            Register<IDeviceTrustCryptoService>(deviceTrustCryptoService);
            Register<IPasswordResetEnrollmentService>(passwordResetEnrollmentService);
#if ANDROID
            Register<IAssetLinksService>(new AssetLinksService(apiService));
#endif
        }

        public static void Register<T>(string serviceName, T obj)
        {
            if (!RegisteredServices.TryAdd(serviceName, obj))
            {
                throw new Exception($"Service {serviceName} has already been registered.");
            }
        }

        public static T Resolve<T>(string serviceName, bool dontThrow = false)
        {
            if (RegisteredServices.TryGetValue(serviceName, out var service))
            {
                return (T)service;
            }
            if (dontThrow)
            {
                return (T)(object)null;
            }
            throw new Exception($"Service {serviceName} is not registered.");
        }

        public static void Register<T>(T obj)
            where T : class
        {
            Register(typeof(T), obj);
        }

        public static void Register(Type type, object obj)
        {
            var serviceName = GetServiceRegistrationName(type);
            if (!RegisteredServices.TryAdd(serviceName, obj))
            {
                throw new Exception($"Service {serviceName} has already been registered.");
            }
        }

        public static T Resolve<T>()
            where T : class
        {
            return (T)Resolve(typeof(T));
        }

        public static object Resolve(Type type)
        {
            var serviceName = GetServiceRegistrationName(type);
            if (RegisteredServices.TryGetValue(serviceName, out var service))
            {
                return service;
            }
            throw new Exception($"Service {serviceName} is not registered.");
        }

        public static bool TryResolve<T>(out T service)
            where T : class
        {
            try
            {
                var toReturn = TryResolve(typeof(T), out var serviceObj);
                service = (T)serviceObj;
                return toReturn;
            }
            catch (Exception)
            {
                service = null;
                return false;
            }
        }

        public static bool TryResolve(Type type, out object service)
        {
            var serviceName = GetServiceRegistrationName(type);
            return RegisteredServices.TryGetValue(serviceName, out service);
        }

        public static void Reset()
        {
            foreach (var service in RegisteredServices)
            {
                if (service.Value != null && service.Value is IDisposable disposableService)
                {
                    disposableService.Dispose();
                }
            }
            Inited = false;
            RegisteredServices.Clear();
            RegisteredServices = new ConcurrentDictionary<string, object>();
        }

        /// <summary>
        /// Gets the service registration name
        /// </summary>
        /// <param name="type">Type of the service</param>
        /// <remarks>
        /// In order to work with already register/resolve we need to maintain the naming convention
        /// of camelCase without the first "I" on the services interfaces
        /// e.g. "ITokenService" -> "tokenService"
        /// </remarks>
        static string GetServiceRegistrationName(Type type)
        {
            var typeName = type.Name;
            var sb = new StringBuilder();

            var indexToLowerCase = 0;
            if (typeName[0] == 'I' && char.IsUpper(typeName[1]))
            {
                // if it's an interface then we ignore the first char
                // and lower case the 2nd one (index 1)
                indexToLowerCase = 1;
            }
            sb.Append(char.ToLower(typeName[indexToLowerCase], new CultureInfo("en-US")));
            sb.Append(typeName.Substring(++indexToLowerCase));
            return sb.ToString();
        }
    }
}
