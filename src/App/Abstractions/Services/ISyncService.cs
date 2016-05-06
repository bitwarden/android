using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface ISyncService
    {
        Task<bool> SyncAsync();
    }
}