using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models;
using Newtonsoft.Json.Linq;

namespace Bit.App.Services
{
    public class NoopPushNotificationListenerService : IPushNotificationListenerService
    {
        public Task OnMessageAsync(JObject value, string deviceType)
        {
            return Task.FromResult(0);
        }

        public Task OnRegisteredAsync(string token, string deviceType)
        {
            return Task.FromResult(0);
        }

        public void OnUnregistered(string deviceType)
        {
        }

        public void OnError(string message, string deviceType)
        {
        }

        public bool ShouldShowNotification()
        {
            return false;
        }

        public Task OnNotificationTapped(BaseNotificationData data)
        {
            return Task.FromResult(0);
        }

        public Task OnNotificationDismissed(BaseNotificationData data)
        {
            return Task.FromResult(0);
        }
    }
}
