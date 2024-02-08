using Bit.Core.Models.Data;
using BwRegion = Bit.Core.Enums.Region;

namespace Bit.Core.Abstractions
{
    public interface IEnvironmentService
    {
        string ApiUrl { get; set; }
        string BaseUrl { get; set; }
        string IconsUrl { get; set; }
        string IdentityUrl { get; set; }
        string NotificationsUrl { get; set; }
        string WebVaultUrl { get; set; }
        string EventsUrl { get; set; }
        BwRegion SelectedRegion { get; set; }

        string GetWebVaultUrl(bool returnNullIfDefault = false);
        string GetWebSendUrl();
        string GetCurrentDomain();
        Task SetUrlsFromStorageAsync();
        Task<EnvironmentUrlData> SetRegionAsync(BwRegion region, EnvironmentUrlData selfHostedUrls = null);
    }
}
