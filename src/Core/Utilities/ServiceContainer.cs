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

            var stateService = new StateService();
            var cryptoFunctionService = new PclCryptoFunctionService(cryptoPrimitiveService);
            var cryptoService = new CryptoService(storageService, secureStorageService, cryptoFunctionService);
            var tokenService = new TokenService(storageService);
            var apiService = new ApiService(tokenService, platformUtilsService, (bool expired) => Task.FromResult(0));
            var appIdService = new AppIdService(storageService);
            var userService = new UserService(storageService, tokenService);

            Register<IStateService>("stateService", stateService);
            Register<ICryptoFunctionService>("cryptoFunctionService", cryptoFunctionService);
            Register<ICryptoService>("cryptoService", cryptoService);
            Register<ITokenService>("tokenService", tokenService);
            Register<ApiService>("apiService", apiService); // TODO: interface
            Register<IAppIdService>("appIdService", appIdService);
            Register<IUserService>("userService", userService);
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
