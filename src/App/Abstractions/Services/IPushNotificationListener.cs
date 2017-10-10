using Newtonsoft.Json.Linq;

namespace Bit.App.Abstractions
{
    public interface IPushNotificationListener
    {
        void OnMessage(JObject values, string device);
        void OnRegistered(string token, string device);
        void OnUnregistered(string device);
        void OnError(string message, string device);
        bool ShouldShowNotification();
    }
}
