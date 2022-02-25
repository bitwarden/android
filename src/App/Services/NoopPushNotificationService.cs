using System.Threading.Tasks;
using Bit.App.Abstractions;

namespace Bit.App.Services
{
    public class NoopPushNotificationService : IPushNotificationService
    {
        public bool IsRegisteredForPush => false;

        public Task<string> GetTokenAsync()
        {
            return Task.FromResult(null as string);
        }

        public Task RegisterAsync()
        {
            return Task.FromResult(0);
        }

        public Task UnregisterAsync()
        {
            return Task.FromResult(0);
        }
    }
}
