using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IWatchDeviceService
    {
        Task SyncDataToWatchAsync();
    }
}
