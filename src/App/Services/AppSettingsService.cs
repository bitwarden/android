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

        public DateTime LastCacheClear
        {
            get
            {
                return _settings.GetValueOrDefault(Constants.LastCacheClearDate, DateTime.MinValue);
            }
            set
            {
                _settings.AddOrUpdateValue(Constants.LastCacheClearDate, value);
            }
        }

        public bool AutofillPersistNotification
        {
            get
            {
                return _settings.GetValueOrDefault(Constants.AutofillPersistNotification, false);
            }
            set
            {
                _settings.AddOrUpdateValue(Constants.AutofillPersistNotification, value);
            }
        }

        public bool AutofillPasswordField
        {
            get
            {
                return _settings.GetValueOrDefault(Constants.AutofillPasswordField, false);
            }
            set
            {
                _settings.AddOrUpdateValue(Constants.AutofillPasswordField, value);
            }
        }

        public string SecurityStamp
        {
            get
            {
                return _settings.GetValueOrDefault<string>(Constants.SecurityStamp);
            }
            set
            {
                _settings.AddOrUpdateValue(Constants.SecurityStamp, value);
            }
        }
    }
}
