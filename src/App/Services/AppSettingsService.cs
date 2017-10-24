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

        public bool DisableWebsiteIcons
        {
            get
            {
                return _settings.GetValueOrDefault(Constants.SettingDisableWebsiteIcons, false);
            }
            set
            {
                _settings.AddOrUpdateValue(Constants.SettingDisableWebsiteIcons, value);
            }
        }

        public string SecurityStamp
        {
            get
            {
                return _settings.GetValueOrDefault(Constants.SecurityStamp, null);
            }
            set
            {
                _settings.AddOrUpdateValue(Constants.SecurityStamp, value);
            }
        }

        public string BaseUrl
        {
            get
            {
                return _settings.GetValueOrDefault(Constants.BaseUrl, null);
            }
            set
            {
                if(value == null)
                {
                    _settings.Remove(Constants.BaseUrl);
                    return;
                }

                _settings.AddOrUpdateValue(Constants.BaseUrl, value);
            }
        }

        public string WebVaultUrl
        {
            get
            {
                return _settings.GetValueOrDefault(Constants.WebVaultUrl, null);
            }
            set
            {
                if(value == null)
                {
                    _settings.Remove(Constants.WebVaultUrl);
                    return;
                }

                _settings.AddOrUpdateValue(Constants.WebVaultUrl, value);
            }
        }

        public string ApiUrl
        {
            get
            {
                return _settings.GetValueOrDefault(Constants.ApiUrl, null);
            }
            set
            {
                if(value == null)
                {
                    _settings.Remove(Constants.ApiUrl);
                    return;
                }

                _settings.AddOrUpdateValue(Constants.ApiUrl, value);
            }
        }

        public string IdentityUrl
        {
            get
            {
                return _settings.GetValueOrDefault(Constants.IdentityUrl, null);
            }
            set
            {
                if(value == null)
                {
                    _settings.Remove(Constants.IdentityUrl);
                    return;
                }

                _settings.AddOrUpdateValue(Constants.IdentityUrl, value);
            }
        }

        public string IconsUrl
        {
            get => _settings.GetValueOrDefault(Constants.IconsUrl, null);
            set
            {
                if(value == null)
                {
                    _settings.Remove(Constants.IconsUrl);
                    return;
                }

                _settings.AddOrUpdateValue(Constants.IconsUrl, value);
            }
        }
    }
}
