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
            var secureStorageService = Resolve<IStorageService>("secureStorageService");
            var i18nService = Resolve<II18nService>("i18nService");
            var messagingService = Resolve<IMessagingService>("messagingService");
            var cryptoFunctionService = Resolve<ICryptoFunctionService>("cryptoFunctionService");
            var cryptoService = Resolve<ICryptoService>("cryptoService");
            SearchService searchService = null;

            var stateService = new StateService();
            var tokenService = new TokenService(storageService);
            var apiService = new ApiService(tokenService, platformUtilsService, (bool expired) =>
            {
                messagingService.Send("logout", expired);
                return Task.FromResult(0);
            }, customUserAgent);
            var appIdService = new AppIdService(storageService);
            var userService = new UserService(storageService, tokenService);
            var settingsService = new SettingsService(userService, storageService);
            var fileUploadService = new FileUploadService(apiService);
            var cipherService = new CipherService(cryptoService, userService, settingsService, apiService, fileUploadService,
                storageService, i18nService, () => searchService, clearCipherCacheKey, allClearCipherCacheKeys);
            var folderService = new FolderService(cryptoService, userService, apiService, storageService,
                i18nService, cipherService);
            var collectionService = new CollectionService(cryptoService, userService, storageService, i18nService);
            var sendService = new SendService(cryptoService, userService, apiService, fileUploadService, storageService,
                i18nService, cryptoFunctionService);
            searchService = new SearchService(cipherService, sendService);
            var vaultTimeoutService = new VaultTimeoutService(cryptoService, userService, platformUtilsService, 
                storageService, folderService, cipherService, collectionService, searchService, messagingService, tokenService,
                null, (expired) =>
                {
                    messagingService.Send("logout", expired);
                    return Task.FromResult(0);
                });
            var policyService = new PolicyService(storageService, userService);
            var syncService = new SyncService(userService, apiService, settingsService, folderService,
                cipherService, cryptoService, collectionService, storageService, messagingService, policyService, sendService,
                (bool expired) =>
                {
                    messagingService.Send("logout", expired);
                    return Task.FromResult(0);
                });
            var passwordGenerationService = new PasswordGenerationService(cryptoService, storageService,
                cryptoFunctionService, policyService);
            var totpService = new TotpService(storageService, cryptoFunctionService);
            var authService = new AuthService(cryptoService, apiService, userService, tokenService, appIdService,
                i18nService, platformUtilsService, messagingService, vaultTimeoutService);
            var exportService = new ExportService(folderService, cipherService, cryptoService);
            var auditService = new AuditService(cryptoFunctionService, apiService);
            var environmentService = new EnvironmentService(apiService, storageService);
            var eventService = new EventService(storageService, apiService, userService, cipherService);

            Register<IStateService>("stateService", stateService);
            Register<ITokenService>("tokenService", tokenService);
            Register<IApiService>("apiService", apiService);
            Register<IAppIdService>("appIdService", appIdService);
            Register<IUserService>("userService", userService);
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
