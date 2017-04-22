using System;
using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface ISyncService
    {
        bool SyncInProgress { get; }
        Task<bool> SyncCipherAsync(string id);
        Task<bool> SyncFolderAsync(string id);
        Task<bool> SyncDeleteFolderAsync(string id, DateTime revisionDate);
        Task<bool> SyncDeleteLoginAsync(string id);
        Task<bool> SyncSettingsAsync();
        Task<bool> SyncProfileAsync();
        Task<bool> FullSyncAsync(bool forceSync = false);
        Task<bool> FullSyncAsync(TimeSpan syncThreshold, bool forceSync = false);
    }
}
