using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface ISyncService
    {
        Task<bool> SyncAsync(string id);
        Task<bool> SyncDeleteFolderAsync(string id);
        Task<bool> SyncDeleteSiteAsync(string id);
        Task<bool> FullSyncAsync();
        Task<bool> IncrementalSyncAsync();
    }
}
