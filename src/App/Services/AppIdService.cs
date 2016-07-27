using System;
using Bit.App.Abstractions;

namespace Bit.App.Services
{
    public class AppIdService : IAppIdService
    {
        private const string AppIdKey = "appId";
        private readonly ISecureStorageService _secureStorageService;
        private Guid? _appId;

        public AppIdService(ISecureStorageService secureStorageService)
        {
            _secureStorageService = secureStorageService;
        }

        public string AppId
        {
            get
            {
                if(_appId.HasValue)
                {
                    return _appId.Value.ToString();
                }

                var appIdBytes = _secureStorageService.Retrieve(AppIdKey);
                if(appIdBytes != null)
                {
                    _appId = new Guid(appIdBytes);
                    return _appId.Value.ToString();
                }

                _appId = Guid.NewGuid();
                _secureStorageService.Store(AppIdKey, _appId.Value.ToByteArray());
                return _appId.Value.ToString();
            }
        }
    }
}
