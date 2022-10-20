using System.Threading.Tasks;
using Bit.App.Models;
using Newtonsoft.Json.Linq;

namespace Bit.App.Abstractions
{
    public interface IPushNotificationListenerService
    {
        Task OnMessageAsync(JObject values, string device);
        Task OnRegisteredAsync(string token, string device);
        void OnUnregistered(string device);
        void OnError(string message, string device);
        Task OnNotificationTapped(BaseNotificationData data);
        Task OnNotificationDismissed(BaseNotificationData data);
        bool ShouldShowNotification();
    }
}
