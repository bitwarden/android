using System;
using System.Threading.Tasks;
using Bit.Core.Models.Response;

namespace Bit.Core.Abstractions
{
    public interface ISyncService
    {
        bool SyncInProgress { get; set; }
        Task<bool> FullSyncAsync(bool forceSync, bool allowThrowOnError = false);
        Task<DateTime?> GetLastSyncAsync();
        Task SetLastSyncAsync(DateTime date);
        Task<bool> SyncDeleteCipherAsync(SyncCipherNotification notification);
        Task<bool> SyncDeleteFolderAsync(SyncFolderNotification notification);
        Task<bool> SyncUpsertCipherAsync(SyncCipherNotification notification, bool isEdit);
        Task<bool> SyncUpsertFolderAsync(SyncFolderNotification notification, bool isEdit);
        // Passwordless code will be moved to an independent service in future techdept
        Task SyncPasswordlessLoginRequestsAsync();
    }
}
