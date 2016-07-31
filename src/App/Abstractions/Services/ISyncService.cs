using System;
using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface ISyncService
    {
        bool SyncInProgress { get; }
        Task<bool> SyncAsync(string id);
        Task<bool> SyncDeleteFolderAsync(string id, DateTime revisionDate);
        Task<bool> SyncDeleteSiteAsync(string id);
        Task<bool> FullSyncAsync();
        Task<bool> IncrementalSyncAsync(TimeSpan syncThreshold);
        Task<bool> IncrementalSyncAsync();
    }
}
