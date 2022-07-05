using System.Threading.Tasks;
using Bit.App.Abstractions;
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
    }
}
