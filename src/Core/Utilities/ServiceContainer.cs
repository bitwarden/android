using Bit.Core.Abstractions;
using Bit.Core.Services;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.Core.Utilities
{
    public static class ServiceContainer
    {
        public static Dictionary<string, object> RegisteredServices { get; set; } = new Dictionary<string, object>();
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
            SearchService searchService = null;

            var tokenService = new TokenService(stateService);
            var apiService = new ApiService(tokenService, platformUtilsService, (extras) =>
            {
                messagingService.Send("logout", extras);
                return Task.FromResult(0);
            }, customUserAgent);
            var appIdService = new AppIdService(storageService);
            var organizationService = new OrganizationService(stateService);
            var settingsService = new SettingsService(stateService);
            var fileUploadService = new FileUploadService(apiService);
            var cipherService = new CipherService(cryptoService, stateService, settingsService, apiService, 
                fileUploadService, storageService, i18nService, () => searchService, clearCipherCacheKey, 
                allClearCipherCacheKeys);
            var folderService = new FolderService(cryptoService, stateService, apiService, i18nService, cipherService);
            var collectionService = new CollectionService(cryptoService, stateService, i18nService);
            var sendService = new SendService(cryptoService, stateService, apiService, fileUploadService, i18nService,
                cryptoFunctionService);
            searchService = new SearchService(cipherService, sendService);
            var policyService = new PolicyService(stateService, organizationService);
            var keyConnectorService = new KeyConnectorService(stateService, cryptoService, tokenService, apiService,
                organizationService);
            var vaultTimeoutService = new VaultTimeoutService(cryptoService, stateService, platformUtilsService,
                folderService, cipherService, collectionService, searchService, messagingService, tokenService,
                policyService, keyConnectorService, null, (extras) =>
                {
                    messagingService.Send("logout", extras);
                    return Task.FromResult(0);
                });
            var syncService = new SyncService(stateService, apiService, settingsService, folderService, cipherService, 
                cryptoService, collectionService, organizationService, messagingService, policyService, sendService,
                keyConnectorService, (extras) =>
                {
                    messagingService.Send("logout", extras);
                    return Task.FromResult(0);
                });
            var passwordGenerationService = new PasswordGenerationService(cryptoService, stateService,
                cryptoFunctionService, policyService);
            var totpService = new TotpService(stateService, cryptoFunctionService);
            var authService = new AuthService(cryptoService, cryptoFunctionService, apiService, stateService, 
                tokenService, appIdService, i18nService, platformUtilsService, messagingService, vaultTimeoutService, 
                keyConnectorService);
            var exportService = new ExportService(folderService, cipherService, cryptoService);
            var auditService = new AuditService(cryptoFunctionService, apiService);
            var environmentService = new EnvironmentService(apiService, stateService);
            var eventService = new EventService(apiService, stateService, organizationService, cipherService);
            var userVerificationService = new UserVerificationService(apiService, platformUtilsService, i18nService, 
                cryptoService);

            Register<ITokenService>("tokenService", tokenService);
            Register<IApiService>("apiService", apiService);
            Register<IAppIdService>("appIdService", appIdService);
            Register<IOrganizationService>("organizationService", organizationService);
            Register<ISettingsService>("settingsService", settingsService);
            Register<ICipherService>("cipherService", cipherService);
            Register<IFolderService>("folderService", folderService);
            Register<ICollectionService>("collectionService", collectionService);
            Register<ISendService>("sendService", sendService);
            Register<ISearchService>("searchService", searchService);
            Register<IPolicyService>("policyService", policyService);
            Register<ISyncService>("syncService", syncService);
            Register<IVaultTimeoutService>("vaultTimeoutService", vaultTimeoutService);
            Register<IPasswordGenerationService>("passwordGenerationService", passwordGenerationService);
            Register<ITotpService>("totpService", totpService);
            Register<IAuthService>("authService", authService);
            Register<IExportService>("exportService", exportService);
            Register<IAuditService>("auditService", auditService);
            Register<IEnvironmentService>("environmentService", environmentService);
            Register<IEventService>("eventService", eventService);
            Register<IKeyConnectorService>("keyConnectorService", keyConnectorService);
            Register<IUserVerificationService>("userVerificationService", userVerificationService);
        }

        public static void Register<T>(string serviceName, T obj)
        {
            if (RegisteredServices.ContainsKey(serviceName))
            {
                throw new Exception($"Service {serviceName} has already been registered.");
            }
            RegisteredServices.Add(serviceName, obj);
        }

        public static T Resolve<T>(string serviceName, bool dontThrow = false)
        {
            if (RegisteredServices.ContainsKey(serviceName))
            {
                return (T)RegisteredServices[serviceName];
            }
            if (dontThrow)
            {
                return (T)(object)null;
            }
            throw new Exception($"Service {serviceName} is not registered.");
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
            RegisteredServices = new Dictionary<string, object>();
        }
    }
}
