using Newtonsoft.Json.Linq;
using Bit.App.Abstractions;

namespace Bit.App.Services
{
    public class NoopPushNotificationListener : IPushNotificationListener
    {
        public void OnMessage(JObject value, string deviceType)
        {
        }

        public void OnRegistered(string token, string deviceType)
        {
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
