using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface IPushNotificationService
    {
        Task<string> GetTokenAsync();
        Task RegisterAsync();
        Task UnregisterAsync();
    }
}
