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

        public static void Init()
        {
            if(Inited)
            {
                return;
            }
            Inited = true;

            var platformUtilsService = Resolve<IPlatformUtilsService>("platformUtilsService");
            var storageService = Resolve<IStorageService>("storageService");
            var secureStorageService = Resolve<IStorageService>("secureStorageService");
            var cryptoPrimitiveService = Resolve<ICryptoPrimitiveService>("cryptoPrimitiveService");
            var i18nService = Resolve<II18nService>("i18nService");
            var messagingService = Resolve<IMessagingService>("messagingService");
            SearchService searchService = null;

            var stateService = new StateService();
            var cryptoFunctionService = new PclCryptoFunctionService(cryptoPrimitiveService);
            var cryptoService = new CryptoService(storageService, secureStorageService, cryptoFunctionService);
            var tokenService = new TokenService(storageService);
            var apiService = new ApiService(tokenService, platformUtilsService, (bool expired) => Task.FromResult(0));
            var appIdService = new AppIdService(storageService);
            var userService = new UserService(storageService, tokenService);
            var settingsService = new SettingsService(userService, storageService);
            var cipherService = new CipherService(cryptoService, userService, settingsService, apiService,
                storageService, i18nService, () => searchService);
            var folderService = new FolderService(cryptoService, userService, apiService, storageService,
                i18nService, cipherService);
            var collectionService = new CollectionService(cryptoService, userService, storageService, i18nService);
            searchService = new SearchService(cipherService);
            var lockService = new LockService(cryptoService, userService, platformUtilsService, storageService,
                folderService, cipherService, collectionService, searchService, messagingService);
            var syncService = new SyncService(userService, apiService, settingsService, folderService,
                cipherService, cryptoService, collectionService, storageService, messagingService);
            var passwordGenerationService = new PasswordGenerationService(cryptoService, storageService,
                cryptoFunctionService);
            var totpService = new TotpService(storageService, cryptoFunctionService);
            var authService = new AuthService(cryptoService, apiService, userService, tokenService, appIdService,
                i18nService, platformUtilsService, messagingService);
            // TODO: export service
            var auditService = new AuditService(cryptoFunctionService, apiService);
            var environmentService = new EnvironmentService(apiService, storageService);
            // TODO: notification service

            Register<IStateService>("stateService", stateService);
            Register<ICryptoFunctionService>("cryptoFunctionService", cryptoFunctionService);
            Register<ICryptoService>("cryptoService", cryptoService);
            Register<ITokenService>("tokenService", tokenService);
            Register<IApiService>("apiService", apiService);
            Register<IAppIdService>("appIdService", appIdService);
            Register<IUserService>("userService", userService);
            Register<ISettingsService>("settingsService", settingsService);
            Register<ICipherService>("cipherService", cipherService);
            Register<IFolderService>("folderService", folderService);
            Register<ICollectionService>("collectionService", collectionService);
            Register<ISearchService>("searchService", searchService);
            Register<ISyncService>("syncService", syncService);
            Register<ILockService>("lockService", lockService);
            Register<IPasswordGenerationService>("passwordGenerationService", passwordGenerationService);
            Register<ITotpService>("totpService", totpService);
            Register<IAuthService>("authService", authService);
            Register<IAuditService>("auditService", auditService);
            Register<IEnvironmentService>("environmentService", environmentService);
        }

        public static void Register<T>(string serviceName, T obj)
        {
            if(RegisteredServices.ContainsKey(serviceName))
            {
                throw new Exception($"Service {serviceName} has already been registered.");
            }
            RegisteredServices.Add(serviceName, obj);
        }

        public static T Resolve<T>(string serviceName, bool dontThrow = false)
        {
            if(RegisteredServices.ContainsKey(serviceName))
            {
                return (T)RegisteredServices[serviceName];
            }
            if(dontThrow)
            {
                return (T)(object)null;
            }
            throw new Exception($"Service {serviceName} is not registered.");
        }
    }
}
