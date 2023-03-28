using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;

namespace Bit.Core.Services
{
    public class EnvironmentService : IEnvironmentService
    {
        private const string DEFAULT_WEB_VAULT_URL = "https://vault.bitwarden.com";
        private const string DEFAULT_WEB_SEND_URL = "https://send.bitwarden.com/#";

        private readonly IApiService _apiService;
        private readonly IStateService _stateService;

        public EnvironmentService(
            IApiService apiService,
            IStateService stateService)
        {
            _apiService = apiService;
            _stateService = stateService;
        }

        public string DefaultBaseUsRegion => "https://vault.bitwarden.com";
        public string DefaultBaseEuRegion => "https://vault.bitwarden.eu";

        public string BaseUrl { get; set; }
        public string WebVaultUrl { get; set; }
        public string ApiUrl { get; set; }
        public string IdentityUrl { get; set; }
        public string IconsUrl { get; set; }
        public string NotificationsUrl { get; set; }
        public string EventsUrl { get; set; }

        public string GetWebVaultUrl(bool returnNullIfDefault = false)
        {
            if (!string.IsNullOrWhiteSpace(WebVaultUrl))
            {
                return WebVaultUrl;
            }

            if (!string.IsNullOrWhiteSpace(BaseUrl))
            {
                return BaseUrl;
            }

            return returnNullIfDefault ? (string)null : DEFAULT_WEB_VAULT_URL;
        }

        public string GetWebSendUrl()
        {
            return GetWebVaultUrl(true) is string webVaultUrl ? $"{webVaultUrl}/#/send/" : DEFAULT_WEB_SEND_URL;
        }

        public async Task SetUrlsFromStorageAsync()
        {
            var urls = await _stateService.GetEnvironmentUrlsAsync();
            if (urls == null)
            {
                urls = await _stateService.GetPreAuthEnvironmentUrlsAsync();
            }
            if (urls == null)
            {
                urls = new EnvironmentUrlData();
            }
            var envUrls = new EnvironmentUrls();
            if (!string.IsNullOrWhiteSpace(urls.Base))
            {
                BaseUrl = envUrls.Base = urls.Base;
                _apiService.SetUrls(envUrls);
                return;
            }
            BaseUrl = urls.Base;
            WebVaultUrl = urls.WebVault;
            ApiUrl = envUrls.Api = urls.Api;
            IdentityUrl = envUrls.Identity = urls.Identity;
            IconsUrl = urls.Icons;
            NotificationsUrl = urls.Notifications;
            EventsUrl = envUrls.Events = urls.Events;
            _apiService.SetUrls(envUrls);
        }

        public async Task<EnvironmentUrlData> SetUrlsAsync(EnvironmentUrlData urls)
        {
            urls.Base = FormatUrl(urls.Base);
            urls.WebVault = FormatUrl(urls.WebVault);
            urls.Api = FormatUrl(urls.Api);
            urls.Identity = FormatUrl(urls.Identity);
            urls.Icons = FormatUrl(urls.Icons);
            urls.Notifications = FormatUrl(urls.Notifications);
            urls.Events = FormatUrl(urls.Events);
            await _stateService.SetPreAuthEnvironmentUrlsAsync(urls);
            BaseUrl = urls.Base;
            WebVaultUrl = urls.WebVault;
            ApiUrl = urls.Api;
            IdentityUrl = urls.Identity;
            IconsUrl = urls.Icons;
            NotificationsUrl = urls.Notifications;
            EventsUrl = urls.Events;

            var envUrls = new EnvironmentUrls();
            if (!string.IsNullOrWhiteSpace(BaseUrl))
            {
                envUrls.Base = BaseUrl;
            }
            else
            {
                envUrls.Api = ApiUrl;
                envUrls.Identity = IdentityUrl;
                envUrls.Events = EventsUrl;
            }

            _apiService.SetUrls(envUrls);
            return urls;
        }

        private string FormatUrl(string url)
        {
            if (string.IsNullOrWhiteSpace(url))
            {
                return null;
            }
            url = Regex.Replace(url, "\\/+$", string.Empty);
            if (!url.StartsWith("http://") && !url.StartsWith("https://"))
            {
                url = string.Concat("https://", url);
            }
            return url.Trim();
        }

        public async Task<EnvironmentUrlData> SetUsUrlsAsync()
        {
            return await SetUrlsAsync(new EnvironmentUrlData()
            { Base = DefaultBaseUsRegion });
        }

        public async Task<EnvironmentUrlData> SetEuUrlsAsync()
        {
            return await SetUrlsAsync(new EnvironmentUrlData()
            { Base = DefaultBaseEuRegion });
        }
    }
}
