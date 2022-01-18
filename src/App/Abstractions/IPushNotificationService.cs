using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface IPushNotificationService
    {
        bool IsRegisteredForPush { get; }
        Task<string> GetTokenAsync();
        Task RegisterAsync();
        Task UnregisterAsync();
    }
}
