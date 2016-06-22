using System;
using Bit.App.Abstractions;
using Plugin.Settings.Abstractions;

namespace Bit.App.Services
{
    public class AppIdService : IAppIdService
    {
        private const string AppIdKey = "appId";
        private readonly ISettings _settings;
        private string _appId;

        public AppIdService(ISettings settings)
        {
            _settings = settings;
        }

        public string AppId
        {
            get
            {
                if(!string.IsNullOrWhiteSpace(_appId))
                {
                    return _appId;
                }

                _appId = _settings.GetValueOrDefault<string>(AppIdKey);
                if(!string.IsNullOrWhiteSpace(_appId))
                {
                    return _appId;
                }

                _appId = Guid.NewGuid().ToString();
                _settings.AddOrUpdateValue(AppIdKey, _appId);
                return _appId;
            }
        }
    }
}
