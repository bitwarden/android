using System.Threading.Tasks;
using Bit.Core.Enums;
using Bit.Core.Models.Data;

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
        Region SelectedRegion { get; set; }

        string GetWebVaultUrl(bool returnNullIfDefault = false);
        string GetWebSendUrl();
        string GetCurrentDomain();
        Task SetUrlsFromStorageAsync();
        Task<EnvironmentUrlData> SetRegionAsync(Region region, EnvironmentUrlData selfHostedUrls = null);
    }
}
