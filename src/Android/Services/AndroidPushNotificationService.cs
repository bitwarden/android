#if !FDROID
using System;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.Core;
using Bit.Core.Abstractions;
using Xamarin.Forms;

namespace Bit.Droid.Services
{
    public class AndroidPushNotificationService : IPushNotificationService
    {
        private readonly IStorageService _storageService;
        private readonly IPushNotificationListenerService _pushNotificationListenerService;

        public AndroidPushNotificationService(
            IStorageService storageService,
            IPushNotificationListenerService pushNotificationListenerService)
        {
            _storageService = storageService;
            _pushNotificationListenerService = pushNotificationListenerService;
        }

        public async Task<string> GetTokenAsync()
        {
            return await _storageService.GetAsync<string>(Constants.PushCurrentTokenKey);
        }

        public async Task RegisterAsync()
        {
            var registeredToken = await _storageService.GetAsync<string>(Constants.PushRegisteredTokenKey);
            var currentToken = await GetTokenAsync();
            if (!string.IsNullOrWhiteSpace(registeredToken) && registeredToken != currentToken)
            {
                await _pushNotificationListenerService.OnRegisteredAsync(registeredToken, Device.Android);
            }
            else
            {
                await _storageService.SaveAsync(Constants.PushLastRegistrationDateKey, DateTime.UtcNow);
            }
        }

        public Task UnregisterAsync()
        {
            // Do we ever need to unregister?
            return Task.FromResult(0);
        }
    }
}
#endif
