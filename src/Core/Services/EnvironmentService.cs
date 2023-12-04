using Bit.Core.Abstractions;
using Bit.Core.Models.Data;
using Bit.Core.Utilities;
using BwRegion = Bit.Core.Enums.Region;

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
        public BwRegion SelectedRegion { get; set; }

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
            return new EnvironmentUrlData
            {
                WebVault = WebVaultUrl,
                Base = BaseUrl,
                Api = ApiUrl,
                Identity = IdentityUrl
            }.GetDomainOrHostname();
        }

        public async Task SetUrlsFromStorageAsync()
        {
            try
            {
                var region = await _stateService.GetActiveUserRegionAsync();
                var urls = await _stateService.GetEnvironmentUrlsAsync();
                urls ??= await _stateService.GetPreAuthEnvironmentUrlsAsync();

                if (urls == null || urls.IsEmpty || region is null)
                {
                    await SetRegionAsync(BwRegion.US);
                    _conditionedAwaiterManager.SetAsCompleted(AwaiterPrecondition.EnvironmentUrlsInited);
                    return;
                }

                await SetRegionAsync(region.Value, urls);
                _conditionedAwaiterManager.SetAsCompleted(AwaiterPrecondition.EnvironmentUrlsInited);
            }
            catch (System.Exception ex)
            {
                _conditionedAwaiterManager.SetException(AwaiterPrecondition.EnvironmentUrlsInited, ex);
                throw;
            }

        }

        public async Task<EnvironmentUrlData> SetRegionAsync(BwRegion region, EnvironmentUrlData selfHostedUrls = null)
        {
            EnvironmentUrlData urls;

            if (region == BwRegion.SelfHosted)
            {
                // If user saves a self-hosted region with empty fields, default to US
                if (selfHostedUrls.IsEmpty)
                {
                    return await SetRegionAsync(BwRegion.US);
                }
                urls = selfHostedUrls.FormatUrls();
            }
            else
            {
                urls = region.GetUrls();
            }

            SelectedRegion = region;
            await _stateService.SetPreAuthRegionAsync(region);
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
    }
}
