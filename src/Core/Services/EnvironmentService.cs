using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;

namespace Bit.Core.Services
{
    public class EnvironmentService : IEnvironmentService
    {
        private const string DEFAULT_WEB_VAULT_URL = "https://vault.bitwarden.com";
        private const string DEFAULT_WEB_SEND_URL = "https://send.bitwarden.com/#";

        private readonly IApiService _apiService;
        private readonly IStateService _stateService;
        private readonly IConditionedAwaiterManager _conditionedAwaiterManager;

        public EnvironmentService(
            IApiService apiService,
            IStateService stateService,
            IConditionedAwaiterManager conditionedAwaiterManager)
        {
            _apiService = apiService;
            _stateService = stateService;
            _conditionedAwaiterManager = conditionedAwaiterManager;
        }

        public string BaseUrl { get; set; }
        public string WebVaultUrl { get; set; }
        public string ApiUrl { get; set; }
        public string IdentityUrl { get; set; }
        public string IconsUrl { get; set; }
        public string NotificationsUrl { get; set; }
        public string EventsUrl { get; set; }
        public Region SelectedRegion { get; set; }

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

        public string GetCurrentDomain()
        {
            var url = WebVaultUrl ?? BaseUrl ?? ApiUrl ?? IdentityUrl;
            if (!string.IsNullOrWhiteSpace(url))
            {
                if (url.Contains(Region.US.Domain()) || url.Contains(Region.EU.Domain()))
                {
                    return CoreHelpers.GetDomain(url);
                }
                return CoreHelpers.GetHostname(url);
            }
            return string.Empty;
        }

        public async Task SetUrlsFromStorageAsync()
        {
            try
            {
                var region = await _stateService.GetActiveUserRegionAsync();
                var urls = await _stateService.GetEnvironmentUrlsAsync();
                urls ??= await _stateService.GetPreAuthEnvironmentUrlsAsync();

                if (urls == null || urls.IsEmpty)
                {
                    await SetRegionAsync(Region.US);
                    _conditionedAwaiterManager.SetAsCompleted(AwaiterPrecondition.EnvironmentUrlsInited);
                    return;
                }

                // Migrate old users to regions
                region ??= await MigrateToRegionsAsync(urls);

                switch (region)
                {
                    case Region.US:
                    case Region.EU:
                        await SetRegionAsync(region.Value);
                        break;
                    case Region.SelfHosted:
                    case null:
                    default:
                        await SetRegionAsync(Region.SelfHosted, urls);
                        break;
                }
                _conditionedAwaiterManager.SetAsCompleted(AwaiterPrecondition.EnvironmentUrlsInited);
            }
            catch (System.Exception ex)
            {
                _conditionedAwaiterManager.SetException(AwaiterPrecondition.EnvironmentUrlsInited, ex);
                throw;
            }

        }

        private async Task<Region?> MigrateToRegionsAsync(EnvironmentUrlData urls)
        {
            if (urls.Base == Region.US.BaseUrl())
            {
                await _stateService.UpdateActiveUserEnvironmentAsync(Region.US, Region.US.GetUrls());
                return Region.US;
            }
            if (urls.Base == Region.EU.BaseUrl())
            {
                await _stateService.UpdateActiveUserEnvironmentAsync(Region.EU, Region.EU.GetUrls());
                return Region.EU;
            }
            await _stateService.UpdateActiveUserEnvironmentAsync(Region.SelfHosted, urls);
            return Region.SelfHosted;
        }

        public async Task<EnvironmentUrlData> SetUrlsAsync(EnvironmentUrlData urls)
        {
            // format urls for Base comparison
            urls = FormatUrls(urls);
            if (urls.Base == Region.EU.BaseUrl())
            {
                return await SetRegionAsync(Region.EU);
            }
            if (urls.Base == Region.US.BaseUrl())
            {
                return await SetRegionAsync(Region.US);
            }
            return await SetRegionAsync(Region.SelfHosted, urls);
        }

        public async Task<EnvironmentUrlData> SetRegionAsync(Region region, EnvironmentUrlData selfHostedUrls = null)
        {
            EnvironmentUrlData urls;
            await _stateService.SetPreAuthRegionAsync(region);
            SelectedRegion = region;

            if (region == Region.SelfHosted)
            {
                // If user saves a self-hosted region with empty fields, default to US
                if (selfHostedUrls.IsEmpty)
                {
                    return await SetRegionAsync(Region.US);
                }
                urls = FormatUrls(selfHostedUrls);
            }
            else
            {
                urls = region.GetUrls();
            }

            await _stateService.SetPreAuthEnvironmentUrlsAsync(urls);
            BaseUrl = urls.Base;
            WebVaultUrl = urls.WebVault;
            ApiUrl = urls.Api;
            IdentityUrl = urls.Identity;
            IconsUrl = urls.Icons;
            NotificationsUrl = urls.Notifications;
            EventsUrl = urls.Events;
            _apiService.SetUrls(urls);
            return urls;
        }

        private EnvironmentUrlData FormatUrls(EnvironmentUrlData urls)
        {
            urls.Base = FormatUrl(urls.Base);
            urls.WebVault = FormatUrl(urls.WebVault);
            urls.Api = FormatUrl(urls.Api);
            urls.Identity = FormatUrl(urls.Identity);
            urls.Icons = FormatUrl(urls.Icons);
            urls.Notifications = FormatUrl(urls.Notifications);
            urls.Events = FormatUrl(urls.Events);
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
