using Newtonsoft.Json.Linq;
using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface IPushNotificationListenerService
    {
        Task OnMessageAsync(JObject values, string device);
        Task OnRegisteredAsync(string token, string device);
        void OnUnregistered(string device);
        void OnError(string message, string device);
        bool ShouldShowNotification();
    }
}
