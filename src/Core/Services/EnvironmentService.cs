using Bit.Core.Abstractions;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using System.Text.RegularExpressions;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class EnvironmentService : IEnvironmentService
    {
        private readonly IApiService _apiService;
        private readonly IStateService _stateService;

        public EnvironmentService(
            IApiService apiService,
            IStateService stateService)
        {
            _apiService = apiService;
            _stateService = stateService;
        }

        public string BaseUrl { get; set; }
        public string WebVaultUrl { get; set; }
        public string ApiUrl { get; set; }
        public string IdentityUrl { get; set; }
        public string IconsUrl { get; set; }
        public string NotificationsUrl { get; set; }
        public string EventsUrl { get; set; }

        public string GetWebVaultUrl()
        {
            if (!string.IsNullOrWhiteSpace(WebVaultUrl))
            {
                return WebVaultUrl;
            }
            else if (!string.IsNullOrWhiteSpace(BaseUrl))
            {
                return BaseUrl;
            }
            return null;
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
    }
}
