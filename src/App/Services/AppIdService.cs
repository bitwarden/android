using System;
using Bit.App.Abstractions;

namespace Bit.App.Services
{
    public class AppIdService : IAppIdService
    {
        private const string AppIdKey = "appId";
        private const string AnonymousAppIdKey = "anonymousAppId";
        private readonly ISecureStorageService _secureStorageService;
        private Guid? _appId;
        private Guid? _anonymousAppId;

        public AppIdService(ISecureStorageService secureStorageService)
        {
            _secureStorageService = secureStorageService;
        }

        public string AppId => GetAppId(AppIdKey, ref _appId);
        public string AnonymousAppId => GetAppId(AnonymousAppIdKey, ref _anonymousAppId);

        private string GetAppId(string key, ref Guid? appId)
        {
            if(appId.HasValue)
            {
                return appId.Value.ToString();
            }

            var appIdBytes = _secureStorageService.Retrieve(key);
            if(appIdBytes != null)
            {
                appId = new Guid(appIdBytes);
                return appId.Value.ToString();
            }

            appId = Guid.NewGuid();
            _secureStorageService.Store(key, appId.Value.ToByteArray());
            return appId.Value.ToString();
        }
    }
}
