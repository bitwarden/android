using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IWatchDeviceService
    {
        bool IsConnected { get; }

        Task SetShouldConnectToWatchAsync(bool shouldConnectToWatch);
        Task SyncDataToWatchAsync();
    }
}
