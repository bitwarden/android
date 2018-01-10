using Bit.App.Abstractions;

namespace Bit.App.Services
{
    public class NoopPushNotificationService : IPushNotificationService
    {
        public string Token => null;

        public void Register()
        {
        }

        public void Unregister()
        {
        }
    }
}
