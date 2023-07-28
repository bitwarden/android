using System.Threading.Tasks;
using Bit.Core.Enums;

namespace Bit.Core.Abstractions
{
    public interface IEventService
    {
        Task ClearEventsAsync();
        Task CollectAsync(EventType eventType, string cipherId = null, bool uploadImmediately = false);
        Task UploadEventsAsync();
    }
}
