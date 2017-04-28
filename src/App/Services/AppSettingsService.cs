using System;
using Bit.App.Abstractions;
using Plugin.Settings.Abstractions;

namespace Bit.App.Services
{
    public class AppSettingsService : IAppSettingsService
    {
        private readonly ISettings _settings;

        public AppSettingsService(
            ISettings settings)
        {
            _settings = settings;
        }

        public bool Locked
        {
            get
            {
                return _settings.GetValueOrDefault(Constants.Locked, false);
            }
            set
            {
                _settings.AddOrUpdateValue(Constants.Locked, value);
            }
        }

        public DateTime LastActivity
        {
            get
            {
                return _settings.GetValueOrDefault(Constants.LastActivityDate, DateTime.MinValue);
            }
            set
            {
                _settings.AddOrUpdateValue(Constants.LastActivityDate, value);
            }
        }
    }
}
